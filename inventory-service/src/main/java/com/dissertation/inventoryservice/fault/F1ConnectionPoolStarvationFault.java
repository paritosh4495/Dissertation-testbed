package com.dissertation.inventoryservice.fault;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class F1ConnectionPoolStarvationFault implements Fault {

    private final FaultRegistry registry;
    private final DataSource dataSource;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean activating = new AtomicBoolean(false);
    private final List<Connection> heldConnections = new CopyOnWriteArrayList<>();
    private final ExecutorService acquisitionExecutor = Executors.newFixedThreadPool(10);

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getId() {
        return "f1";
    }

    @Override
    public String getDescription() {
        return "Starves the database connection pool by holding all connections";
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public synchronized void activate() {
        if (active.get()) return;

        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            throw new IllegalStateException("DataSource is not a HikariDataSource");
        }

        activating.set(true);
        int poolSize = hikariDataSource.getHikariConfigMXBean().getMaximumPoolSize();
        log.info("F1: Attempting deterministic activation (target: {} connections)", poolSize);

        List<Connection> batch = new ArrayList<>();
        try {
            for (int i = 0; i < poolSize; i++) {
                // Try to acquire each connection with a short timeout to keep endpoint responsive
                Connection conn = tryGetConnection(500); 
                if (conn == null) {
                    throw new RuntimeException("Could not acquire connection " + (i + 1) + " of " + poolSize + ". Pool might be already saturated.");
                }
                batch.add(conn);
                log.debug("F1: Acquired connection {}/{}", batch.size(), poolSize);
            }

            heldConnections.addAll(batch);
            active.set(true);
            log.info("F1: Activation successful. Pool starved with {} connections.", heldConnections.size());

        } catch (Exception e) {
            log.error("F1: Activation failed - {}. Rolling back {} connections.", e.getMessage(), batch.size());
            batch.forEach(this::closeQuietly);
            active.set(false);
            throw new RuntimeException(e.getMessage());
        } finally {
            activating.set(false);
        }
    }

    @Override
    public synchronized void deactivate() {
        if (active.compareAndSet(true, false)) {
            log.info("Deactivating F1: Releasing {} connections", heldConnections.size());
            heldConnections.forEach(this::closeQuietly);
            heldConnections.clear();
        }
    }

    private Connection tryGetConnection(long timeoutMs) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Connection conn = dataSource.getConnection();
                    // If activation was cancelled/rolled back while we were waiting, close the connection immediately
                    if (!activating.get()) {
                        closeQuietly(conn);
                        return null;
                    }
                    return conn;
                } catch (SQLException e) {
                    return null;
                }
            }, acquisitionExecutor).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return null;
        }
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            log.warn("F1: Error closing connection: {}", e.getMessage());
        }
    }
}

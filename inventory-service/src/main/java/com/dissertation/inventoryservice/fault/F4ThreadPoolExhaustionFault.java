package com.dissertation.inventoryservice.fault;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class F4ThreadPoolExhaustionFault implements Fault {

    private final FaultRegistry registry;
    private final ServerProperties serverProperties;
    
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicInteger blockedThreadsCount = new AtomicInteger(0);
    private final Object lock = new Object();
    
    // Buffer to ensure we don't block deactivation/management threads
    private static final int MANAGEMENT_BUFFER = 5;

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getId() {
        return "f4";
    }

    @Override
    public String getDescription() {
        return "Exhausts Tomcat worker threads by blocking incoming requests";
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void activate() {
        if (active.compareAndSet(false, true)) {
            log.info("F4: Thread pool exhaustion trap activated.");
        }
    }

    @Override
    public void deactivate() {
        if (active.compareAndSet(true, false)) {
            log.info("F4: Deactivating. Releasing {} blocked threads.", blockedThreadsCount.get());
            synchronized (lock) {
                lock.notifyAll();
            }
            blockedThreadsCount.set(0);
        }
    }

    /**
     * Attempts to block the current thread if the fault is active and there is capacity in the trap.
     * @return true if the thread was blocked and then released, false if it wasn't blocked.
     */
    public boolean tryBlock() {
        if (!active.get()) {
            return false;
        }

        int maxThreads = getMaxThreads();
        int limit = Math.max(1, maxThreads - MANAGEMENT_BUFFER);

        if (blockedThreadsCount.get() < limit) {
            int current = blockedThreadsCount.incrementAndGet();
            log.debug("F4: Blocking thread. Total blocked: {}/{}", current, limit);
            
            synchronized (lock) {
                try {
                    while (active.get()) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    blockedThreadsCount.decrementAndGet();
                }
            }
            return true;
        } else {
            log.warn("F4: Thread trap full ({}/{}). Allowing request to pass to avoid complete deadlock.", 
                    blockedThreadsCount.get(), limit);
            return false;
        }
    }

    private int getMaxThreads() {
        Integer max = null;
        if (serverProperties.getTomcat() != null && serverProperties.getTomcat().getThreads() != null) {
            max = serverProperties.getTomcat().getThreads().getMax();
        }
        return (max != null) ? max : 200; // Default Tomcat max threads
    }
}

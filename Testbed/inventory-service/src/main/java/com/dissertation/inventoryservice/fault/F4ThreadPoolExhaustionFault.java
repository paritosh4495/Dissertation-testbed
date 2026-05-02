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
            log.info("F4: Thread pool exhaustion trap activated. Max threads: {}, Limit: {}",
                    getMaxThreads(),getBlockLimit());
        }
    }

    @Override
    public void deactivate() {
        if (active.compareAndSet(true, false)) {
            log.info("F4: Deactivating. Releasing {} blocked threads.", blockedThreadsCount.get());
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }


    public boolean tryBlock() {
        if (!active.get()) {
            return false;
        }
        int limit = getBlockLimit();

        int current;
        do {
            current = blockedThreadsCount.get();
            if(current >= limit) {
                log.warn("F4: Thread trap full ({}/{}). Allowing request through (management buffer).",current,limit);
                return false;
            }
        } while (!blockedThreadsCount.compareAndSet(current, current + 1));

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

    }

    private int getMaxThreads() {
        Integer max = null;
        if (serverProperties.getTomcat() != null && serverProperties.getTomcat().getThreads() != null) {
            max = serverProperties.getTomcat().getThreads().getMax();
        }
        return (max != null) ? max : 200; // Default Tomcat max threads
    }


    private int getBlockLimit() {
        int max = getMaxThreads();
        int buffer = Math.max(2, max / 4);
        return max - buffer;
    }
}

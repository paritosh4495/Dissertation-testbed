package com.dissertation.inventoryservice.fault;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class F2CpuSaturationFault implements Fault {

    private final FaultRegistry registry;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final List<Thread> workers = new ArrayList<>();

    // The "Blackhole": Prevents the JIT compiler from optimizing away our math
    public volatile double blackhole;

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getId() {
        return "f2";
    }

    @Override
    public String getDescription() {
        return "Saturates CPU by spawning intensive worker threads";
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public synchronized void activate() {
        if (active.compareAndSet(false, true)) {
            int cores = Runtime.getRuntime().availableProcessors();
            int threadCount = cores + 1;
            log.info("Activating F2: Spawning {} worker threads", threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                Thread worker = new Thread(this::cpuIntensiveTask, "fault-f2-worker-" + i);
                worker.setDaemon(true);
                workers.add(worker);
                worker.start();
            }
        }
    }

    @Override
    public synchronized void deactivate() {
        if (active.compareAndSet(true, false)) {
            log.info("Deactivating F2: Stopping {} worker threads", workers.size());
            for (Thread worker : workers) {
                worker.interrupt();
            }
            for (Thread worker : workers) {
                try {
                    worker.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            workers.clear();
            log.info("F2: All Worker threads confirmed stopped");
        }
    }

    private void cpuIntensiveTask() {
        double val = 1.1;
        long iteration = 0;
        while (active.get() && !Thread.currentThread().isInterrupted()) {
            val = Math.tan(Math.atan(val));

            // Every 500 iterations, push the result to the volatile blackhole.
            // This forces the JVM to compute the math because it thinks another
            // thread might read the 'blackhole' variable at any time.
            if (++iteration % 500 == 0) {
                blackhole = val;
            }
        }
    }

    @PreDestroy
    public void cleanUp() {
        if (active.get()) {
            deactivate();
        }
    }


}

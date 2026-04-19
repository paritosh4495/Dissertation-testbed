package com.dissertation.inventoryservice.fault;

import jakarta.annotation.PostConstruct;
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
            log.info("Activating F2: Spawning {} worker threads", cores);
            
            for (int i = 0; i < cores; i++) {
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
            log.info("Deactivating F2: Stopping worker threads");
            for (Thread worker : workers) {
                worker.interrupt();
            }
            workers.clear();
        }
    }

    private void cpuIntensiveTask() {
        double val = 1.1;
        while (active.get() && !Thread.currentThread().isInterrupted()) {
            // Deterministic CPU-bound task using local variables
            val = Math.tan(Math.atan(val));
        }
    }
}

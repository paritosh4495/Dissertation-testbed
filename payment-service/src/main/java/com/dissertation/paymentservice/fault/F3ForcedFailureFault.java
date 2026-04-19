package com.dissertation.paymentservice.fault;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class F3ForcedFailureFault implements Fault {

    private final FaultRegistry registry;
    private final AtomicBoolean active = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getId() {
        return "f3";
    }

    @Override
    public String getDescription() {
        return "Forced payment authorization failures (HTTP 500)";
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void activate() {
        active.set(true);
    }

    @Override
    public void deactivate() {
        active.set(false);
    }
}

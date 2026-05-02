package com.dissertation.paymentservice.fault;

/**
 * Interface representing a deterministic fault that can be injected into the service.
 */
public interface Fault {
    /**
     * @return Unique identifier for the fault (e.g., "f1", "f2").
     */
    String getId();

    /**
     * @return Human-readable description of what the fault does.
     */
    String getDescription();

    /**
     * @return True if the fault is currently active.
     */
    boolean isActive();

    /**
     * Activates the fault. Implementation must be idempotent.
     */
    void activate();

    /**
     * Deactivates the fault. Implementation must be idempotent.
     */
    void deactivate();
}

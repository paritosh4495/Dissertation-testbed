package com.dissertation.inventoryservice.fault;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FaultRegistry {

    private final Map<String, Fault> faults = new ConcurrentHashMap<>();

    public void register(Fault fault) {
        log.info("Registering fault: {} - {}", fault.getId(), fault.getDescription());
        faults.put(fault.getId().toLowerCase(), fault);
    }

    public List<FaultResponse> getAllFaults() {
        return faults.values().stream()
                .map(f -> FaultResponse.builder()
                        .faultId(f.getId())
                        .active(f.isActive())
                        .applied(false)
                        .message(f.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    public FaultResponse activate(String faultId) {
        Fault fault = getFault(faultId);
        if (fault.isActive()) {
            return FaultResponse.builder()
                    .faultId(fault.getId())
                    .active(true)
                    .applied(false)
                    .message("Fault already active")
                    .build();
        }

        try {
            log.warn("Activating fault: {}", fault.getId());
            fault.activate();
            return FaultResponse.builder()
                    .faultId(fault.getId())
                    .active(true)
                    .applied(true)
                    .message("Fault activated")
                    .build();
        } catch (Exception e) {
            log.error("Failed to activate fault {}: {}", fault.getId(), e.getMessage());
            return FaultResponse.builder()
                    .faultId(fault.getId())
                    .active(false)
                    .applied(false)
                    .message("Activation failed: " + e.getMessage())
                    .build();
        }
    }

    public FaultResponse deactivate(String faultId) {
        Fault fault = getFault(faultId);
        boolean alreadyInactive = !fault.isActive();

        if (!alreadyInactive) {
            log.warn("Deactivating fault: {}", fault.getId());
            fault.deactivate();
            return FaultResponse.builder()
                    .faultId(fault.getId())
                    .active(false)
                    .applied(true)
                    .message("Fault deactivated")
                    .build();
        } else {
            return FaultResponse.builder()
                    .faultId(fault.getId())
                    .active(false)
                    .applied(false)
                    .message("Fault already inactive")
                    .build();
        }
    }

    private Fault getFault(String faultId) {
        Fault fault = faults.get(faultId.toLowerCase());
        if (fault == null) {
            throw new FaultNotFoundException(faultId);
        }
        return fault;
    }
}

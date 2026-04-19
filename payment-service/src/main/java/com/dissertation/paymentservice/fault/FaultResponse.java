package com.dissertation.paymentservice.fault;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FaultResponse {
    private String faultId;
    private boolean active;
    private boolean applied;
    private String message;
}

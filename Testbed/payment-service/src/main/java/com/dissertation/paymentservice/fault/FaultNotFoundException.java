package com.dissertation.paymentservice.fault;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class FaultNotFoundException extends RuntimeException {
    private final String faultId;

    public FaultNotFoundException(String faultId) {
        super("Fault not found: " + faultId);
        this.faultId = faultId;
    }
}

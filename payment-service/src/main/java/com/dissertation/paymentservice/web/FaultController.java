package com.dissertation.paymentservice.web;

import com.dissertation.paymentservice.fault.FaultNotFoundException;
import com.dissertation.paymentservice.fault.FaultRegistry;
import com.dissertation.paymentservice.fault.FaultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/fault")
@RequiredArgsConstructor
public class FaultController {

    private final FaultRegistry faultRegistry;

    @GetMapping
    public List<FaultResponse> listFaults() {
        return faultRegistry.getAllFaults();
    }

    @PostMapping("/activate/{faultId}")
    public FaultResponse activate(@PathVariable String faultId) {
        return faultRegistry.activate(faultId);
    }

    @PostMapping("/deactivate/{faultId}")
    public FaultResponse deactivate(@PathVariable String faultId) {
        return faultRegistry.deactivate(faultId);
    }

    @ExceptionHandler(FaultNotFoundException.class)
    public ResponseEntity<FaultResponse> handleNotFound(FaultNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(FaultResponse.builder()
                        .faultId(ex.getFaultId())
                        .active(false)
                        .applied(false)
                        .message(ex.getMessage())
                        .build());
    }
}

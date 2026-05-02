package com.dissertation.paymentservice.service;

import com.dissertation.paymentservice.domain.PaymentStatus;
import com.dissertation.paymentservice.dto.PaymentRequest;
import com.dissertation.paymentservice.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final com.dissertation.paymentservice.fault.F3ForcedFailureFault f3Fault;

    @Override
    public PaymentResponse authorize(PaymentRequest request) {
        if (f3Fault.isActive()) {
            log.error("F3 Fault active: Forcing payment failure for order {}", request.getOrderNumber());
            throw new RuntimeException("F3 Fault injected: Forced Payment Failure");
        }

        log.info("Processing authorization for order {} and customer {} with amount {}", 
                request.getOrderNumber(), request.getCustomerId(), request.getAmount());

        // Simulate processing delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);
        }

        return PaymentResponse.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderNumber(request.getOrderNumber())
                .status(PaymentStatus.AUTHORIZED)
                .message("Payment authorized successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
}

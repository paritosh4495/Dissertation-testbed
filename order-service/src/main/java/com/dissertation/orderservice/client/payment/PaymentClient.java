package com.dissertation.orderservice.client.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class PaymentClient {

    @CircuitBreaker(name = "payment")
    public void processPayment(String orderNumber, String customerId, BigDecimal amount) {
        log.info("Processing payment for order {} for customer {} with amount {}", orderNumber, customerId, amount);
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
        log.info("Payment processed successfully for order {}", orderNumber);
    }
}

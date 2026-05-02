package com.dissertation.orderservice.client.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentClient {

    private final RestClient paymentRestClient;

    @CircuitBreaker(name = "payment")
    public PaymentResponse authorizePayment(PaymentRequest request) {
        log.info("Requesting payment authorization for order {}", request.getOrderNumber());
        
        return paymentRestClient.post()
                .uri("/api/payments/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }
}

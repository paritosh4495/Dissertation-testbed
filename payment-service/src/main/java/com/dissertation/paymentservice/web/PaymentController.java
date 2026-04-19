package com.dissertation.paymentservice.web;

import com.dissertation.paymentservice.dto.PaymentRequest;
import com.dissertation.paymentservice.dto.PaymentResponse;
import com.dissertation.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorizations")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment authorization request for order: {}", request.getOrderNumber());
        PaymentResponse response = paymentService.authorize(request);
        return ResponseEntity.ok(response);
    }
}

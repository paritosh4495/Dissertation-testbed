package com.dissertation.paymentservice.service;

import com.dissertation.paymentservice.dto.PaymentRequest;
import com.dissertation.paymentservice.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse authorize(PaymentRequest request);
}

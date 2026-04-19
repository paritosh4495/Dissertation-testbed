package com.dissertation.orderservice.service;

import com.dissertation.orderservice.dto.CreateOrderRequest;
import com.dissertation.orderservice.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderByNumber(String orderNumber);
}

package com.dissertation.orderservice.service;

import com.dissertation.orderservice.dto.CreateOrderRequest;
import com.dissertation.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getOrdersByCustomerId(String customerId);
}

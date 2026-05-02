package com.dissertation.orderservice.mapper;

import com.dissertation.orderservice.domain.Order;
import com.dissertation.orderservice.domain.OrderItem;
import com.dissertation.orderservice.dto.OrderItemResponseDto;
import com.dissertation.orderservice.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private OrderItemResponseDto toItemResponse(OrderItem item) {
        return OrderItemResponseDto.builder()
                .productCode(item.getProductCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}

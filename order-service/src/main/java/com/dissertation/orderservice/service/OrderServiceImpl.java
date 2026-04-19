package com.dissertation.orderservice.service;

import com.dissertation.orderservice.client.inventory.*;
import com.dissertation.orderservice.client.payment.PaymentClient;
import com.dissertation.orderservice.domain.Order;
import com.dissertation.orderservice.domain.OrderItem;
import com.dissertation.orderservice.domain.OrderStatus;
import com.dissertation.orderservice.dto.CreateOrderRequest;
import com.dissertation.orderservice.dto.OrderResponse;
import com.dissertation.orderservice.mapper.OrderMapper;
import com.dissertation.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Creating order {} for customer {}", orderNumber, request.getCustomerId());

        // 1. Initialise Order as PENDING
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO) // Will be updated after reservation
                .build();
        
        order = orderRepository.save(order);

        StockOperationRequest stockRequest = StockOperationRequest.builder()
                .orderId(orderNumber)
                .items(request.getItems().stream()
                        .map(item -> OrderItemRequest.builder()
                                .productCode(item.getProductCode())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        try {
            // 2. Reserve Stock
            StockOperationResponse stockResponse = inventoryClient.reserveStock(stockRequest);
            
            if (!stockResponse.isSuccess()) {
                log.warn("Stock reservation failed for order {}", orderNumber);
                order.setStatus(OrderStatus.INVENTORY_REJECTED);
                return orderMapper.toResponse(orderRepository.save(order));
            }

            // Update order items with prices and calculate total
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ReservedItemResponse reservedItem : stockResponse.getReservedItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .productCode(reservedItem.getProductCode())
                        .quantity(reservedItem.getQuantity())
                        .unitPrice(reservedItem.getUnitPrice())
                        .build();
                order.addItem(orderItem);
                
                BigDecimal itemTotal = reservedItem.getUnitPrice().multiply(BigDecimal.valueOf(reservedItem.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);
            }
            order.setTotalAmount(totalAmount);
            order = orderRepository.save(order);

            // 3. Process Payment
            try {
                paymentClient.processPayment(orderNumber, request.getCustomerId(), totalAmount);
                
                // 4. Confirm Order
                order.setStatus(OrderStatus.CONFIRMED);
                log.info("Order {} confirmed successfully", orderNumber);
                
            } catch (Exception e) {
                log.error("Payment failed for order {}: {}", orderNumber, e.getMessage());
                
                // COMPENSATE: Release Stock
                inventoryClient.releaseStock(stockRequest);
                
                order.setStatus(OrderStatus.PAYMENT_FAILED);
            }

        } catch (Exception e) {
            log.error("Error during order creation flow for {}: {}", orderNumber, e.getMessage());
            order.setStatus(OrderStatus.INVENTORY_REJECTED);
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }
}

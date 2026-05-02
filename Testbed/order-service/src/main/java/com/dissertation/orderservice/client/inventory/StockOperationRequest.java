package com.dissertation.orderservice.client.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOperationRequest {
    private String orderId;
    private List<OrderItemRequest> items;
}

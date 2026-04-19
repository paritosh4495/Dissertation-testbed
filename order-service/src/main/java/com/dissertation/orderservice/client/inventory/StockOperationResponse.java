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
public class StockOperationResponse {
    private String orderId;
    private boolean success;
    private String message;
    private List<ReservedItemResponse> reservedItems;
    private List<String> missingItems;
}

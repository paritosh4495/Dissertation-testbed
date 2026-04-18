package com.dissertation.inventoryservice.dto;

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
    private List<String> missingItems; // List of product codes that failed
}

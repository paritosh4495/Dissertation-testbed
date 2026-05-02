package com.dissertation.orderservice.client.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedItemResponse {
    private String productCode;
    private Integer quantity;
    private BigDecimal unitPrice;
}

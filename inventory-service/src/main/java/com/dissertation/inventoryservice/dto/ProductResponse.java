package com.dissertation.inventoryservice.dto;

import com.dissertation.inventoryservice.domain.BookFormat;
import com.dissertation.inventoryservice.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String code;
    private String name;
    private String author;
    private String isbn;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String genre;
    private BookFormat format;
    private Integer stockQuantity;
    private Integer availableQuantity;
    private ProductStatus status;
}

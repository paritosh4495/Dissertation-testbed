package com.dissertation.inventoryservice.mapper;

import com.dissertation.inventoryservice.domain.Product;
import com.dissertation.inventoryservice.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;
        
        return ProductResponse.builder()
                .code(product.getCode())
                .name(product.getName())
                .author(product.getAuthor())
                .isbn(product.getIsbn())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .genre(product.getGenre())
                .format(product.getFormat())
                .stockQuantity(product.getStockQuantity())
                .availableQuantity(product.getStockQuantity() - product.getReservedQuantity())
                .status(product.getStatus())
                .build();
    }
}

package com.dissertation.inventoryservice.service;

import com.dissertation.inventoryservice.dto.*;
import java.math.BigDecimal;

public interface ProductService {


    PageResult<ProductResponse> getProducts(
            String query,
            String genre,
            String author,
            String name,
            String isbn,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int pageNo
    );

    ProductResponse getProductByCode(String code);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(String code, UpdateProductRequest request);

    void updateProductPrice(String code, BigDecimal newPrice);

    void adjustStock(String code, Integer quantity);

    StockOperationResponse reserveStock(StockOperationRequest request);

    void commitStock(StockOperationRequest request);

    void releaseStock(StockOperationRequest request);
}

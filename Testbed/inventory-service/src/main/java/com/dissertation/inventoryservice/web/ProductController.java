package com.dissertation.inventoryservice.web;

import com.dissertation.inventoryservice.dto.*;
import com.dissertation.inventoryservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResult<ProductResponse>> getProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int pageNo) {
        
        return ResponseEntity.ok(productService.getProducts(query, genre, author, name, isbn, minPrice, maxPrice, pageNo));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ProductResponse> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(productService.getProductByCode(code));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String code, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(code, request));
    }

    @PatchMapping("/{code}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable String code, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newPrice = request.get("price");
        if (newPrice == null) {
            throw new IllegalArgumentException("Price is required");
        }
        productService.updateProductPrice(code, newPrice);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{code}/stock")
    public ResponseEntity<Void> adjustStock(@PathVariable String code, @Valid @RequestBody StockAdjustmentRequest request) {
        productService.adjustStock(code, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservations")
    public ResponseEntity<StockOperationResponse> reserveStock(@Valid @RequestBody StockOperationRequest request) {
        return ResponseEntity.ok(productService.reserveStock(request));
    }

    @PostMapping("/commit")
    public ResponseEntity<Void> commitStock(@Valid @RequestBody StockOperationRequest request) {
        productService.commitStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseStock(@Valid @RequestBody StockOperationRequest request) {
        productService.releaseStock(request);
        return ResponseEntity.ok().build();
    }
}

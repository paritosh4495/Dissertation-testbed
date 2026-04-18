package com.dissertation.inventoryservice.service;

import com.dissertation.inventoryservice.ApplicationProperties;
import com.dissertation.inventoryservice.domain.Product;
import com.dissertation.inventoryservice.domain.ProductStatus;
import com.dissertation.inventoryservice.dto.*;
import com.dissertation.inventoryservice.exception.InsufficientStockException;
import com.dissertation.inventoryservice.exception.ProductAlreadyExistsException;
import com.dissertation.inventoryservice.exception.ProductNotFoundException;
import com.dissertation.inventoryservice.mapper.ProductMapper;
import com.dissertation.inventoryservice.repository.ProductRepository;
import com.dissertation.inventoryservice.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ApplicationProperties properties;
    
    private static final String CODE_PREFIX = "BK-";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    @Override
    public PageResult<ProductResponse> getProducts(
            String query,
            String genre,
            String author,
            String name,
            String isbn,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int pageNo
    ) {
        Sort sort = Sort.by("name").ascending();
        int pageIndex = pageNo <= 1 ? 0 : pageNo - 1;
        Pageable pageable = PageRequest.of(pageIndex, properties.pageSize(), sort);
        
        Specification<Product> spec = ProductSpecification.search(query, genre, author, name, isbn, minPrice, maxPrice);
        Page<Product> page = productRepository.findAll(spec, pageable);

        return new PageResult<>(
                page.getContent().stream().map(productMapper::toResponse).toList(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Override
    public ProductResponse getProductByCode(String code) {
        return productRepository.findByCodeAndStatusNot(code, ProductStatus.DISCONTINUED)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(code));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (request.getIsbn() != null && productRepository.existsByIsbn(request.getIsbn())) {
            throw new ProductAlreadyExistsException("Product with ISBN " + request.getIsbn() + " already exists");
        }

        String code = generateUniqueCode();
        
        Product product = Product.builder()
                .code(code)
                .name(request.getName())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .genre(request.getGenre())
                .format(request.getFormat())
                .stockQuantity(request.getStockQuantity())
                .reservedQuantity(0)
                .status(request.getStatus())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product with code: {}", code);
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String code, UpdateProductRequest request) {
        Product product = productRepository.findByCodeAndStatusNot(code, ProductStatus.DISCONTINUED)
                .orElseThrow(() -> new ProductNotFoundException(code));

        product.setName(request.getName());
        product.setAuthor(request.getAuthor());
        product.setIsbn(request.getIsbn());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setPrice(request.getPrice());
        product.setGenre(request.getGenre());
        product.setFormat(request.getFormat());
        product.setStatus(request.getStatus());

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void updateProductPrice(String code, BigDecimal newPrice) {
        Product product = productRepository.findByCodeAndStatusNot(code, ProductStatus.DISCONTINUED)
                .orElseThrow(() -> new ProductNotFoundException(code));
        
        if (newPrice.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Price must be at least 0.01");
        }
        
        product.setPrice(newPrice);
        productRepository.save(product);
        log.info("Updated price for product {}: {}", code, newPrice);
    }

    @Override
    @Transactional
    public void adjustStock(String code, Integer quantity) {
        Product product = productRepository.findByCodeAndStatusNot(code, ProductStatus.DISCONTINUED)
                .orElseThrow(() -> new ProductNotFoundException(code));
        
        int newQuantity = product.getStockQuantity() + quantity;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        log.info("Adjusted stock for product {}: {} -> {}", code, product.getStockQuantity() - quantity, newQuantity);
    }

    @Override
    @Transactional
    public StockOperationResponse reserveStock(StockOperationRequest request) {
        log.info("Attempting to reserve stock for order: {}", request.getOrderId());
        List<String> missingItems = new ArrayList<>();

        for (OrderItemRequest item : request.getItems()) {
            int updated = productRepository.reserveStock(item.getProductCode(), item.getQuantity());
            if (updated == 0) {
                missingItems.add(item.getProductCode());
            }
        }

        if (!missingItems.isEmpty()) {
            log.warn("Failed to reserve stock for order {}. Missing items: {}", request.getOrderId(), missingItems);
            throw new InsufficientStockException(missingItems);
        }

        return StockOperationResponse.builder()
                .orderId(request.getOrderId())
                .success(true)
                .message("Stock reserved successfully")
                .build();
    }

    @Override
    @Transactional
    public void commitStock(StockOperationRequest request) {
        log.info("Committing stock for order: {}", request.getOrderId());
        for (OrderItemRequest item : request.getItems()) {
            int updated = productRepository.commitStock(item.getProductCode(), item.getQuantity());
            if (updated == 0) {
                log.error("Failed to commit stock for product {} in order {}", item.getProductCode(), request.getOrderId());
            }
        }
    }

    @Override
    @Transactional
    public void releaseStock(StockOperationRequest request) {
        log.info("Releasing stock for order: {}", request.getOrderId());
        for (OrderItemRequest item : request.getItems()) {
            productRepository.releaseStock(item.getProductCode(), item.getQuantity());
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_PREFIX);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (productRepository.existsByCode(code));
        return code;
    }
}

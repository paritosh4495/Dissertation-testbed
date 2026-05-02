package com.dissertation.inventoryservice.web;

import com.dissertation.inventoryservice.exception.InsufficientStockException;
import com.dissertation.inventoryservice.exception.ProductAlreadyExistsException;
import com.dissertation.inventoryservice.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String serviceName;

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFoundException(ProductNotFoundException ex) {
        log.error("Product not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Product Not Found", ex.getMessage(), "PRODUCT_NOT_FOUND");
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStockException(InsufficientStockException ex) {
        log.error("Insufficient stock: {}", ex.getMessage());
        ProblemDetail pd = createProblemDetail(HttpStatus.BAD_REQUEST, "Insufficient Stock", ex.getMessage(), "INSUFFICIENT_STOCK");
        pd.setProperty("missingItems", ex.getMissingItems());
        return pd;
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ProblemDetail handleProductAlreadyExistsException(ProductAlreadyExistsException ex) {
        log.error("Product already exists: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "Product Already Exists", ex.getMessage(), "PRODUCT_ALREADY_EXISTS");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), "INVALID_ARGUMENT");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(error -> errors.append(error.getDefaultMessage()).append("; "));
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failed", errors.toString(), "VALIDATION_FAILED");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "INTERNAL_SERVER_ERROR");
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, String errorCode) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("error_code", errorCode);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}

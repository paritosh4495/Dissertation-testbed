package com.dissertation.orderservice.client.inventory;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestClient inventoryRestClient;

    @CircuitBreaker(name = "inventory")
    public StockOperationResponse reserveStock(StockOperationRequest request) {
        log.info("Requesting stock reservation for order: {}", request.getOrderId());
        return inventoryRestClient.post()
                .uri("/api/products/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(StockOperationResponse.class);
    }

    @CircuitBreaker(name = "inventory")
    public void releaseStock(StockOperationRequest request) {
        log.info("Requesting stock release for order: {}", request.getOrderId());
        inventoryRestClient.post()
                .uri("/api/products/release")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}

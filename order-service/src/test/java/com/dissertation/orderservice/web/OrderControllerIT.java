package com.dissertation.orderservice.web;

import com.dissertation.orderservice.AbstractIT;
import com.dissertation.orderservice.client.inventory.InventoryClient;
import com.dissertation.orderservice.client.inventory.ReservedItemResponse;
import com.dissertation.orderservice.client.inventory.StockOperationResponse;
import com.dissertation.orderservice.client.payment.PaymentClient;
import com.dissertation.orderservice.domain.OrderStatus;
import com.dissertation.orderservice.dto.CreateOrderRequest;
import com.dissertation.orderservice.dto.OrderItemDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrderControllerIT extends AbstractIT {

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    void shouldCreateOrderSuccessfully() {
        OrderItemDto item = OrderItemDto.builder()
                .productCode("BK-123")
                .quantity(2)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-001")
                .items(List.of(item))
                .build();

        StockOperationResponse stockResponse = StockOperationResponse.builder()
                .orderId("some-id")
                .success(true)
                .reservedItems(List.of(
                        ReservedItemResponse.builder()
                                .productCode("BK-123")
                                .quantity(2)
                                .unitPrice(new BigDecimal("29.99"))
                                .build()
                ))
                .build();

        when(inventoryClient.reserveStock(any())).thenReturn(stockResponse);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .body("orderNumber", startsWith("ORD-"))
                .body("status", equalTo("CONFIRMED"))
                .body("totalAmount", equalTo(59.98f))
                .body("items", hasSize(1))
                .body("items[0].productCode", equalTo("BK-123"))
                .body("items[0].unitPrice", equalTo(29.99f));
    }

    @Test
    void shouldFailOrderWhenStockReservationFails() {
        OrderItemDto item = OrderItemDto.builder()
                .productCode("BK-999")
                .quantity(1)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-001")
                .items(List.of(item))
                .build();

        StockOperationResponse stockResponse = StockOperationResponse.builder()
                .success(false)
                .missingItems(List.of("BK-999"))
                .build();

        when(inventoryClient.reserveStock(any())).thenReturn(stockResponse);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("INVENTORY_REJECTED"));
    }
}

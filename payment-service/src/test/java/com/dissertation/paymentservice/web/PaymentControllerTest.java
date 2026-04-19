package com.dissertation.paymentservice.web;

import com.dissertation.paymentservice.AbstractIT;
import com.dissertation.paymentservice.domain.PaymentStatus;
import com.dissertation.paymentservice.dto.PaymentRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class PaymentControllerTest extends AbstractIT {

    @Test
    @DisplayName("Should authorize payment successfully")
    void shouldAuthorizePaymentSuccessfully() {
        PaymentRequest request = PaymentRequest.builder()
                .orderNumber("ORD-12345")
                .customerId("CUST-001")
                .amount(new BigDecimal("99.99"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/payments/authorizations")
                .then()
                .statusCode(200)
                .body("status", is(PaymentStatus.AUTHORIZED.name()))
                .body("orderNumber", is("ORD-12345"))
                .body("paymentId", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("Should return 400 when request is invalid")
    void shouldReturn400WhenInvalid() {
        PaymentRequest request = PaymentRequest.builder()
                .orderNumber("") // Invalid
                .customerId("CUST-001")
                .amount(new BigDecimal("-1.00")) // Invalid
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/payments/authorizations")
                .then()
                .statusCode(400)
                .body("title", is("Validation Failed"))
                .body("error_code", is("VALIDATION_FAILED"));
    }
}

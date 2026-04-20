package com.dissertation.inventoryservice.web;

import com.dissertation.inventoryservice.AbstractIT;
import com.dissertation.inventoryservice.domain.BookFormat;
import com.dissertation.inventoryservice.domain.ProductStatus;
import com.dissertation.inventoryservice.dto.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Sql("/test-data.sql")
class ProductControllerTest extends AbstractIT {

    @Nested
    @DisplayName("GET /api/products")
    class GetProductsTests {

        @Test
        @DisplayName("Should return paginated products")
        void shouldReturnPaginatedProducts() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/api/products")
                    .then()
                    .statusCode(200)
                    .body("data", hasSize(greaterThanOrEqualTo(5)))
                    .body("totalElements", is(greaterThanOrEqualTo(5)));
        }

        @Test
        @DisplayName("Should filter products by genre")
        void shouldFilterByGenre() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("genre", "Fantasy")
                    .when()
                    .get("/api/products")
                    .then()
                    .statusCode(200)
                    .body("data", everyItem(hasEntry("genre", "Fantasy")));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{code}")
    class GetProductByCodeTests {

        @Test
        @DisplayName("Should return product by code")
        void shouldReturnProductByCode() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/api/products/BK-HP003")
                    .then()
                    .statusCode(200)
                    .body("code", is("BK-HP003"))
                    .body("name", is("Harry Potter and the Philosopher's Stone"));
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void shouldReturn404WhenNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/api/products/NON-EXISTENT")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Test Book")
                    .author("Test Author")
                    .isbn("1234567890")
                    .price(new BigDecimal("29.99"))
                    .stockQuantity(10)
                    .status(ProductStatus.AVAILABLE)
                    .format(BookFormat.HARDCOVER)
                    .genre("Testing")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/products")
                    .then()
                    .statusCode(201)
                    .body("code", notNullValue())
                    .body("name", is("Test Book"));
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{code}")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void shouldUpdateProductSuccessfully() {
            UpdateProductRequest request = UpdateProductRequest.builder()
                    .name("Updated Book")
                    .author("George Orwell")
                    .price(new BigDecimal("9.99"))
                    .status(ProductStatus.AVAILABLE)
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/api/products/BK-1984")
                    .then()
                    .statusCode(200)
                    .body("name", is("Updated Book"))
                    .body("price", is(9.99f));
        }
    }

    @Nested
    @DisplayName("PATCH /api/products/{code}/price")
    class UpdatePriceTests {

        @Test
        @DisplayName("Should update price successfully")
        void shouldUpdatePriceSuccessfully() {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("price", 15.50))
                    .when()
                    .patch("/api/products/BK-EJ002/price")
                    .then()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/api/products/BK-EJ002")
                    .then()
                    .statusCode(200)
                    .body("price", is(15.50f));
        }
    }

    @Nested
    @DisplayName("PATCH /api/products/{code}/stock")
    class AdjustStockTests {

        @Test
        @DisplayName("Should adjust stock successfully")
        void shouldAdjustStockSuccessfully() {
            StockAdjustmentRequest request = new StockAdjustmentRequest(10, "Restock");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .patch("/api/products/BK-GTM/stock")
                    .then()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/api/products/BK-GTM")
                    .then()
                    .statusCode(200)
                    .body("stockQuantity", is(25)); // Initial 15 + 10
        }
    }

    @Nested
    @DisplayName("Stock Operations (Reservations/Commit/Release)")
    class StockOperationTests {

        @Test
        @DisplayName("Should reserve, commit and release stock successfully")
        void shouldHandleStockLifecycle() {
            String orderId = "ORD-TEST-1";
            StockOperationRequest request = StockOperationRequest.builder()
                    .orderId(orderId)
                    .items(List.of(new OrderItemRequest("BK-TH001", 5)))
                    .build();

            // 1. Reserve
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/products/reservations")
                    .then()
                    .statusCode(200)
                    .body("success", is(true));

            given()
                    .when()
                    .get("/api/products/BK-TH001")
                    .then()
                    .statusCode(200)
                    .body("availableQuantity", is(45)); // Initial 50 - 5

            // 2. Release
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/products/release")
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .get("/api/products/BK-TH001")
                    .then()
                    .statusCode(200)
                    .body("availableQuantity", is(50));

            // 3. Reserve again for commit
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/products/reservations")
                    .then()
                    .statusCode(200);

            // 4. Commit
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/products/commit")
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .get("/api/products/BK-TH001")
                    .then()
                    .statusCode(200)
                    .body("stockQuantity", is(45))
                    .body("availableQuantity", is(45));
        }
    }
}

# Project Overview: Dissertation Testbed

This project is a microservices-based testbed simulating a simple e-commerce bookstore.
It is built for dissertation research at Newcastle University (MSc Advanced Computer Science).

The simulation provides a realistic distributed system foundation. Once the bookstore
simulation is complete and stable, the project will be extended with fault injection
and an LLM-based diagnostic agent — but that comes later.


## Architecture
The system consists of three primary services:
1.  **Inventory Service (Port 8081):** Manages product catalog and stock levels. Supports stock reservation, committing, and releasing.
2.  **Order Service (Port 8082):** Orchestrates the order creation process. It coordinates with the Inventory and Payment services using a SAGA-like pattern (Orchestration).
3.  **Payment Service (Port 8083):** A mock service that simulates payment authorization.

## Technology Stack
- **Language:** Java 25
- **Framework:** Spring Boot 3.5.13
- **Data Access:** Spring Data JPA (PostgreSQL)
- **Database Migrations:** Flyway
- **Communication:** Spring RestClient (Synchronous HTTP)
- **Resilience:** Resilience4j (Circuit Breakers)
- **Observability**:
  - Spring Boot Actuator.
  - Micrometer with Prometheus registry.
  - Structured Logging in ECS (Elastic Common Schema) format.
- **Testing:** Testcontainers (PostgreSQL), RestAssured, JUnit 5
- **Build/Deployment:** Maven, Docker, Docker Compose, Taskfile

## Key Workflows
### Order Creation (Orchestration SAGA)
1. `order-service` receives a `CreateOrderRequest`.
2. It initializes an Order with `PENDING` status.
3. It calls `inventory-service` to reserve stock.
   - If stock reservation fails, order status becomes `INVENTORY_REJECTED`.
4. If reservation succeeds, it calls `payment-service` to authorize payment.
   - If payment succeeds, order status becomes `CONFIRMED`.
   - If payment fails, it calls `inventory-service` to **release** the reserved stock (Compensation) and sets order status to `PAYMENT_FAILED`.

### Infrastructure
- **PostgreSQL:** A single container hosts two logical databases — `inventory` and `order`.
  Initialised via `deployment/docker-compose/init.sql`.



## Building and Running

### Prerequisites
- Java 25
- Docker and Docker Compose
- [Task](https://taskfile.dev/) (optional, but recommended)

### Commands
- **Build all modules:**
  ```bash
  ./mvnw clean verify
  ```
  *Note: This runs tests which require Docker (Testcontainers).*

- **Build Docker images:**
  ```bash
  task build
  ```

- **Start Infrastructure (PostgreSQL):**
  ```bash
  task start_infra
  ```

- **Start all services (Full Stack):**
  ```bash
  task start
  ```

- **Stop all services:**
  ```bash
  task stop
  ```

- **Run individual service locally:**
  ```bash
  cd [service-name]
  ../mvnw spring-boot:run
  ```

## Development Conventions
- **Package Structure:** Standard Spring Boot DDD-lite:
  - `domain`: JPA Entities and Enums.
  - `dto`: Request and Response objects.
  - `repository`: Spring Data JPA repositories.
  - `service`: Business logic interfaces and implementations.
  - `web`: REST Controllers and Exception Handlers.
  - `client`: External service clients (in `order-service`).
  - `mapper`: MapStruct or manual mappers (Lombok is used extensively).
- **Resilience:** All external calls should be wrapped in Resilience4j Circuit Breakers.
- **Testing:**
  - Unit tests for services and logic.
  - Integration tests (`AbstractIT`) using Testcontainers to spin up real PostgreSQL instances.
  - API verification using RestAssured.
- **Configuration:** Externalized via `application.yml`. Profile-specific configurations (e.g., `application-docker.yml`) are used for containerized environments.

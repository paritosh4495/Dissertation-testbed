# Gemini Context: Dissertation Testbed

This project is a microservices-based testbed simulating an e-commerce bookstore, designed for research on distributed systems, fault injection, and AI-driven diagnostics.

## Project Architecture
The system follows a microservices architecture built with **Java 25** and **Spring Boot 3.5.x**.

### Core Services
- **Inventory Service (`/inventory-service`)**: Manages product catalog and stock levels. Uses PostgreSQL for persistence.
- **Order Service (`/order-service`)**: Orchestrates order creation, communicating with Inventory and Payment services via `RestClient`.
- **Payment Service (`/payment-service`)**: Simulates payment processing.

### Infrastructure & Deployment
- **Database**: PostgreSQL (managed via Docker/Kubernetes).
- **Migration**: Flyway handles schema versioning.
- **Resilience**: Resilience4j is used for circuit breakers and retries.
- **Deployment**:
    - **Docker Compose**: Located in `deployment/docker-compose/`. `infra.yml` for infrastructure and `apps.yml` for services.
    - **Kubernetes**: Manifests in `deployment/kubernetes/`.
- **Observability:** Spring Boot Actuator, Micrometer (Prometheus format), Structured Logging (ECS format).
- **Testing:** JUnit 5, Testcontainers, RestAssured.
- **Automation**: `Taskfile.yml` provides shortcuts for common operations.

## Key Workflows

### Building and Testing
- **Standard Maven**: `mvn clean verify` runs all tests including integration tests.
- **Docker Images**: `task build` builds Spring Boot OCI images.
- **Integration Tests**: Uses **Testcontainers** (PostgreSQL) and **RestAssured**.

### Running the System
- **Infrastructure Only**: `task start_infra`
- **Full Stack (Docker)**: `task start` (Note: builds images first).
- **Kubernetes Deployment**: `task k8s_start` (Note: requires a running K8s cluster and pre-built images).

## Fault Injection Framework

The system includes a custom fault injection mechanism accessible via `FaultController` in each service.

| Fault ID | Name | Service | Description |
| :--- | :--- | :--- | :--- |
| **F1** | Connection Pool Starvation | Inventory | Exhausts HikariCP connection pool. |
| **F2** | CPU Saturation | Inventory | Spawns threads to consume 100% CPU. |
| **F3** | Forced Failure | Payment | Causes the service to return 500 errors. |
| **F4** | Thread Pool Exhaustion | Inventory | Saturates the Tomcat/Server thread pool. |
| **F5** | Slow Memory Leak | Inventory | Gradually consumes heap memory. |


### Fault Injection (Experimental)
Each service (currently Inventory and Payment) features a runtime fault injection mechanism.
- **Endpoint**: `/internal/fault`
- **Actions**:
    - `GET /internal/fault`: List available faults.
    - `POST /internal/fault/activate/{faultId}`: Enable a specific fault (e.g., `f4` for thread pool exhaustion).
    - `POST /internal/fault/deactivate/{faultId}`: Disable the fault.

## Development Conventions

### Coding Style
- **Java 25 Features**: Use modern Java features (records, pattern matching, etc.).
- **Lombok**: Extensively used for boilerplate reduction (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`).
- **Validation**: JSR-303/JSR-380 bean validation in DTOs.
- **Mapping**: MapStruct is inferred for DTO/Entity mapping (check `target/generated-sources`).

### Testing Practices
- **Unit Tests**: Standard JUnit 5.
- **Integration Tests**: Found in `*IT.java` files. They extend `AbstractIT` which typically manages Testcontainers lifecycle.
- **API Testing**: RestAssured is used in controller-level integration tests.

### Project Layout
- `src/main/java/.../domain`: JPA Entities.
- `src/main/java/.../dto`: Request/Response objects.
- `src/main/java/.../fault`: Fault injection logic.
- `src/main/resources/db/migration`: Flyway SQL scripts.

## LLM Interaction Guidelines
- **Contextual Awareness**: When modifying a service, always check its specific `application.yml` for configuration and port mappings.
- **Port Mappings (Docker)**:
    - Inventory Service: `8081`
    - Order Service: `8080`
    - Payment Service: `8082`
    - Postgres: `5432`
- **Sub-Agent Usage**: Use `codebase_investigator` for complex cross-service logic analysis or dependency mapping.

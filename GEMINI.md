# Project Overview: Dissertation Testbed

This project is a microservices-based testbed simulating a simple e-commerce bookstore.
It is built for dissertation research at Newcastle University (MSc Advanced Computer Science).

The simulation provides a realistic distributed system foundation. Once the bookstore
simulation is complete and stable, the project will be extended with fault injection
and an LLM-based diagnostic agent — but that comes later.


## Architecture & Technologies
- **Java 25**: Utilizes the latest Java features.
- **Spring Boot 3.5.x**: Core framework for microservices.
- **Microservices**:
  - `inventory-service`: Manages product inventory and stock.
  - `order-service`: Manages customer orders.
- **Persistence**: Spring Data JPA with PostgreSQL for each service.
- **Database Migrations**: Flyway.
- **Reliability**: Resilience4j for circuit breakers and fault tolerance.
- **Observability**:
  - Spring Boot Actuator.
  - Micrometer with Prometheus registry.
  - Structured Logging in ECS (Elastic Common Schema) format.
- **Testing**:
  - JUnit 5 for unit and integration tests.
  - RestAssured for API testing.
  - Testcontainers for integration testing with real PostgreSQL instances.
- **Infrastructure**: Docker Compose for local development and deployment.
- **Automation**: `Taskfile.yml` for common development tasks.


## Project Structure

```
testbed/                          ← root Maven multi-module project
|__ .github 
    -- workflows/                   ← GitHub Actions CI/CD pipelines

├── pom.xml                       ← parent POM (manages versions for all modules)
├── inventory-service/            ← Spring Boot module
├── order-service/                ← Spring Boot module
├── payment-service/              ← Spring Boot module (skeleton not yet created)
├── deployment/
│   └── docker-compose/
│       ├── infra.yml             ← PostgreSQL container
│       ├── apps.yml              ← application containers
│       └── init.sql              ← creates inventory + order databases
└── Taskfile.yml                  ← centralised task runner
   
```


### Infrastructure
- **PostgreSQL:** A single container hosts two logical databases — `inventory` and `order`.
  Initialised via `deployment/docker-compose/init.sql`.


## Building and Running


### Key Commands
The project uses `Taskfile.yml` to automate common operations.

- **Run all tests**:
  ```powershell
  task test
  # OR
  ./mvnw clean verify
  ```
- **Build Docker images**:
  ```powershell
  task build
  ```
- **Start Infrastructure (PostgreSQL)**:
  ```powershell
  task start_infra
  ```
- **Stop Infrastructure**:
  ```powershell
  task stop_infra
  ```
- **Start All Services and Infrastructure**:
  ```powershell
  task start
  ```
- **Stop All**:
  ```powershell
  task stop
  ```

## Development Conventions

### Coding Style
- **Lombok**: Extensively used to reduce boilerplate (e.g., `@Data`, `@Slf4j`, `@RequiredArgsConstructor`).
- **Modern Java**: Use Java 25 idiomatic features where applicable.
- **DTOs**: Used for request/response bodies in controllers.
- **Global Exception Handling**: Implemented via `@RestControllerAdvice`.

### Database
- Flyway migrations are located in `src/main/resources/db/migration`.
- Naming convention: `V1__init_schema.sql`, `V2__seed_data.sql`, etc.
- Entity-to-Database validation (`ddl-auto: validate`) is used in production/local profiles.

### Testing
- Integration tests should extend `AbstractIT`.
- `AbstractIT` uses `TestcontainersConfiguration` to start a PostgreSQL container.
- Use `RestAssured` for asserting API responses in integration tests.
- Test data should be managed via `src/test/resources/test-data.sql` or specific Flyway migrations for tests.

### Observability
- All services have Actuator endpoints enabled on port `8081` (Inventory) and `8082` (Order).
- Structured logging is enabled by default to facilitate log analysis.
- Graceful shutdown is enabled for all services.

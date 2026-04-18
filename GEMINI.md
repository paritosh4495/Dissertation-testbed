# Project Overview: Bookstore Testbed

This project is a microservices-based testbed simulating a simple e-commerce bookstore.
It is built for dissertation research at Newcastle University (MSc Advanced Computer Science).

The simulation provides a realistic distributed system foundation. Once the bookstore
simulation is complete and stable, the project will be extended with fault injection
and an LLM-based diagnostic agent — but that comes later.


## Core Technologies
- **Java:** Version 25
- **Framework:** Spring Boot 3.5.13
- **Persistence:** Spring Data JPA with PostgreSQL
- **Migrations:** Flyway
- **Resilience:** Resilience4j
- **Observability:** Spring Boot Actuator, Micrometer (Prometheus), and Structured Logging (ECS format)
- **Containerization:** Docker & Docker Compose
- **Build / Automation:** Maven (multi-module, with wrapper) and Task (Taskfile)
- **Testing:** JUnit 5, Testcontainers (for integration tests)


## Services

| Service            | Port | Database                 | Role                                      |
|--------------------|------|--------------------------|-------------------------------------------|
| `inventory-service`| 8081 | `inventory` (PostgreSQL) | Manages book stock and catalogue  |
| `order-service`    | 8082 | `orders` (PostgreSQL)    | Manages customer orders           |
| `payment-service`  | 8083 | None                     | Processes payments for orders             |

### Infrastructure
- **PostgreSQL:** A single container hosts two logical databases — `inventory` and `order`.
  Initialised via `deployment/docker-compose/init.sql`.
  `payment-service` has no database — it is stateless.


## Project Structure

```
testbed/                          ← root Maven multi-module project
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

## Development Conventions

### Coding Style
- **Lombok:** Extensively used to reduce boilerplate (Data, NoArgsConstructor, etc.).
- **Configuration Processor:** Used for type-safe configuration properties.
- **Validation:** Standard Bean Validation (JSR 380) is used in controllers/entities.

### Observability
- All services expose Prometheus metrics at `/actuator/prometheus`.
- Health checks are detailed and include circuit breaker and DB status.
- Logging is structured in ECS format for better log aggregation compatibility.

### General Rules
- Never hardcode credentials — use environment variables or Spring profiles
- Never commit `.env` files
- All schema changes via Flyway migrations only — never modify the DB directly
- Use the `docker` Spring profile when running inside Docker Compose
-
## Infrastructure Details
- **Database:** A single PostgreSQL container is used, initialized with `deployment/docker-compose/init.sql` to create `inventory` and `order` databases.
- **Graceful Shutdown:** Configured for all services to ensure connection draining.

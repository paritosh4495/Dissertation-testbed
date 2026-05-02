# Gemini Project Context: Dissertation Testbed

This project is a microservices-based testbed simulating an e-commerce bookstore, developed for dissertation research at Newcastle University. It serves as a foundation for experimenting with fault injection and LLM-based diagnostics in distributed systems.
## Research Overview
This project evaluates the impact of **Framework-Native vs. Generic Observability** on LLM-based fault diagnosis in Spring Boot microservices. It compares two observability regimes (Condition A vs. Condition B) across a suite of deterministic faults.

## Project Overview

- **Purpose**: Research platform for microservices reliability, fault injection, and automated diagnostics.
- **Architecture**: A distributed system consisting of three core Spring Boot microservices, a PostgreSQL database, and a Python-based load generator.
- **Key Services**:
    - **Inventory Service**: Manages product catalog and stock. Includes a custom fault injection framework.
    - **Order Service**: Orchestrates the order lifecycle, interacting with Inventory and Payment services.
    - **Payment Service**: Handles transaction processing simulation.
    - **Load Generator**: Simulates concurrent users browsing and purchasing items.

## Technology Stack

- **Runtime**: Java 25
- **Framework**: Spring Boot 3.5.13
- **Data**: PostgreSQL 16, Spring Data JPA, Flyway migrations
- **Communication**: REST (using Spring RestClient)
- **Resilience**: Resilience4j (Circuit Breakers, etc.)
- **Observability**: Spring Boot Actuator, Micrometer, Prometheus
- **Build/Dev**: Maven (via `mvnw`), Taskfile (`task`)
- **Infrastructure**: Docker Compose, Kubernetes (Minikube)
- **Testing**: JUnit 5, Testcontainers, RestAssured



## Core Commands (via Taskfile)

The project uses `task` (Taskfile.yml) to automate common operations.

| Command | Description |
| :--- | :--- |
| `task build` | Builds Spring Boot Docker images for all services. |
| `task start` | Launches the full stack using Docker Compose. |
| `task stop` | Stops and removes all containers. |
| `task test` | Runs all tests across services (requires Docker for Testcontainers). |
| `task k8s_start` | Deploys the entire stack to a Kubernetes namespace (`bookstore-testbed`). |
| `task k8s_stop` | Removes all Kubernetes resources. |


## Fault Taxonomy (`/internal/fault`)
The system supports six deterministic faults injected via internal REST endpoints:
- **`f1` (Connection Pool Starvation)**: Reduces HikariCP pool size and adds JDBC delay in `inventory-service`.
- **`f2` (CPU Saturation)**: Spawns CPU-intensive worker threads in `inventory-service`.
- **`f3` (Circuit Breaker Trip)**: Forces HTTP 500 failures in `payment-service`
    causing the `order-service` Resilience4j circuit breaker to trip OPEN.
    Observable via HTTP 503 on order creation and
    `resilience4j.circuitbreaker.state{name="payment"}` metric in `order-service`.
- **`f4` (Thread Pool Exhaustion)**: Saturates the Tomcat thread pool (max: 20 threads)
    in `inventory-service`. Observable via `http.server.requests` latency spike and
    `jvm.threads.live` metric. `/internal/fault` and `/actuator` paths are excluded from trapping.
- **`f5` (Slow Memory Leak)**: Gradually retains objects in the JVM heap of `inventory-service`.
- **`f6` (Pod OOMKill)**: Allocates off-heap native memory (ByteBuffer.allocateDirect)
  at 50MB/500ms in `inventory-service` until the container exceeds its 850Mi limit.
  The Linux OOM killer terminates the process (exit code 137).
  Observable ONLY via Kubernetes layer: `kubectl describe pod` shows `OOMKilled`.
  Pod restarts automatically. All fault states reset on restart.
  **Kubernetes-only** — no OOMKill occurs in Docker Compose (no memory limit enforced).
- 
## Fault Injection API

All services expose fault control at:
- `POST /internal/fault/activate/{faultId}` — activate a fault
- `POST /internal/fault/deactivate/{faultId}` — deactivate a fault
- `GET  /internal/fault` — list all faults and their current state

**Service ports (local-kubernetes (minikube) / docker-compose):**
| Service | Port |
|---|---|
| inventory-service | 8081 |
| order-service | 8082 |
| payment-service | 8083 |

**F3 requires activating on payment-service (port 8083), not order-service.**

## Development Conventions

- **Fault Injection**: The `inventory-service` contains a `fault` package with a `FaultRegistry`. Faults (e.g., `f1` for Connection Pool Starvation) can be toggled via the `FaultController`.
- **API Standards**: Services follow RESTful principles. Global exception handling is centralized in `web/GlobalExceptionHandler.java`.
- **Database Migrations**: Every service uses Flyway. SQL migrations are located in `src/main/resources/db/migration/`.
- **Testing**: Integration tests (`AbstractIT.java`) utilize Testcontainers to spin up a real PostgreSQL instance. Always ensure Docker is running before executing tests.
- **CI/CD**: GitHub Actions workflows are defined in `.github/workflows/` for each service.

## Directory Structure Highlights

- `/deployment`: Contains Docker Compose (`docker-compose/`) and Kubernetes (`kubernetes/`) manifests.
- `/inventory-service`: The most feature-rich service, containing the fault injection logic.
- `/order-service`: Demonstrates service-to-service communication via `RestClient`.
- `/load-generator`: A simple Python script (`loadgen.py`) for generating synthetic traffic.
- `/pom.xml`: Root Maven configuration aggregating the modules.

## LLM Interaction Guidelines

- **Surgical Edits**: When modifying logic, adhere to the existing `Service` -> `Repository` -> `Controller` pattern.
- **Testing**: When adding features, always add or update the corresponding `AbstractIT` based integration test.
- **Faults**: When asked to implement a new fault, follow the `Fault` interface pattern and register it in the `FaultRegistry`.

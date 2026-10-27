# Product Catalog Service

A Java 25 / Spring Boot REST service for managing product catalog data. The project focuses on a maintainable API, PostgreSQL persistence and migrations, request validation, caching configuration, automated tests, containerization, and a CI workflow.

> **Implementation status:** The repository includes a PostgreSQL-backed transactional outbox model and tests for persisting product lifecycle events. Outbox-to-Kafka publishing or Debezium CDC delivery should not be considered implemented until the publisher/connector and end-to-end delivery have been configured and verified.

## Contents

- [Architecture](#architecture)
- [Technology stack](#technology-stack)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Run locally](#run-locally)
- [Docker](#docker)
- [API](#api)
- [API documentation](#api-documentation)
- [Testing](#testing)
- [Transactional outbox](#transactional-outbox)
- [Observability and health](#observability-and-health)
- [CI and container publishing](#ci-and-container-publishing)
- [Design considerations and next steps](#design-considerations-and-next-steps)

## Architecture

The current service is organized as a Spring Boot application with a REST controller, service layer, JPA repositories, and PostgreSQL persistence.

```text
Client
  |
  v
Product Catalog REST API (Spring Boot)
  |
  v
ProductService
  |                         \
  v                          v
PostgreSQL product data   PostgreSQL outbox records
  |
  v
Flyway-managed schema
```

Redis connection and Spring caching dependencies are configured in the application. Actual cache behavior depends on the service's cache annotations and runtime configuration.

The outbox provides a place to persist product events in PostgreSQL. A separate publisher or CDC connector is required to deliver these records to Kafka or another event broker. A downstream search service and search index are architectural extensions; they are not claimed here as part of this service's verified runtime behavior.

## Technology stack

- **Language/runtime:** Java 25 (LTS)
- **Application framework:** Spring Boot 4.1.2, Spring MVC
- **Persistence:** Spring Data JPA, PostgreSQL
- **Schema migrations:** Flyway
- **Caching integration:** Spring Cache and Spring Data Redis
- **API documentation:** SpringDoc OpenAPI / Swagger UI
- **Observability dependencies:** Spring Boot Actuator, Micrometer Prometheus registry, Micrometer tracing bridge, OpenTelemetry Zipkin exporter, Logstash Logback Encoder
- **Mapping:** MapStruct
- **Testing:** JUnit 5, Spring Boot Test, MockMvc, Mockito, AssertJ, H2, Testcontainers for PostgreSQL and Redis
- **Build:** Gradle Kotlin DSL, JaCoCo
- **Containerization / CI:** Docker and GitHub Actions; Trivy image scanning and GitHub Container Registry publishing as configured in the workflow

Dependency presence does not by itself mean that a tracing backend, metrics scraper, Kafka publisher, or production deployment is running. See the relevant sections below.

## Project structure

```text
product-catalog/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/dev/jahlopez/productcatalog/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── event/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/java/dev/jahlopez/productcatalog/
│       ├── controller/
│       ├── integration/
│       └── repository/
└── .github/workflows/
```

This is a representative layout; the exact package contents may evolve as the project develops.

## Prerequisites

- JDK 25
- Docker Desktop or another working Docker Engine for Testcontainers and container builds
- PostgreSQL for local application runs
- Redis if the configured cache integration is required by the active application configuration

## Configuration

The application configuration uses these environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL hostname |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `productdb` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `password` | Database password; local-development default only |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` in the suggested configuration | Trace sampling probability, if configured in `application.yml` |

The datasource URL is assembled from the `DB_*` variables. When running inside Docker Compose, use the database/cache service names as hostnames rather than `localhost`.

**Security note:** Do not use the example database password in shared, staging, or production environments. Supply secrets through an appropriate secret-management mechanism.

The application uses Hibernate schema validation (`ddl-auto: validate`) and Flyway migrations. The database schema must therefore be created and maintained by the migration scripts before Hibernate validates it. `baseline-on-migrate` should be enabled only when deliberately adopting Flyway for a pre-existing schema.

## Run locally

1. Start PostgreSQL and create the configured database (default: `productdb`).
2. Start Redis if it is required by the application's active configuration.
3. Set environment variables as needed.
4. Start the application:

```bash
./gradlew bootRun
```

Run the unit and application test suite with:

```bash
./gradlew test
```

Generate JaCoCo reports with:

```bash
./gradlew jacocoTestReport
```

The HTML report is generated under `build/reports/jacoco/test/html/`; the XML report is generated under `build/reports/jacoco/test/jacocoTestReport.xml`.

## Docker

The Dockerfile uses a multi-stage build with a Java 25 JDK build stage and a Java 25 JRE runtime stage. The runtime image runs the application as a non-root user and exposes port `8080`. The image build skips tests (`bootJar -x test`), so tests should run in CI before the image is published.

Build the image from the repository root:

```bash
docker build -t product-catalog:local .
```

Run it with PostgreSQL on the host (Docker Desktop for macOS/Windows supports `host.docker.internal`):

```bash
docker run --rm --name product-catalog \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=productdb \
  -e DB_USER=postgres \
  -e DB_PASSWORD=change-me \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  product-catalog:local
```

Replace the sample credentials. If PostgreSQL or Redis is not available on the host, change the host settings or run the application and dependencies on the same Docker network. This example supplies connection settings; it does not start PostgreSQL or Redis for you.

## API

The controller tests exercise the following routes. Confirm the controller implementation before treating this list as a complete API contract.

| Method | Path | Tested behavior |
|---|---|---|
| `GET` | `/api/v1/products` | Returns a JSON list of products (`200 OK`) |
| `GET` | `/api/v1/products/{id}` | Returns a product (`200 OK`) or not found (`404 Not Found`) |
| `POST` | `/api/v1/products` | Creates a product (`201 Created`) or rejects invalid input (`400 Bad Request`) |
| `DELETE` | `/api/v1/products/{id}` | Deletes a product (`204 No Content`) or returns not found (`404 Not Found`) |

Request validation is exercised in the controller test for an invalid SKU/name and negative price. The service is verified not to be called when that request is rejected.

## API documentation

With the application running, SpringDoc is configured for:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The final Swagger UI route can depend on SpringDoc version and configuration; if the first URL redirects or returns 404, try `http://localhost:8080/swagger-ui/index.html`.

## Testing

The test suite includes distinct test layers:

- **Controller slice tests:** `@WebMvcTest` with MockMvc and a mocked `ProductService`, covering HTTP status codes, JSON response fields, validation, and not-found handling.
- **Repository tests:** `@DataJpaTest` with PostgreSQL Testcontainers for outbox persistence and querying unprocessed records.
- **Service integration tests:** `@SpringBootTest` with PostgreSQL and Redis Testcontainers to exercise product operations and inspect outbox records.
- **Mapper tests:** verify DTO/entity mapping behavior.

Testcontainers requires a working Docker environment. If Docker is unavailable, container-backed tests may be skipped or fail to start; a successful Gradle process alone should not be interpreted as proof that skipped integration tests executed. Check the Gradle test report for passed, failed, and skipped counts.

Run an individual test class, for example:

```bash
./gradlew test --tests "dev.jahlopez.productcatalog.unit.mapper.controller.ProductControllerTest"
./gradlew test --tests "dev.jahlopez.productcatalog.unit.mapper.repository.OutboxRepositoryTest"
./gradlew test --tests "dev.jahlopez.productcatalog.integration.ProductOutboxIntegrationTest"
```

JaCoCo report generation is configured after tests. A generated report does not enforce a minimum coverage threshold unless a coverage-verification task is configured and included in the CI command.

## Transactional outbox

The `OutboxEvent` entity maps to the PostgreSQL `outbox` table and stores:

- `eventId` (UUID, unique; matches `eventId` in the payload)
- `aggregateType` (`product`) and `aggregateId`
- `eventType` (`ProductCreated` / `ProductDeleted`)
- `payload` (JSON serialized as text, using Product Event schema version 1)
- `schemaVersion` (Debezium Connect schema metadata)
- `createdAt`
- `processed` status

The V2 migration adds `schema_version` with a default of `1`; new event payloads
also include `schemaVersion: 1`. Product Search validates the expanded JSON
payload against its versioned event schema before processing.

The repository exposes `findByProcessed(boolean)` for querying records by processing status. Current tests verify that product creation and deletion persist corresponding outbox records, and that unprocessed records can be retrieved.

**Delivery limitation:** An outbox table and repository query do not publish events on their own. Before claiming end-to-end delivery, implement and test either a reliable polling publisher or a configured CDC path such as Debezium/Kafka Connect. Verify transaction atomicity, retries, duplicate delivery/idempotency, ordering/version handling, and outbox retention/cleanup. No Kafka client or Kafka Testcontainers dependency is present in the supplied `build.gradle.kts`, so Kafka publishing is not documented as currently verified.

## Observability and health

The supplied configuration exposes Actuator endpoints for health, info, Prometheus metrics, and metrics, and enables Spring Boot health probes. Common endpoints are:

- Health: `http://localhost:8080/actuator/health`
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Prometheus: `http://localhost:8080/actuator/prometheus`
- Metrics: `http://localhost:8080/actuator/metrics`

These URLs assume the app is running on port 8080 and that the supplied configuration is active. Endpoint exposure does not mean an external Prometheus server is scraping metrics. Likewise, tracing dependencies and a sampling setting do not prove spans reach a collector; ensure the exporter dependency, endpoint, and backend configuration agree before claiming tracing is operational.

For non-local environments, avoid exposing detailed health information to unauthenticated callers and choose a trace-sampling rate appropriate for traffic and cost.

## CI and container publishing

The supplied GitHub Actions workflow is configured to:

1. Run `./gradlew test` using Temurin Java 25.
2. Build a Docker image and scan it with Trivy for `HIGH` and `CRITICAL` findings, uploading SARIF results to GitHub Code Scanning.
3. On a push to `main`, publish the image to GitHub Container Registry (GHCR) after the required preceding jobs succeed.
4. Tag the image with the commit SHA and `latest`.

The vulnerability scan is limited to the configured severities and scanner behavior; it does not prove the image is vulnerability-free. Pin third-party GitHub Actions to reviewed versions or commit SHAs instead of mutable branches, and keep workflow permissions minimal. The workflow publishes an image; it does not by itself demonstrate deployment to Kubernetes/EKS.

## Design considerations and next steps

Potential next steps, to be marked complete only after implementation and verification:

- Add/verify a reliable outbox publisher or Debezium CDC connector and end-to-end event delivery tests.
- Add rollback/atomicity tests proving product changes and outbox records commit or roll back together.
- Assert outbox payload fields by parsing JSON rather than checking only for a substring.
- Add tests for product updates, malformed JSON, invalid path IDs, and individual validation constraints where supported by the controller contract.
- Verify Redis cache behavior with tests and confirm cache invalidation on product changes.
- Align OTLP/Zipkin dependencies and endpoint configuration, then verify trace export to a running backend.
- Add a deliberate JaCoCo coverage threshold and run `./gradlew check` in CI if coverage enforcement is desired.
- Add deployment manifests and runtime verification before claiming Kubernetes/EKS deployment support.

---

## License

Add the repository's chosen license here. Until a license is selected and included in the repository, reuse and redistribution terms should not be assumed.

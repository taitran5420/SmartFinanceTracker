# SmartFinanceTracker

A Spring Boot REST API for personal finance tracking: transactions, budgets, categories, recurring (scheduled) transactions, analytics, and real-time notifications.

## Stack

- Java 21, Spring Boot 4.0.6
- Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL
- Quartz (clustered, JDBC job store) for recurring transactions
- Spring Kafka for inter-module events, Redis (Spring Cache) for analytics caching
- springdoc + Scalar for API docs
- Maven, Lombok

## Features

- **Auth** - register/login with JWT issuance.
- **Transactions** - income/expense tracking with idempotency keys, overdraft protection, and budget-threshold warnings.
- **Categories** - user-defined and shared system-default categories.
- **Budgets** - monthly per-category caps with spend/remaining/over-budget summaries.
- **Recurring transactions** - scheduled (once/daily/weekly/monthly) transactions processed by a Quartz job.
- **Analytics** - spending-by-category, period summaries, and income/expense trends, Redis-cached.
- **Notifications** - Kafka-driven, persisted, and pushed live via Server-Sent Events (SSE).

## Getting started

### Prerequisites

- JDK 21
- Docker (for Postgres, Kafka, and Redis via `docker-compose`)

### 1. Configure environment

Copy the template and fill in your own values:

```
cp .env.example .env
```

See `.env.example` for the full list of variables (database credentials, JWT secret/expiration, and optional overrides for CORS, the recurring-transaction cron schedule, and Redis host/port). `KAFKA_BOOTSTRAP_SERVERS` must also be set in your environment or `.env` - the app has no default for it.

### 2. Start infrastructure

```
docker-compose up -d
```

This starts Postgres (`5432`), Kafka in KRaft mode (`9092`, with Kafka-UI at `http://localhost:4040`), and Redis (`6379`).

### 3. Run the app

```
./mvnw spring-boot:run
```

The app runs under the `local` profile by default and loads `.env` automatically.

### API docs

- Interactive docs (Scalar): `http://localhost:8080/api/docs`
- OpenAPI spec: `http://localhost:8080/api/openapi.json`

All endpoints are served under the `/api` context path.

## Testing

```
./mvnw test
```

Run a single test class:

```
./mvnw test -Dtest=TransactionServiceImplTest
```

Run a single test method:

```
./mvnw test -Dtest=TransactionServiceImplTest#createTransaction_rejectsOverdraft
```

Integration tests spin up **Testcontainers** (Postgres) against `application-test.yml`; test data is built with **Instancio** and **Datafaker**.

## Project structure

The codebase is feature-sliced under `com.seap.smartfinancetracker`, with each module (`auth`, `security`, `user`, `category`, `transaction`, `budget`, `notification`, `analytics`, `common`) following the same internal layout: `controller / service (+Impl) / repository / entity / dto / mapper / exception (ErrorCode) / constant`.

Database schema changes are managed exclusively through Flyway migrations in `src/main/resources/db/migration/` - Hibernate's `ddl-auto` is set to `validate`.
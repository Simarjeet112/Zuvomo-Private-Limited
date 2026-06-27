# Trading Signal Tracking Application

A backend service for creating and tracking trading signals against live Binance prices. Built with Java 17, Spring Boot 3, Spring Data JPA, and PostgreSQL.

## Tech Stack

- **Java 17**, **Spring Boot 3.3.4**
- **Spring MVC** (REST API), **Spring Data JPA / Hibernate** (persistence)
- **PostgreSQL** (database)
- **Maven** (build tool)
- **JUnit 5 + Mockito + AssertJ** (testing)
- **springdoc-openapi** (Swagger UI / API docs)
- **Lombok** (boilerplate reduction)
- **Binance Public REST API** (live price data — no API key required)

## Prerequisites

- JDK 17 (the project will not compile correctly on newer JDKs due to a Lombok/javac compatibility issue — see Troubleshooting below)
- Maven 3.9+
- PostgreSQL running locally (or accessible via network)

## Setup Instructions

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd tracker
```

### 2. Database setup

Create the database (via `psql` or any Postgres client):

```sql
CREATE DATABASE trading_signals;
```

The application uses Hibernate's `ddl-auto: update` setting, so the `signals` table and all columns/constraints are created automatically on first run — no manual schema setup needed.

### 3. Configure database credentials

Edit `src/main/resources/application.yml` and set your local Postgres credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_signals
    username: <your-postgres-username>
    password: <your-postgres-password>
```

### 4. Build the project

```bash
mvn clean install
```

This compiles the code, runs all unit tests, and packages the application.

### 5. Run the application

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

## API Documentation

Once running, interactive API documentation is available via Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI spec (JSON):

```
http://localhost:8080/api-docs
```

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/signals` | Create a new trading signal |
| `GET` | `/api/signals` | Get all signals, with live status evaluation against current Binance price |
| `GET` | `/api/signals/{id}` | Get a single signal by id, with live status evaluation |
| `DELETE` | `/api/signals/{id}` | Delete a signal |

### Example request — create a signal

```http
POST /api/signals
Content-Type: application/json

{
  "symbol": "BTCUSDT",
  "direction": "BUY",
  "entryPrice": 50000,
  "stopLoss": 48000,
  "targetPrice": 55000,
  "entryTime": "2026-06-27T16:00:00Z",
  "expiryTime": "2026-06-28T16:00:00Z"
}
```

## Running Tests

```bash
mvn test
```

This runs the full test suite (26 tests), covering:
- BUY/SELL validation rules (cross-field constraint validation)
- Time validation (24-hour historical window, future-entry rejection, expiry-after-entry)
- Status evaluation logic (TARGET_HIT, STOPLOSS_HIT, EXPIRED, terminal-state immutability)
- ROI calculation (BUY and SELL formulas)
- Spring application context loading (full integration smoke test)

## Troubleshooting

**Build fails with `ExceptionInInitializerError: TypeTag :: UNKNOWN`**
This means Maven is compiling with a JDK newer than 17 (commonly JDK 24/25 installed alongside 17). Lombok's annotation processor is not yet stable on these versions. Fix by pointing `JAVA_HOME` at JDK 17:

```bash
export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version   # should print 17.x.x
```

**`FATAL: database "trading_signals" does not exist`**
The database hasn't been created yet — run the `CREATE DATABASE` command in step 2 above.

**Lombok getters/setters not found during compilation**
Confirm `pom.xml` has an explicit `maven-compiler-plugin` configuration with `annotationProcessorPaths` pointing at the Lombok dependency (see `pom.xml` in this repo for the working configuration).
# Architecture Overview

## Project Structure

The application follows a layered architecture with clear separation of concerns:

```
com.tradingsignal.tracker
├── controller/      → REST endpoints, request/response mapping only
├── service/         → Business logic (orchestration, evaluation, external API calls)
├── repository/      → Spring Data JPA interfaces (no implementation needed)
├── entity/          → JPA-mapped domain objects + enums
├── dto/             → Request/response shapes, decoupled from entities
├── validation/      → Custom cross-field validation (BUY/SELL rules, time rules)
├── exception/       → Custom exceptions + centralized exception handling
└── config/          → Bean configuration (e.g. RestClient for Binance)
```

Each layer has exactly one responsibility, and dependencies flow in one direction: `controller → service → repository`. Controllers contain no business logic — they validate input shape (`@Valid`) and delegate everything else.

## Business Logic Flow

### Signal creation
1. `POST /api/signals` request hits `SignalController.createSignal()`.
2. `@Valid` triggers two layers of validation before the method body runs:
   - **Field-level** (`@NotNull`, `@Positive`) on individual fields in `CreateSignalRequest`.
   - **Cross-field** (`@ValidSignal` → `ValidSignalValidator`) — checks BUY/SELL price relationships (e.g. for BUY, `stopLoss < entryPrice < targetPrice`) and time rules (expiry after entry, entry within the last 24 hours, entry not in the future). These checks run independently so a single request surfaces *all* applicable errors, not just the first one found.
3. `SignalController` delegates to `SignalService.createSignal()`, which builds a `Signal` entity (status defaults to `OPEN`), saves it via `SignalRepository`, and returns a mapped `SignalResponse`.

### Status evaluation (the core state machine)
There is no scheduler in this implementation. Instead, status is evaluated **lazily, on every read** (`GET /signals` and `GET /signals/{id}`):

1. `SignalService.evaluateAndMaybePersist()` checks if the signal's current status is already terminal (`TARGET_HIT`, `STOPLOSS_HIT`, `EXPIRED`). If so, it returns immediately — no Binance call is made, since a final-state signal can never change again.
2. If still `OPEN`, it fetches the live price via `BinancePriceService`, then passes the signal + price to `SignalEvaluationService.evaluateStatus()`.
3. `evaluateStatus()` checks price-based conditions (target/stop-loss hit) **before** checking expiry — this matches the spec's rule that expiry only applies "if no target or stop loss has been hit." Order of these checks is deliberate, not incidental.
4. If the status changed, the new status (and, if now terminal, the calculated ROI) is persisted immediately, so the database is never left in a stale state between reads.

**Design tradeoff:** this read-driven evaluation model means a signal that is never read will never auto-transition on its own. The spec lists `@Scheduled` as an optional bonus for proactive (push-based) evaluation; given the 24-hour assignment window, the read-driven model was chosen as the simpler approach that still fully satisfies "automatically evaluate signal status" for every signal a client actually queries.

### Terminal state immutability
`SignalStatus.isFinal()` is the single source of truth for "can this signal still change." Every place that needs to ask this question calls the same method, rather than re-implementing the same `status == X || status == Y` check in multiple places. This was verified in production by querying the same `TARGET_HIT` signal twice, several minutes apart — the response was byte-for-byte identical (status and ROI) despite the live BTC price having moved in between.

## External API Integration

`BinancePriceService` wraps all communication with Binance's public REST API (`GET /api/v3/ticker/price?symbol={symbol}`, no API key required) behind a single method: `getCurrentPrice(symbol)`. It uses Spring's `RestClient`, configured via a dedicated `@Configuration` bean (`RestClientConfig`) with the base URL injected from `application.yml` rather than hardcoded.

Failures (network errors, malformed responses, missing price data) are wrapped in a custom `BinancePriceException` and surfaced to the client as `502 Bad Gateway` — distinct from `500 Internal Server Error` — to correctly signal "an upstream dependency failed" rather than "this server has a bug."

This isolation (price-fetching logic in one class, called only from `SignalService`) is what made unit testing the evaluation logic possible without mocking HTTP at all: `SignalEvaluationService` takes a price as a plain `BigDecimal` parameter, so tests simply pass in whatever price they want to simulate.

## Data Types

- **`BigDecimal`** for all prices and ROI — not `double`/`float` — since floating-point binary representation cannot exactly represent most decimal fractions, which would silently corrupt financial calculations over time.
- **`Instant`** for all timestamps — not `LocalDateTime` — since `Instant` is always UTC and unambiguous, avoiding timezone-related bugs in expiry/entry-time comparisons.

## Error Handling

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) centralizes all error responses into a consistent JSON shape (`ErrorResponse`), mapping:
- Validation failures → `400 Bad Request` (with per-field error messages)
- `SignalNotFoundException` → `404 Not Found`
- `BinancePriceException` → `502 Bad Gateway`
- Any unhandled exception → `500 Internal Server Error` (generic message, no stack trace leaked to the client)
# Real-Time Auction Platform

An enterprise-grade, production-style backend for a real-time online auction platform — built end-to-end in Java 21 / Spring Boot 3, covering everything from JWT authentication to concurrency-safe bidding, proxy auto-bidding, WebSocket live updates, Redis caching, and a fully containerized deployment.

This isn't a CRUD tutorial project. It was built in 16 sequential phases, each one adding a real production concern — race conditions, cache invalidation, horizontal scalability, security defense-in-depth — with the reasoning behind every non-obvious decision documented as it was made.

---

## Table of Contents

- [Overview](#overview)
- [Key Engineering Highlights](#key-engineering-highlights)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Feature Modules](#feature-modules)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Design Decisions Worth Discussing](#design-decisions-worth-discussing)
- [Roadmap / Not Yet Implemented](#roadmap--not-yet-implemented)

---

## Overview

Buyers and sellers register, sellers list items for auction, buyers bid manually or set a maximum for automatic proxy bidding (eBay-style), auctions transition through a scheduler-driven lifecycle with anti-sniping protection, winners pay through a gateway-agnostic payment module, and everyone gets real-time updates over WebSocket without polling.

Three roles: **Buyer**, **Seller**, **Admin** — with a full admin panel for user/auction moderation and platform statistics.

## Key Engineering Highlights

These are the parts of the system most worth discussing in a technical interview:

- **Concurrency-safe bidding** — pessimistic row-locking (`SELECT ... FOR UPDATE`) prevents lost updates when multiple bids race on the same auction simultaneously. Verified with a real multi-threaded integration test, not mocked.
- **Proxy auto-bidding algorithm** — a genuine eBay-style resolution loop: the system bids only as much as needed to beat the current competitor, never revealing a bidder's true maximum until forced to.
- **Race-condition-free anti-sniping** — a bid extending an auction's end time and the scheduler ending that same auction share one lock, with a re-check after acquiring it, closing a classic TOCTOU (time-of-check-to-time-of-use) bug.
- **Defense-in-depth privacy boundaries** — draft auctions return `404` to non-owners (existence hidden), published auctions a non-owner can't edit return `403` (existence is public, permission isn't) — a deliberate, consistent pattern applied everywhere from auth to search filters.
- **Horizontally scalable real-time updates** — WebSocket broadcasts go through Redis Pub/Sub, not directly to the local STOMP broker, so live bid updates reach every connected client regardless of which app instance they're connected to.
- **Reserve price never leaks** — one shared response mapper, one conditional field, enforced at the data layer (including in the Redis cache) rather than scattered across DTOs.
- **Gateway-agnostic payments** — a `PaymentGatewayService` interface with a mock implementation standing in for Razorpay/Stripe; swapping to a real gateway touches one class, nothing else.
- **Fail-soft infrastructure** — Redis caching and rate limiting both degrade gracefully (cache miss / rate-limit-open) if Redis is ever unreachable, rather than taking the whole app down.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21, Spring Boot 3.3 |
| Security | Spring Security, JWT (JJWT), BCrypt |
| Persistence | Spring Data JPA (Hibernate), MySQL 8 |
| Caching / Rate Limiting | Redis 7 (Lettuce), Pub/Sub |
| Real-Time | WebSocket (STOMP over SockJS) |
| Object Mapping | MapStruct, Lombok |
| API Docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, AssertJ, H2 |
| Build / Deploy | Maven, Docker, Docker Compose (multi-stage build) |

## Architecture

Standard layered architecture, strictly enforced:

```
Controller → Service → Repository → Database
```

- Controllers never see entities — only DTOs, mapped via MapStruct or purpose-built mappers.
- A consistent `ApiResponse<T>` envelope wraps every response.
- A centralized `GlobalExceptionHandler` converts every domain exception into a consistent error shape.
- Bean Validation (`@Valid`) on every request DTO.
- Method-level (`@PreAuthorize`) and URL-level (`SecurityConfig`) authorization, each used where it fits best.

## Feature Modules

| Module | Capabilities |
|---|---|
| **Auth** | Register, login, JWT access + refresh tokens (rotation + reuse-detection), email verification, forgot/reset password |
| **Profile** | Update profile, multiple addresses with default-address logic, change password, profile image upload |
| **Auctions** | Draft → Scheduled → Live → Ended → Cancelled state machine, categories, multi-image upload |
| **Bidding** | Manual bids, proxy auto-bidding, full bid history, concurrency-safe under load |
| **Real-Time** | WebSocket live bid updates, no polling, horizontally scalable via Redis Pub/Sub |
| **Scheduler** | Automatic auction start/end, anti-sniping extension (race-condition-safe) |
| **Watchlist & Notifications** | Track auctions, email + WebSocket notifications for outbid/won/started/ending-soon events |
| **Payments** | Gateway-agnostic order creation, signature-verified webhook confirmation, refunds, idempotent handling |
| **Search** | Keyword, category, price range, seller, and status filters — Specification-pattern based, combinable |
| **Admin** | User ban/unban, auction removal (soft-cancel, history preserved), platform-wide statistics |
| **Caching & Rate Limiting** | Redis-cached auction views and auth lookups, login/bid rate limiting, all fail-soft |

## Getting Started

### Prerequisites

- Docker Desktop (recommended — runs the entire stack with one command)
- *or*, for local development without Docker: Java 21, Maven, a running MySQL 8 instance, a running Redis 7 instance

### Run with Docker (recommended)

```bash
docker compose up --build
```

This builds the app image and starts three containers: `auction-app`, `auction-mysql`, `auction-redis`. The app waits for MySQL and Redis to pass their health checks before starting.

On first run against a fresh database, load the schema:

```bash
docker exec -i auction-mysql mysql -uroot -proot auction_platform < src/main/resources/schema.sql
```

The app is then available at `http://localhost:8080`.

### Run locally (IDE)

1. Start MySQL and Redis only: `docker compose up mysql redis`
2. Run `AuctionPlatformApplication` from your IDE, or `mvn spring-boot:run`
3. Roles are seeded automatically on startup (`DataSeeder`)

### Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | dev placeholder | HMAC signing key for JWTs — **must** be overridden in production |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | MySQL credentials |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `FRONTEND_BASE_URL` | `http://localhost:3000` | Used to build verification/reset links in emails |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | unset | If unset, email sending fails soft and the link is logged instead |
| `LOCAL_STORAGE_PATH` | `./uploads` | Where profile/auction images are stored |

## API Documentation

Once running, full interactive API docs are at:

```
http://localhost:8080/swagger-ui.html
```

Every endpoint is documented with request/response shapes, grouped by module (Authentication, Profile, Auctions, Bids, Auto-Bid, Payments, Admin, etc.).

## Testing

```bash
mvn test
```

Includes:
- **Integration tests** (H2) covering the full auth lifecycle — register, verify, login, refresh-token rotation, password reset.
- **Unit tests** (Mockito) for the auto-bid resolution algorithm, auction state-machine rules, and default-address logic.
- **A concurrency integration test** that spins up 10 threads bidding the same amount on the same auction simultaneously, and asserts exactly one succeeds — direct proof the pessimistic-locking design prevents lost updates.

## Project Structure

```
src/main/java/com/auction/platform/
├── config/          # Security, WebSocket, Redis, Scheduler configuration
├── controller/      # REST controllers — DTOs only, no entities
├── dto/             # Request/response DTOs
├── entity/          # JPA entities + enums
├── exception/       # Domain exceptions + global handler
├── mapper/          # MapStruct + manual mappers
├── repository/      # Spring Data JPA repositories
├── security/        # JWT, STOMP auth interceptor, UserDetails
└── service/         # Business logic, one interface + impl per concern
```

## Design Decisions Worth Discussing

A few decisions that came up repeatedly in code review and are good interview talking points:

1. **Pessimistic locking over optimistic locking for bidding.** Optimistic locking (retry-on-conflict) causes thrashing under the exact high-contention scenario auctions create (the final seconds). Pessimistic locking serializes instead — no wasted retries, no failed requests, just a short queue.
2. **BigDecimal for all monetary fields.** `double`/`float` cannot represent currency exactly; every price, bid, and payment amount uses `BigDecimal`.
3. **SHA-256 for token hashing, BCrypt for passwords.** Refresh/verification tokens are already high-entropy random values — brute-forcing them is infeasible regardless of hash speed, so a fast hash is correct. Passwords are low-entropy by comparison and need BCrypt's deliberate slowness.
4. **Webhook signature verification on raw bytes, not the parsed object.** JSON re-serialization isn't guaranteed byte-identical to the original payload; verifying against raw request bytes avoids false signature mismatches.
5. **Reserve price is never serialized to a public response, including in the cache.** One mapper, one conditional field — not duplicated logic scattered across DTOs that could drift out of sync.

## Roadmap / Not Yet Implemented

Deliberately deferred, consistent with the original project scope:

- Kafka (event-driven architecture)
- Elasticsearch (would replace the current `LIKE` keyword search, which doesn't scale past a moderate dataset)
- AWS S3 for image storage (currently local disk, behind a swappable `ProfileImageStorageService` interface for exactly this reason)
- GitHub Actions CI/CD
- Kubernetes deployment manifests
- Prometheus & Grafana monitoring
- Testcontainers-based concurrency test against real MySQL (current test uses H2, which is not byte-for-byte identical to InnoDB locking under extreme concurrency)

---

Built as a systematic, phase-by-phase deep dive into production backend engineering — every design decision documented at the point it was made, not retrofitted afterward.

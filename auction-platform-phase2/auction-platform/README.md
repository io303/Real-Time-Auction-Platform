# Real-Time Auction Platform — Phase 1 + Phase 2

Enterprise-style Spring Boot backend for a real-time auction platform. This delivery covers:

- **Phase 1**: Register, Login, JWT issuing, RBAC scaffolding (Buyer / Seller / Admin roles).
- **Phase 2**: Refresh tokens (with rotation + reuse detection), email verification (gates login), forgot/reset password.

**Note on Phase 2 breaking change:** registration no longer returns an access token immediately — it now requires
email verification first. Login is rejected with `403` until the account's email is verified. See the design
rationale in the accompanying conversation.

## Stack

Java 21 · Spring Boot 3.3 · Spring Security · Spring Data JPA · MySQL · JWT (JJWT) · Spring Mail · Lombok · MapStruct · Swagger/OpenAPI · JUnit 5 · H2 (tests)

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for MySQL/Redis)

## Running locally

1. Start infrastructure:

   ```bash
   docker compose up -d
   ```

   This brings up MySQL on `3306` and Redis on `6379` (Redis isn't used yet in Phase 1, but is included since later phases need it).

2. Run the app:

   ```bash
   mvn spring-boot:run
   ```

   On startup, `DataSeeder` automatically inserts `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN` into the `roles` table if they don't already exist.

3. The app is available at `http://localhost:8080`.
   Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running tests

Tests run against an in-memory H2 database (see `src/test/resources/application-test.yml`) — no Docker required.

```bash
mvn test
```

Covers: successful registration, duplicate-email conflict, weak-password validation rejection, successful login, and wrong-password handling (verifying no field-specific leak on bad credentials).

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | No | Register new user (`ROLE_BUYER`), sends verification email, no token issued yet |
| GET | `/api/v1/auth/verify-email?token=` | No | Verify email using the token from the email (or app logs in dev) |
| POST | `/api/v1/auth/login` | No | Authenticate — requires verified email — returns access + refresh tokens |
| POST | `/api/v1/auth/refresh-token` | No | Exchange a valid refresh token for a new access + refresh token pair (rotates) |
| POST | `/api/v1/auth/logout` | No | Revoke a specific refresh token |
| POST | `/api/v1/auth/forgot-password` | No | Request a password reset email (always generic response) |
| POST | `/api/v1/auth/reset-password` | No | Complete password reset using the emailed token; revokes all sessions |

### Example — Register → Verify → Login

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Aditi Sharma",
    "email": "aditi@example.com",
    "password": "SecureP@ss1",
    "phoneNumber": "+919812345678"
  }'

# 2. No SMTP configured locally? Check application logs for a line like:
#    "Verification link for aditi@example.com (dev visibility): http://localhost:3000/verify-email?token=<TOKEN>"
#    Copy <TOKEN> from there.

# 3. Verify
curl "http://localhost:8080/api/v1/auth/verify-email?token=<TOKEN>"

# 4. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "aditi@example.com", "password": "SecureP@ss1" }'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` on protected endpoints, and the `refreshToken`
against `/api/v1/auth/refresh-token` once the access token expires. **Refresh tokens rotate on every use** —
reusing an already-rotated token revokes all of that user's sessions (theft-detection behavior).

## Environment variables (override defaults in `application.yml`)

| Variable | Default | Purpose |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | (dev placeholder — **change in production**) | HMAC signing key for JWTs |
| `FRONTEND_BASE_URL` | `http://localhost:3000` | Base URL used to build verification/reset links in emails |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | `localhost` / `587` / empty / empty | Mail server config. If unset, email sending fails soft and the link is logged instead (see `EmailServiceImpl`). |

## What's next

Phase 3 adds User Profile and Address Management. See project roadmap in the accompanying conversation/design doc.

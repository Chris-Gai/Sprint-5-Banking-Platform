# Sprint 5 — Docker + Microservice Split

Two Spring Boot 3.4 / Java 21 services, split out of the Sprint 3/4 banking monolith:

- **auth-service** (port 8081) — owns the `users` table, issues RS256 JWTs, publishes the
  public key at `/.well-known/jwks.json`.
- **banking-service** (port 8080) — owns `accounts`, `bank_transactions`, `idempotency_records`.
  Validates JWTs against auth-service's JWKS endpoint via Spring Security's OAuth2 Resource
  Server — it never sees the private key.

Each service has its own MySQL database (`auth-db`, `banking-db`) and its own Flyway
migrations. There is no foreign key from `accounts.owner_id` to `users.id` — that's not
possible across two databases. The trust boundary is the JWT signature.

## One note on the Dockerfiles

The sprint spec's Dockerfile uses `./mvnw` (the Maven wrapper). These Dockerfiles use the
official `maven:3.9.9-eclipse-temurin-21` image as the build stage instead — same multi-stage,
same layered-jar runtime image, one less set of wrapper files to keep in sync. If your
bootcamp grader specifically wants `mvnw`, run `mvn -N io.takari:maven:wrapper -Dmaven=3.9.9`
inside each service folder once and swap the build stage back to `COPY .mvn/ mvnw ...` /
`RUN ./mvnw ...`.

## Running it

```bash
docker compose up --build
```

Wait for all four containers to report healthy (`docker compose ps`).

## Smoke test

```bash
# 1. Register against auth-service
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"matthew","email":"m@x.com","password":"supersecret123"}'
# -> { "token": "eyJhbGciOiJSUzI1NiI...", ... }

# 2. Confirm JWKS
curl http://localhost:8081/.well-known/jwks.json

TOKEN="paste-the-token-here"

# 3. List accounts (empty)
curl http://localhost:8080/api/v1/accounts -H "Authorization: Bearer $TOKEN"

# 4. Open an account idempotently
curl -X POST http://localhost:8080/api/v2/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"initialDeposit": 100.00}'

# 5. Tamper with the token -> 401
curl http://localhost:8080/api/v1/accounts -H "Authorization: Bearer ${TOKEN}X"
```

Adminer is at http://localhost:8090. Connect to `auth-db` (user `authuser` / pass `authpass`)
and `banking-db` (user `bankuser` / pass `bankpass`) separately to confirm the schema split —
no `users` table in `banking-db`.

## What's where

```
auth-service/
  src/main/java/com/example/authservice/
    entity/        User, Role
    repository/     UserRepository
    security/       JwtService (RS256 sign), CustomUserDetailsService, SecurityConfig
    service/        AuthService (register/login)
    controller/     AuthController, JwksController
    exception/      GlobalExceptionHandler, ErrorResponse, DuplicateUserException
  src/main/resources/
    db/migration/   V1__create_users_table.sql
    keys/           dev RSA keypair (gitignored - never commit private_key.pem)

banking-service/
  src/main/java/com/example/bankapi/
    entity/         Account, BankTransaction, IdempotencyRecord
    repository/      AccountRepository, BankTransactionRepository, IdempotencyRepository
    security/        SecurityConfig (resource server), JwtUserExtractor
    service/         AccountService, TransferService, IdempotencyService
    controller/v1/   deprecated endpoints (Deprecation header via interceptor)
    controller/v2/   current endpoints, idempotent via Idempotency-Key header
    interceptor/     DeprecationInterceptor
    exception/       GlobalExceptionHandler, ErrorResponse, custom exceptions
  src/main/resources/
    db/migration/    V1 accounts, V2 transactions, V3 idempotency_records
  src/test/.../CrossServiceJwtIntegrationTest.java
    Testcontainers test: spins up a real auth-service + MySQL, gets a real JWT,
    proves banking-service validates it via JWKS over the network (and rejects tampering).
```

## Things to know before you submit

- **Never edit an applied Flyway migration.** New change = new `V{n}__` file.
- **`owner_id` on `Account` is a plain `Long`, no `@ManyToOne`.** The `User` entity doesn't
  exist in this service.
- **JWKS endpoint is intentionally public.** It only contains the public key.
- The `kid` in `application.properties` (`app.jwt.key-id=auth-key-1`) must match between
  what auth-service signs with and what it publishes — `JwksController` reads it from the
  same `JwtService`, so they can't drift.
- v1 endpoints get `Deprecation`, `Sunset`, and `Link` response headers via
  `DeprecationInterceptor`. v2 endpoints require an `Idempotency-Key` header; reusing a key
  with a different body returns `409`.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

Multi-module Maven project. Maven CLI is **not** on PATH on the dev machine — build via Docker:

```bash
# Build a single service image (recommended — only the changed module recompiles)
docker compose build <service-name>

# Build all services
docker compose build

# Start the full stack
docker compose up -d

# Start a subset (services declare depends_on so deps come up automatically)
docker compose up -d ai-service              # also brings discovery-service, postgres-ai, otel-collector
docker compose up -d transaction-service     # also brings kafka, postgres-transaction, etc.

# Logs / health
docker logs changee-finance-<service>-1
curl http://localhost:<port>/actuator/health
```

Each service Dockerfile uses a multi-stage Maven build that runs `mvn -pl <service> -am package` inside the container — so `mvn` on the host is not required. To run a single Maven goal inside the builder image without a full rebuild:

```bash
docker run --rm -v "$PWD":/w -w /w maven:3.9-eclipse-temurin-21 \
  mvn -pl <service> -am test
```

There are no test commands wired into CI; this is a learning project. Run module tests with the command above.

## Multi-module Dockerfile gotcha

When adding a new module to `pom.xml`, every other service's Dockerfile must also `COPY <new-module>/pom.xml <new-module>/` in the builder stage — otherwise the parent POM fails to resolve the modules list and **all** builds break, not just the new service. The pattern is: each Dockerfile copies *every* module pom (so the reactor resolves), but only copies *its own* `src/` and runs `mvn -pl <self> -am package`.

## High-level architecture

Eight Spring Boot 3.5.3 services on Java 21, registered with Eureka, fronted by Spring Cloud Gateway. Inter-service calls go through `lb://<service-id>` resolved via Eureka.

```
client → api-gateway :8080 (JWT validate, rate limit, X-User-* injection)
                ↓ lb://
   auth :8081  user :8082  account :8083  transaction :8084  notification :8085  ai :8086
                                              │
                                              └─ Kafka (transaction.{created,completed,failed}) → notification
```

Each service owns a private Postgres DB (`postgres-<service>`). `discovery-service` runs Eureka on `:8761`. `redis` backs the gateway rate limiter. `otel-collector` receives OTLP from every service.

### Cross-cutting conventions

- **Package layout per service**: `org.dinhb.microservice.core.<service>` with sub-packages `domain/` (JPA + repositories), `service/` (business logic), `web/` (controllers, request/response records), `config/` (beans).
- **Identity propagation**: `api-gateway` validates the JWT and injects three headers downstream — `X-User-Id`, `X-Username`, `X-User-Roles` (constants in `common-lib` → `HeaderNames`). Controllers consume them via `@RequestHeader(HeaderNames.X_USER_ID) UUID userId`. **Never trust user identity from a request body** — always read from the header.
- **Public vs internal endpoints**: gateway forwards `/api/<service>/**` with `StripPrefix=2` to `lb://<service>`. Services also expose ports directly (e.g. `:8083`) for internal/admin calls that bypass JWT — used by tools like AI's `AccountInfoTool` and the smoke tests.
- **Auth public-paths**: see `api-gateway/src/main/resources/application.yml` → `gateway.jwt.public-paths`. Add new public routes there.
- **`common-lib`**: shared `JwtUtils`, `HeaderNames`, Kafka event records (`TransactionEvent`, `KafkaTopics`), `ApiError` DTO. Don't duplicate these per service.
- **Resilience**: `transaction-service` calls `account-service` via OpenFeign with a Resilience4j `CircuitBreaker` (instance name `accountService`). Per-controller rate limiting uses `@RateLimiter(name = "...")`. Edge rate limit is global on the gateway.
- **OpenTelemetry**: every service Dockerfile downloads `opentelemetry-javaagent.jar` and sets `JAVA_TOOL_OPTIONS=-javaagent:/app/opentelemetry-javaagent.jar`. Auto-instruments Spring MVC, JDBC, Kafka, Feign. Compose injects `OTEL_SERVICE_NAME` and OTLP endpoint env vars (anchor `*otel-env` in `docker-compose.yml`).
- **Eureka env**: anchor `*eureka-env` in `docker-compose.yml` provides `EUREKA_DEFAULT_ZONE` to every microservice.

### `ai-service` specifics

Uses Spring AI 1.0.3 with the **OpenAI starter** (not `spring-ai-azure-openai`) pointed at Azure AI Foundry's OpenAI-compatible endpoint. Three rules to remember:

1. **Base URL must end at `/openai`** (no trailing path). Spring AI's OpenAI client appends `/v1/chat/completions`. Foundry endpoints ending in `/v1/responses` (Responses API) or `/models` will not work — Foundry's Chat Completions path is `/openai/v1/chat/completions`.
2. **No `@LoadBalanced RestClient.Builder` bean**. The `@LoadBalanced RestClientCustomizer` registered by Spring Cloud LoadBalancer leaks onto Spring AI's default `RestClient.Builder` and causes Spring AI to treat the Foundry hostname as a Eureka service ID (`No servers available for service: ...services.ai.azure.com`). For tools that need to call other microservices, inject `LoadBalancerClient` and resolve URIs manually — see `AccountInfoTool` for the pattern.
3. **Tool identity via `ToolContext`**, not tool params. The AI must not be allowed to forge a `userId`. `AiChatService` puts the caller's `userId` (from the `X-User-Id` header) into `.toolContext(Map.of(USER_ID_KEY, ...))`, and tool methods read it from `ToolContext`. Never expose `userId` as a `@ToolParam`.

Chat memory is persisted in `postgres-ai` via `JdbcChatMemoryRepository` (auto-init schema). The conversation ID is the user's UUID, so memory is partitioned per user.

## Environment

All secrets/config flow through `.env` at the repo root (compose reads it automatically). `.env.example` lists required keys. Required for `ai-service`:

```
AZURE_FOUNDRY_BASE_URL=https://<resource>.services.ai.azure.com/.../openai
AZURE_FOUNDRY_API_KEY=<key>
AI_MODEL=<deployment-name>
```

`docker-compose.yml` interpolates `${VAR}` from `.env` and injects them into containers. If a var is unset on the host, compose injects an **empty string**, which overrides Spring's `${VAR:default}` fallback in `application.yml`. Always set the var in `.env` rather than relying on YAML defaults.

## Smoke test path

The README's end-to-end flow (signup → create account → credit → transfer → notification) exercises every service via the gateway and is the fastest way to verify the whole stack after a change. For ai-service, hit `:8086/chat` directly with an `X-User-Id` header (gateway adds it from JWT in production).

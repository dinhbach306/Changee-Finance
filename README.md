# Changee Finance — Microservices Learning Project

Mini finance domain với 7 services, Kafka, PostgreSQL, OTEL → Datadog, đóng gói Docker.

## Kiến trúc

```
                 ┌──────────────────┐
   client ─────► │  api-gateway     │ :8080  (JWT, rate limit, lb://)
                 └────────┬─────────┘
                          │ via lb://  (Eureka discovery)
       ┌─────────┬────────┼────────┬───────────┐
       ▼         ▼        ▼        ▼           ▼
   auth-svc  user-svc  acct-svc  txn-svc    notif-svc
   :8081     :8082     :8083     :8084      :8085
       │         │        │         │           ▲
       ▼         ▼        ▼         ▼           │
   pg-auth   pg-user  pg-acct  pg-txn       Kafka topics
                                  │       transaction.created
                                  └─────► transaction.completed
                                          transaction.failed

   discovery-service (Eureka) :8761
   redis (gateway rate limiter)
   datadog-agent (OTLP :4317/:4318) ◄── all services export traces/metrics/logs
```

| Service | Port | Trách nhiệm |
|---|---|---|
| `discovery-service` | 8761 | Eureka registry |
| `api-gateway` | 8080 | JWT auth filter, Redis rate limiter, route `lb://` |
| `auth-service` | 8081 | Signup / login, phát JWT |
| `user-service` | 8082 | Customer profile + KYC |
| `account-service` | 8083 | Bank accounts CRUD, debit/credit |
| `transaction-service` | 8084 | Transfer (CB + retry sang account-service) → Kafka publish |
| `notification-service` | 8085 | Consume Kafka events → ghi log notification |

## Cross-cutting

- **Service Discovery**: Netflix Eureka (`discovery-service`)
- **Authentication**: JWT (HS256) phát ở `auth-service`, validate ở `api-gateway` (filter `JwtAuthenticationFilter`). Gateway forward `X-User-Id`, `X-Username`, `X-User-Roles` xuống downstream.
- **Rate Limiting**:
  - Edge: Spring Cloud Gateway `RequestRateLimiter` (Redis token bucket) — global default cho mọi route
  - Per-service: Resilience4j `@RateLimiter` ở các controller
- **Circuit Breaker**: Resilience4j ở `transaction-service` khi gọi `account-service` qua OpenFeign (`accountService` instance)
- **Message Broker**: Kafka — topics `transaction.created`, `transaction.completed`, `transaction.failed`
- **Database**: PostgreSQL, mỗi service một DB riêng (DB-per-service)
- **Observability (OTEL → Datadog)**:
  - Mỗi service Dockerfile mount `opentelemetry-javaagent.jar` qua `JAVA_TOOL_OPTIONS=-javaagent:...`
  - Agent auto-instrument Spring MVC/WebFlux, JDBC, Kafka producer/consumer, Feign
  - Export OTLP/gRPC tới `datadog-agent:4317`
  - Datadog Agent bật `otlp_config` → forward traces (APM), metrics, logs lên Datadog backend

## Chạy local

### 1. Tạo `.env`

```bash
cp .env.example .env
# Mở .env và điền:
#   DD_API_KEY = API key của bạn (https://app.datadoghq.com/organization-settings/api-keys)
#   DD_SITE    = vùng Datadog account của bạn (vd datadoghq.com / datadoghq.eu / ...)
#   JWT_SECRET = chuỗi bí mật ≥ 32 ký tự
```

### 2. Build & start

```bash
docker compose build
docker compose up -d
```

Lần build đầu tiên sẽ lâu vì mỗi Dockerfile build module Spring Boot riêng (Maven cache layer giúp các build sau nhanh hơn).

### 3. Kiểm tra services up

```bash
# Eureka registry UI
open http://localhost:8761

# Health check
curl http://localhost:8080/actuator/health

# Datadog Agent
docker compose exec datadog-agent agent status
```

### 4. Smoke test end-to-end

```bash
# Signup
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | tee /tmp/alice.json

TOKEN=$(jq -r .accessToken /tmp/alice.json)

# Tạo 2 accounts
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"CHECKING"}' | tee /tmp/acc1.json

curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"SAVINGS"}' | tee /tmp/acc2.json

ACC1=$(jq -r .id /tmp/acc1.json)
ACC2=$(jq -r .id /tmp/acc2.json)

# Nạp tiền vào ACC1 (gọi trực tiếp account-service vì là ops nội bộ)
curl -s -X POST http://localhost:8083/$ACC1/credit \
  -H "Content-Type: application/json" \
  -d '{"amount":1000.00}'

# Transfer ACC1 -> ACC2
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":\"$ACC1\",\"toAccountId\":\"$ACC2\",\"amount\":250.00}"

# Kiểm tra notifications (consumed từ Kafka)
curl -s http://localhost:8080/api/.../me   # (hoặc gọi trực tiếp http://localhost:8085/me + header X-User-Id)
```

### 5. Quan sát Datadog

- **APM**: app.datadoghq.com → APM → Services → tìm `api-gateway`, `transaction-service`, ... Thấy distributed trace từ gateway → transaction → account → kafka.
- **Metrics**: app.datadoghq.com → Metrics Explorer → search `otel.*` hoặc `system.*`
- **Logs**: app.datadoghq.com → Logs → search `service:transaction-service`

### 6. Test Circuit Breaker

```bash
# Stop account-service để mô phỏng downstream failure
docker compose stop account-service

# Gọi transfer nhiều lần — sau ~10 lần fail, CB chuyển sang OPEN
for i in {1..15}; do
  curl -s -X POST http://localhost:8080/api/transactions/transfer \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"fromAccountId\":\"$ACC1\",\"toAccountId\":\"$ACC2\",\"amount\":1.00}"
  echo
done

# Kiểm tra CB state
curl -s http://localhost:8084/actuator/circuitbreakers | jq

docker compose start account-service
```

## Cấu trúc thư mục

```
changee_microservice/
├── pom.xml                        # parent POM (multi-module, dependencyManagement)
├── docker-compose.yml             # toàn bộ stack
├── docker/datadog/datadog.yaml    # Datadog Agent config (OTLP receiver)
├── .env.example                   # mẫu env
├── common-lib/                    # JWT utils, header constants, Kafka event schemas
├── discovery-service/             # Eureka Server
├── api-gateway/                   # Spring Cloud Gateway
├── auth-service/
├── user-service/
├── account-service/
├── transaction-service/
└── notification-service/
```

## Ghi chú versions

- **Spring Boot 4.0.6**, **Java 25**
- **Spring Cloud 2026.0.0** (release train cho Spring Boot 4) — chỉnh trong `pom.xml` nếu version chính xác khác
- **OpenTelemetry Java Agent 2.10.0** — pin trong từng `Dockerfile` (`ARG OTEL_AGENT_VERSION`)

## Stop / cleanup

```bash
docker compose down            # giữ data
docker compose down -v         # xoá luôn volumes (Postgres data)
```

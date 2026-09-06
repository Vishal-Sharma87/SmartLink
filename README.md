# SmartLink

SmartLink is a verdict-aware URL shortener. A short URL is not merely a lookup: it must allocate a compact identifier safely under concurrent creation, avoid making a click wait for analysis work, and give a visitor a safety decision before sending them to an unknown destination.

The application is a Spring Boot monolith with a Thymeleaf/vanilla-JS UI. MongoDB is the durable store; Redis handles atomic allocation, redirect-state lookup, and expiring OTP state; Kafka moves slow scanning and analytics work off the request path.

## The request paths that matter

| Workflow            | Request-facing work                                                                                                | Deferred or follow-up work                                                                                                                         |
| ------------------- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| Async link creation | Allocate a Redis counter, Base62-encode it, build a short URL, publish `LinkCreationPayload`, return `202 PENDING` | Consumer scans with VirusTotal, persists the link, and sends email                                                                                 |
| Sync link creation  | Allocate, scan, persist, return `201 ACTIVE`                                                                       | None; the caller waits for scanning                                                                                                                |
| Public redirect     | Read the two-field redirect cache; fall back to MongoDB and refill it                                              | Render the verdict-specific page                                                                                                                   |
| Click tracking      | Accept browser data at `POST /api/track`, publish `LinkAnalysisPayload`, return `204`                              | Consumer increments clicks, looks up IP data, classifies device/browser/OS, and persists analytics                                                 |
| Abuse report        | Validate and consume OTP, reject an existing reporter/hash pair, return `202`                                      | Async worker increments report count, may switch verdict to `PENDING_REVERIFICATION`, invalidates redirect cache, persists report, emails reporter |

The asynchronous creation path intentionally returns before VirusTotal and persistence complete: the consumer owns those slower external and database operations. The synchronous endpoint remains available for callers that explicitly need the completed result in the request.

## How the main pieces work

### Link allocation and storage

Redis seeds `url_counter` and allocates identifiers with `INCR`; `Base62` converts that number into a compact public code. `INCR` is atomic, so concurrent create requests do not need an application-level lock to obtain distinct IDs. The code also rejects a decoded value beyond the last allocated counter before attempting a link lookup, avoiding work for impossible future identifiers.

MongoDB stores the durable `Link`, its verdict, counts, and owner. Link reads used by the dashboard are owner-scoped; the public redirect path instead resolves by short hash because it must work without authentication.

### Verdict-aware navigation

`RedirectService` reads a Redis Hash named from `redirection-cache-prefix + shortCode`. It stores only `longUrl` and `status`, the two values required to decide where and how to continue. On a cache miss or incomplete hash it loads MongoDB, repopulates the Hash, and then chooses a view:

| Verdict                                              | Result                                              |
| ---------------------------------------------------- | --------------------------------------------------- |
| `SAFE`                                               | `track.html`, which queues analytics then redirects |
| `SUSPICIOUS`, `UNVERIFIED`, `PENDING_REVERIFICATION` | `suspicious-warning.html`                           |
| `MALICIOUS`                                          | `malicious-warning.html`                            |

The confirmation route always resolves the destination and returns the tracking page after a visitor elects to continue. Redis caches the narrow redirect representation rather than a serialized link DTO because the lookup consumes only these two fields.

### Asynchronous scanning and analytics

`EventPublisher` is the one application publish path. It publishes a link-creation payload to the configured creation topic and a tracking payload to the configured analysis topic. Each listener declares its own group ID while sharing the same configured consumer factory; the group belongs to the listener because that factory serves both flows.

`LinkCreationConsumer` blocks only in the consumer while waiting for VirusTotal, saves the resulting `Link`, and sends either a normal link-created message or a malicious-link message. `VirusTotalService` polls every five seconds for a completed analysis, caps a scan at two minutes, and falls back to `UNVERIFIED` on errors. `VerdictEvaluationService` persists the raw scan ratios and reason alongside the derived verdict.

`LinkAnalysisConsumer` first increments the link click count. If the link cannot be resolved, it drops the event rather than persisting orphaned analytics. Otherwise it enriches the browser payload with IPInfo data and device classification, then saves a `LinkInformation` record. This lets the browser leave immediately rather than wait for enrichment.

### OTPs, reporting, and notifications

`OtpService` generates a four-digit value with `SecureRandom`, hashes it with SHA-256/Base64, and stores it in Redis for ten minutes. Validation uses one Lua script that compares the stored digest and deletes the key only when it matches. Combining compare and delete in Redis makes a successfully used OTP one-time even when requests race.

Report acceptance validates the OTP first, then checks for a prior report from the same email for the same short code before enqueueing `AsyncReportService`. The worker uses MongoDB `findAndModify` with `$inc` to obtain the new report count atomically. At three reports it changes the link to `PENDING_REVERIFICATION` and removes its redirect cache entry, so subsequent redirects must resolve the newer durable state.

Email creation is separated into content construction, delivery service, and Brevo provider call. OTP, welcome, link-created, malicious-link, and report-accepted flows all reuse that path; provider failures are logged rather than returned to the initiating request.

## Configuration and local setup

`application.yml` imports an optional root `.env` file (`optional:file:.env[.properties]`). Spring scans `@ConfigurationProperties` records, so connection details, keys, message text, and provider credentials are externalized instead of scattered as literals.

| Area                                   | Bound records                                                                                                    |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Link, response, and exception settings | `ApplicationConfigs`, `ResponseMessage`, `ExceptionMessages`, `LinkKeys`, `UserKeys`, `AbuseReportKeys`          |
| Infrastructure                         | `RedisConnectionConfigs`, `RedisKeys`, `ConnectionConfigs`, `KafkaProducerConfigs`, `KafkaTopics`                |
| External/security services             | `VirusTotalConfigs`, `IpInfoConfigs`, `JwtConfigs`, `EmailProviderConfigs`, `EmailSenderConfigs`, `EmailSubject` |
| MVC page names                         | `RedirectionPageConfigs`                                                                                         |

```bash
cp .env.example .env
# Replace CHANGE_ME values and provider credentials.
docker compose up -d mongodb redis kafka
mvn spring-boot:run
```

`.env.example` documents every required variable, including `MONGODB_URI`, Redis/Kafka connection values, `COMPANY_ENDPOINT`, JWT settings, VirusTotal/IPInfo credentials, Brevo credentials, message text, and Docker image/port values. Do not commit a populated `.env` file.

## API contract

Successful JSON endpoints generally wrap their concrete body in:

```json
{
  "data": "<endpoint-specific value>",
  "timestamp": "<ISO-8601 Instant>"
}
```

For example, `POST /link/create` returns `202 Accepted` with an `ApiResponse<LinkCreationResponseDto>`:

```json
{
  "data": {
    "message": "Link creation is pending analysis.",
    "status": "PENDING",
    "shortUrl": "<COMPANY_ENDPOINT>/<Base62 short code>"
  },
  "timestamp": "<creation time>"
}
```

Domain and authentication failures are different: `GlobalExceptionHandler` returns `ApiError` with `timestamp`, `message`, `code`, and `httpStatus`. Not every endpoint uses the success envelope—`DELETE /link/` and `GET /user` return plain strings, `POST /verify/generate-otp` is empty, and MVC navigation routes return views.

| Method | Endpoint                          | Result                                                    |
| ------ | --------------------------------- | --------------------------------------------------------- |
| POST   | `/auth/signup`                    | `ApiResponse<AuthResponse>`: `token`, `message`           |
| POST   | `/auth/login`                     | `ApiResponse<AuthResponse>`: `token`, `message`           |
| POST   | `/auth/signup/initiate`           | `ApiResponse<SignupInitiateResponse>`: `email`, `message` |
| POST   | `/auth/signup/verify`             | `ApiResponse<AuthResponse>`: `token`, `message`           |
| POST   | `/verify/generate-otp`            | `200 OK`, empty body                                      |
| POST   | `/link/create`                    | `202`, `ApiResponse<LinkCreationResponseDto>`             |
| POST   | `/link/create/sync`               | `201`, `ApiResponse<LinkCreationResponseDto>`             |
| GET    | `/link/`                          | `ApiResponse<LinkQueryResponseDto>`: `links`, `message`   |
| GET    | `/link/{hash}`                    | `ApiResponse<LinkAsResponseDto>`                          |
| DELETE | `/link/{hash}`                    | `ApiResponse<String>`                                     |
| DELETE | `/link/`                          | plain `String`                                            |
| GET    | `/link/debug/verdict/{shortCode}` | `ApiResponse<LinkScanResponse>`                           |
| GET    | `/analytics/link?shortHash=...`   | `ApiResponse<LinkAnalyticsResponseDto>`                   |
| POST   | `/report-abuse/`                  | `202`, `ApiResponse<String>`                              |
| GET    | `/user` or `/user/`               | plain `Working` string                                    |
| DELETE | `/user/delete-user`               | `ApiResponse<String>`                                     |
| POST   | `/api/track`                      | `204 No Content`                                          |
| GET    | `/{hash}`                         | verdict-selected Thymeleaf view                           |
| GET    | `/api/confirm/{shortCode}`        | tracking-page view after confirmation                     |

Protected link, analytics, and user endpoints require a Bearer JWT. Public short-code redirects are limited by the configured seven-alphanumeric-character matcher.

## Project layout

The package structure follows features rather than global technical buckets:

```text
com.spring.springboot.smartlink/
├── analytics/      controller, DTO, entity, repository, services
├── link/           controller, DTOs, entity, repository, services, keys
├── user/           authentication plus user controller, entities, repositories, services
├── redirection/    controllers, tracking DTO, redirect service
├── redis/          configuration, keys, hash/value repositories, Lua scripts, service
├── kafka/          configuration, payloads, consumers, publisher
├── report/         controller, DTO, entity, repository, services, keys
├── email/          configuration, content builder, DTOs, provider/service
├── jwt/, geoip/, virustotal/, otp/
├── apiresponse/, advices/, configurations/
└── utils/
```

This puts a feature's controller, persistence code, and service behavior near each other, while keeping cross-cutting exception, shared response, and application configuration code explicit.

## Engineering decisions

### Redis `INCR` plus Base62 instead of application-generated short codes

The numeric source is allocated atomically by Redis, then encoded for a shorter public path. That gives concurrency-safe allocation with a reversible code used for both lookup and counter-bound validation. The tradeoff is that codes are predictable identifiers, not secrets, and Redis counter continuity matters.

### A two-field Redis Hash instead of caching a link DTO

Redirects need only the destination and verdict. Storing `longUrl` and `status` directly matches the read path and avoids serializing/deserializing unrelated link fields. The tradeoff is explicit invalidation: redirect hashes have no TTL and can remain stale until a flow removes or replaces them.

### Kafka for slow workflows, not a blanket replacement for HTTP

The create request publishes then returns, and click tracking returns `204` after publishing. VirusTotal polling and analytics enrichment therefore happen away from the caller. The synchronous create endpoint is deliberately retained, showing the opposite tradeoff: immediate completion at the cost of request latency.

### Shared Kafka infrastructure with listener-owned groups

Both consumers use one `KafkaTemplate`, one consumer factory, and one listener factory with a JSON converter and a fixed retry policy. Topic-specific group IDs remain on `@KafkaListener`, which lets the common factory serve both distinct consumer groups without duplicating connection configuration.

### Typed errors with a shared response writer

Each domain exception carries an `ErrorCode`; the advice maps that code to the HTTP status and emits one `ApiError` shape. The concrete exception still names the domain failure, while HTTP mapping stays centralized. The current API deliberately keeps error and success shapes separate rather than placing error fields in `ApiResponse<T>`.

### Feature-oriented packages instead of technical-role packages

The current package layout groups link, report, analytics, Redis, Kafka, and user behavior with their own DTOs, entities, repositories, and services. This keeps a feature's request path and persistence collaborators together; shared concerns remain in dedicated packages.

## Reliability ledger

Handled:

- Redis `INCR` allocates counter values atomically.
- OTP validation and deletion are one Redis Lua operation.
- Report-count increment is a MongoDB `findAndModify` `$inc`.
- Invalid/future decoded hashes are rejected before a lookup.
- VirusTotal scan errors and the two-minute polling timeout yield `UNVERIFIED`.
- Redirect cache misses fall back to MongoDB.
- Kafka listeners use a fixed one-second backoff with two retries.

Not handled or intentionally incomplete:

- Kafka publishing is fire-and-forget: no outbox, result handling, DLT, replay path, or consumer deduplication is implemented.
- Link-creation and analytics persistence are at-least-once from the application perspective; duplicate delivery can repeat side effects.
- Redirect cache entries have no TTL.
- Duplicate-report prevention is a read before asynchronous report processing, not a database uniqueness constraint or atomic claim.
- Email-provider failures are logged and swallowed.
- IPInfo only handles rate-limit failures explicitly; other lookup failures can fail the listener.
- JWTs have no refresh, revocation, or server-side logout mechanism.
- CSRF is disabled, and OTP rate limiting is not implemented.

## What this project demonstrates

SmartLink demonstrates a deliberately practical boundary: concise URL allocation and navigation remain quick, while threat analysis, analytics enrichment, and notifications are moved to the components that can tolerate asynchronous work. The implementation also makes its tradeoffs visible—especially around cache invalidation, Kafka delivery guarantees, and external-service failure—rather than presenting a shortener as a simple map from one URL to another.

# SmartLink

SmartLink is a Spring Boot URL-shortening application that creates compact Base62 links, evaluates destinations with VirusTotal, records click context asynchronously, and applies verdict-aware redirect behavior through a Thymeleaf web UI.

This README describes the implementation in this repository. It does not claim exactly-once messaging, production-grade moderation, or complete external-service resilience.

## Table of Contents

- [Core Objective](#core-objective)
- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [SmartLink Workflows](#smartlink-workflows)
- [Verdict Lifecycle](#verdict-lifecycle)
- [Kafka Topics](#kafka-topics)
- [System Components](#system-components)
- [Data Stores](#data-stores)
- [Security and Authentication](#security-and-authentication)
- [Frontend](#frontend)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Configuration](#configuration)
- [REST APIs](#rest-apis)
- [Testing the System](#testing-the-system)
- [Design Decisions](#design-decisions)
- [Reliability and Current Limitations](#reliability-and-current-limitations)
- [Future Improvements](#future-improvements)
- [Conclusion](#conclusion)

## Core Objective

SmartLink combines link shortening, destination safety checks, link ownership, click analytics, and community abuse reporting in one Spring MVC application.

The public short-link route is verdict-aware:

- `SAFE` links show a tracking page and then redirect.
- `SUSPICIOUS`, `UNVERIFIED`, and `PENDING_REVERIFICATION` links show a warning page before a user can proceed.
- `MALICIOUS` links are blocked.

## Features

- Authenticated link creation, listing, detail lookup, and deletion.
- Atomic numeric ID allocation in Redis followed by reversible Base62 encoding.
- Asynchronous and synchronous link-creation endpoints.
- VirusTotal submission and polling with verdict evaluation.
- MongoDB persistence for users, links, scan results, abuse reports, and click records.
- Redis Hash redirect state and Redis-backed, expiring OTP state.
- Kafka-backed link creation and click-analysis workflows.
- JWT authentication with BCrypt passwords and ten-minute token expiry.
- Session-backed email OTP signup.
- OTP-protected abuse reports and duplicate-report checks.
- SendGrid link/report notifications where the corresponding code path is used.
- IPInfo Lite country/continent enrichment and browser, operating-system, and device classification.
- Thymeleaf pages with focused JavaScript for dashboard, analytics, authentication, signup verification, reporting, and tracking.

## Architecture Overview

```text
Browser / REST client
        |
        v
Spring MVC controllers
        |
        +--> Redis: URL counter, OTP values, redirect Hashes
        +--> MongoDB: users, links, scans, reports, click records
        +--> Kafka producers
                 |
                 +--> link creation consumer --> VirusTotal --> MongoDB + SendGrid
                 |
                 +--> click analysis consumer --> IPInfo --> MongoDB

GET /{hash}
        |
        +--> verdict lookup --> track.html / suspicious-warning.html / malicious-warning.html
                                      |
                                      +--> POST /api/track --> Kafka click-analysis topic
                                      +--> browser redirect after tracking request
```

The HTTP layer handles authentication, ownership checks, input orchestration, and page rendering. Kafka moves slow or asynchronous work away from link creation and redirect requests. MongoDB is the durable store. Redis holds allocation and short-lived/stateful redirect data.

## SmartLink Workflows

### Link creation

`POST /link/create` obtains the next value from Redis key `url_counter`, encodes it with `Base62`, and publishes a `LinkCreationDto` to `smart_link_link_creation`. It returns `202 Accepted` with a `PROCESSING` response before the link has been persisted.

`ConsumerLinkCreationEvent` scans the URL with VirusTotal. When the reactive scan completes, the consumer callback saves a `Link` document with the resulting `Verdict` and attempts a notification email. The link becomes available after asynchronous processing.

`POST /link/create/sync` performs the counter, Base62, scan, and save sequence synchronously by blocking on the scan and returns `201 Created`. Scanner latency is therefore part of request latency.

### Fetching, redirecting, and tracking

Authenticated dashboard reads use `GET /link/` for the current user's links and `GET /link/{hash}` for one owned link. The public `GET /{hash}` route decodes the Base62 value, reads the link from MongoDB, and chooses a Thymeleaf page from its verdict.

The redirect service also uses Redis Hashes named `smart-link:redirection-hash:<hash>`. A cache miss falls back to MongoDB and writes the URL and status into the Hash. `GET /api/confirm/{shortCode}` is the confirmation flow used after the suspicious warning page's Proceed action.

The tracking page sends `TrackPayloadDto` to `POST /api/track` with the short hash, screen and viewport widths, user-agent, and timezone. The server adds the client IP and publishes a `LinkAnalysisDto`. The browser then navigates to the original URL regardless of whether the fire-and-forget tracking request succeeds.

The analysis consumer resolves the link, enriches the IP with IPInfo Lite, classifies browser/OS/device, increments the link click counter, and stores a `LinkInformation` document. `/analytics/link` summarizes those records for the authenticated owner.

### Abuse reporting

1.  The report page requests an OTP through `POST /verify/generate-otp`.
2.  The OTP's SHA-256/Base64 digest is stored under the reporter email in Redis for ten minutes. The current implementation logs the plaintext OTP because email delivery is marked as a TODO in `OtpService`.
3.  `POST /report-abuse` validates and consumes the OTP through a Redis Lua script.
4.  `ReportLinkService` extracts the last path segment from the submitted short URL and rejects a report already made by the same email for that hash.
5.  `AsyncReportService` increments the MongoDB report count. At three or more reports it changes the link status to `PENDING_REVERIFICATION` and removes its redirect Hash.
6.  The abuse report is stored in `link_reports`; a confirmation email is attempted.

The repository does not implement a human moderation queue or an automatic re-scan after a report.

### Authentication and signup

Login calls `POST /auth/login` and receives a JWT in the `data` field of `AuthResponseDto`. The browser stores it in `sessionStorage` under `smartlink_token` and sends it as `Authorization: Bearer <token>` for protected API requests.

The current UI uses the two-step flow: `POST /auth/signup/initiate` sends an OTP and stores signup details in the HTTP session, then `POST /auth/signup/verify` validates the OTP and creates the user. The older `POST /auth/signup` endpoint remains in the backend and accepts all signup fields, including an OTP, in one request.

## Verdict Lifecycle

`VerdictEvaluationService` calculates ratios from VirusTotal harmless, malicious, suspicious, undetected, and timeout counts and stores a `LinkScanResponse` in `link_scan_matrices`.

| Condition                                               | Verdict      | Reason                         |
| ------------------------------------------------------- | ------------ | ------------------------------ |
| No engines returned signals                             | `UNVERIFIED` | No scan matrix is written      |
| Malicious ratio greater than 5%                         | `MALICIOUS`  | `MALICIOUS_THRESHOLD_EXCEEDED` |
| Any malicious or suspicious result                      | `MALICIOUS`  | `MALICIOUS_THRESHOLD_EXCEEDED` |
| Harmless ratio at least 50% and timeout ratio below 20% | `SAFE`       | `SAFE_CONSENSUS_MET`           |
| Undetected ratio above 60% or timeout ratio above 40%   | `UNVERIFIED` | `UNDETECTED_MAJORITY`          |
| Otherwise                                               | `SUSPICIOUS` | `FALLBACK_NO_CONDITION_MET`    |

Enum values are persisted in the Redis redirect Hash as `enumValue.name()`, for example `SAFE`, and are parsed with `Verdict.valueOf(...)`. `Verdict` keeps its explicit Jackson `@JsonValue` method, which also returns `name()`; no JSON behavior is changed merely because other enums exist.

## Kafka Topics

The application references two topics. No `NewTopic` bean or Compose topic declaration is checked in, so the broker must allow auto-creation or the topics must be created separately.

| Topic                              | Producer          | Consumer group        | Payload           | Purpose                                 |
| ---------------------------------- | ----------------- | --------------------- | ----------------- | --------------------------------------- |
| `smart_link_link_creation`         | `LinkService`     | `link-creation-group` | `LinkCreationDto` | Scan and persist a newly requested link |
| `smart_link_existed_link_analysis` | `AnalysisService` | `link-analysis-group` | `LinkAnalysisDto` | Enrich and persist click analytics      |

Both producers call `send(topic, value)` without an explicit key, so the application does not define a key-based partition strategy. Listener factories use JSON deserialization and trusted package `com.spring.springboot.smartlink.model`. Each listener has a `DefaultErrorHandler` with two retries separated by one second. There is no DLT, event ID, explicit producer-result handling, or consumer deduplication.

## System Components

- `AuthController`, `OtpController`, and `UserController`: authentication, OTP generation, credential updates, and account deletion.
- `LinkController`: authenticated link creation, reads, deletion, and scan-detail lookup.
- `RedirectionController` and `IntermediateRedirectingController`: public verdict-aware navigation and tracking/confirmation pages.
- `AnalyticsController` and `ReportLinkController`: owner analytics and public abuse reports.
- `UiController`: server-rendered Thymeleaf page routes and report-cause model data.
- `LinkService`, `RedirectService`, `AnalysisService`, and `LinkAnalyticsService`: link, redirect, tracking, and analytics orchestration.
- `VirusTotalService` and `VerdictEvaluationService`: URL scanning, polling, and verdict persistence.
- `AuthService`, `JwtService`, `OtpService`, `UserService`, and `EmailService`: identity, token, OTP, account, and notifications.
- `MongoLinkService` and Spring Data repositories: MongoDB queries, atomic report updates, and document persistence.
- `RedisService`: atomic counter allocation, redirect Hash access, and cache removal.
- `ConsumerLinkCreationEvent` and `ConsumerExistedLinkAnalysisEvent`: Kafka consumers.

## Data Stores

### MongoDB

| Collection           | Document           | Main data                                                                    |
| -------------------- | ------------------ | ---------------------------------------------------------------------------- |
| `users`              | `User`             | Username, email, BCrypt password, roles, creation date, malicious URL count  |
| `links`              | `Link`             | Numeric ID, URL, Base62 hash, owner, verdict, creation time, reports, clicks |
| `link_scan_matrices` | `LinkScanResponse` | VirusTotal ratios, engine count, verdict/reason, URL, analysis time          |
| `link_analytics`     | `LinkInformation`  | Click time, IP information, timezone, browser, device, OS                    |
| `link_reports`       | `AbuseReport`      | Link hash, reporter, causes, description, timestamp, report status           |

MongoDB auto-index creation is enabled. User username/email are unique indexes; link owner and analytics short hash are indexed. Link lookup for owner operations uses the numeric decoded ID and owner username.

### Redis

Redis is used for more than a passive cache:

1.  `url_counter` is initialized if absent and incremented with Redis `INCR`. The result is both the MongoDB `Link.id` and the input to Base62 encoding.
2.  `smart-link:redirection-hash:<hash>` is a Redis Hash containing `longUrl` and `status`. Hash values are strings through `StringRedisTemplate`; the status is written using `link.getStatus().name()`. This removes object/DTO JSON conversion overhead from redirect-state reads and writes.
3.  The reporter/signup email key stores a hashed OTP with a ten-minute TTL. A Lua script compares the supplied hash and deletes the OTP on successful validation.

The redirect Hash itself has no TTL configured in the current `RedisService`; it is explicitly removed when an abuse threshold changes a link to pending reverification. MongoDB remains the durable source for links, while Redis provides fast redirect state and allocation/OTP state.

## Security and Authentication

- Passwords are encoded with `BCryptPasswordEncoder`.
- `JwtService` signs `sub`, `name`, `iat`, and `exp` claims with the configured HMAC secret; tokens expire after ten minutes.
- `JwtFilter` accepts `Authorization: Bearer ...` for JWT authentication and loads authorities from MongoDB.
- `/user/**` requires `ROLE_USER`; other protected API routes are covered by the final authenticated rule.
- Authentication, UI routes, static assets, OTP endpoints, report endpoints, and `/api/**` are explicitly permitted as configured.
- A seven-alphanumeric root GET is also permitted for short-link access.
- CSRF is disabled. The UI performs logout by removing its session-storage token; there is no server-side revocation or refresh-token endpoint.

## Frontend

The frontend is server-rendered Thymeleaf with focused browser JavaScript, not a SPA.

| Page/template           | Route or use                    | Responsibility                                      |
| ----------------------- | ------------------------------- | --------------------------------------------------- |
| `index.html`            | `/`, `/home`                    | Product entry page                                  |
| `auth.html`             | `/auth`, `/login`, `/signup`    | Login and signup initiation                         |
| `signup-verify.html`    | `/signup/verify`                | Session-backed OTP verification                     |
| `dashboard.html`        | `/dashboard`                    | Create, list, inspect, copy, and delete owned links |
| `analytics.html`        | `/analytics`                    | Select a link and display click summaries           |
| `report-abuse.html`     | `/report-abuse-page`, `/report` | OTP-protected abuse report form                     |
| `track.html`            | Internal redirect view          | Submit browser telemetry, then navigate             |
| Warning/error templates | Internal                        | Suspicious, malicious, or missing-link outcomes     |

`site.js` owns JWT storage, common alerts, logout, navigation state, and date formatting. `dashboard.js` calls `/link/`, `/link/{hash}`, `/link/create`, `/link/debug/verdict/{shortCode}`, and `DELETE /link/{hash}`. `analytics.js` calls `/link/` and `/analytics/link?shortHash=...`. Authentication, signup verification, and abuse-report scripts call the matching `/auth`, `/verify`, and `/report-abuse` endpoints. Thymeleaf supplies `shortCode`, `longUrl`, and `reportCauses` where needed.

## Technology Stack

- Java 21 and Spring Boot 3.4.10.
- Spring MVC, WebFlux, Validation, Security, Thymeleaf, Data MongoDB, Data Redis, and Kafka.
- MongoDB, Redis, and Apache Kafka through Docker Compose.
- JJWT 0.12.5, Lombok, SendGrid Java 4.10.1, and IPInfo API 3.4.0.
- Maven Wrapper.

## Project Structure

```text
SmartLink/
├── docker-compose.yml
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/java/com/spring/springboot/smartlink/
    │   ├── advices/          # API errors and exception handling
    │   ├── configurations/   # Security, Redis, Kafka, async configuration
    │   ├── controllers/      # REST and Thymeleaf entry points
    │   ├── dto/              # Request, response, external, and email DTOs
    │   ├── entity/           # MongoDB documents
    │   ├── enums/            # Verdict, report, browser, device, OS values
    │   ├── filter/           # JWT filter
    │   ├── kafka/            # Kafka consumers
    │   ├── model/            # Kafka payload models
    │   ├── repositories/     # Spring Data repositories
    │   ├── scripts/          # Redis Lua scripts
    │   └── services/         # Application workflows and integrations
    └── main/resources/
        ├── application.yml
        ├── static/css/ and static/js/
        └── templates/
```

## Local Setup

### Prerequisites

- JDK 21.
- Docker Engine with Docker Compose.
- Credentials/configuration for MongoDB, Redis, Kafka, VirusTotal, SendGrid, IPInfo, and JWT.

### Start infrastructure

`application.yml` imports an optional `.env` file. Create one with the variables in the Configuration section, then start the local services:

```bash
docker compose up -d mongodb redis kafka
docker compose ps
```

The Compose service/container names are `mongodb`/`smart-link-mongodb`, `redis`/`smart-link-redis`, and `kafka`/`smart-link-kafka`. The Compose file uses environment-provided image tags and host ports; it does not hard-code application credentials.

The two Kafka topics are not declared in Compose. If broker auto-creation is disabled, create `smart_link_link_creation` and `smart_link_existed_link_analysis` in the local broker before starting the application.

### Start SmartLink

```bash
bash mvnw spring-boot:run
```

Or build and run the jar:

```bash
bash mvnw clean package
java -jar target/smartlink-0.0.1-SNAPSHOT.jar
```

The application is available at `http://localhost:8080` by default. `COMPANY_ENDPOINT` is used when constructing returned short URLs and notification content.

## Configuration

| Area              | Environment variables                                                                                                             |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| MongoDB           | `MONGODB_URI`, `MONGODB_IMAGE_TAG`, `MONGODB_PORT`, `MONGODB_DATABASE`                                                            |
| Redis             | `REDIS_HOST`, `REDIS_PORT`, `REDIS_IMAGE_TAG`                                                                                     |
| Kafka client      | `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_MECHANISM`, `KAFKA_JAAS_CONFIG`, `KAFKA_SESSION_TIMEOUT`        |
| Kafka serializers | `KAFKA_KEY_SERIALIZER`, `KAFKA_VALUE_SERIALIZER`, `KAFKA_KEY_DESERIALIZER`, `KAFKA_VALUE_DESERIALIZER`, `KAFKA_CONSUMER_GROUP_ID` |
| Kafka Compose     | `KAFKA_IMAGE_TAG`, `KAFKA_PORT`, `KAFKA_ADVERTISED_HOST`                                                                          |
| Link/JWT          | `COMPANY_ENDPOINT`, `JWT_SECRET_KEY`                                                                                              |
| VirusTotal        | `VT_API_KEY`, `VT_ANALYSIS_URL`                                                                                                   |
| SendGrid          | `SENDGRID_API_KEY`, `SENDGRID_EMAIL`                                                                                              |
| IPInfo            | `IP_INFO_TOKEN`                                                                                                                   |

The application reads these values through `application.yml`; the Compose-only image, database, and port variables are used by `docker-compose.yml`. Do not place real credentials in source control.

## REST APIs

Base URL: `http://localhost:8080`.

| Method | Endpoint                          | Authentication                                | Purpose                                          |
| ------ | --------------------------------- | --------------------------------------------- | ------------------------------------------------ |
| POST   | `/auth/signup`                    | Public                                        | Legacy single-request signup with OTP            |
| POST   | `/auth/login`                     | Public                                        | Authenticate and return JWT in `data`            |
| POST   | `/auth/signup/initiate`           | Public                                        | Send signup OTP and create pending session state |
| POST   | `/auth/signup/verify`             | Pending signup session                        | Validate OTP and create account                  |
| POST   | `/verify/generate-otp`            | Public                                        | Store an OTP for an email                        |
| POST   | `/link/create`                    | Authenticated                                 | Queue link creation; returns `202`               |
| POST   | `/link/create/sync`               | Authenticated                                 | Scan and create synchronously; returns `201`     |
| GET    | `/link/`                          | Authenticated                                 | List links owned by the current user             |
| GET    | `/link/{hash}`                    | Authenticated                                 | Read one owned link                              |
| DELETE | `/link/{hash}`                    | Authenticated                                 | Delete one owned link                            |
| DELETE | `/link/`                          | Authenticated                                 | Delete all links owned by the current user       |
| GET    | `/link/debug/verdict/{shortCode}` | Authenticated                                 | Read stored VirusTotal scan details              |
| GET    | `/{hash}`                         | Public for configured seven-character matcher | Verdict-aware redirect page                      |
| GET    | `/api/confirm/{shortCode}`        | Public                                        | Confirmation redirect flow                       |
| POST   | `/api/track`                      | Public                                        | Queue click telemetry; no response body          |
| GET    | `/analytics/link?shortHash=...`   | Authenticated                                 | Summarize analytics for an owned link            |
| POST   | `/report-abuse`                   | Public                                        | Validate OTP and queue abuse report              |
| GET    | `/user` or `/user/`               | `ROLE_USER`                                   | User endpoint health response                    |
| PUT    | `/user/update-user-credentials`   | `ROLE_USER`                                   | Update non-empty credentials                     |
| DELETE | `/user/delete-user`               | `ROLE_USER`                                   | Delete the user's links and account              |

Important request shapes:

```json
{ "actualUrl": "https://example.com/docs" }
```

```json
{
  "shortHash": "abc1234",
  "screenWidth": 1440,
  "viewportWidth": 1280,
  "userAgent": "...",
  "timezone": "Asia/Kolkata"
}
```

`LinkQueryResponseDto` wraps list responses as `{ "data": [...], "message": "...", "timestamp": "..." }`. `LinkCreationResponseDto` contains `message`, `status`, and `shortUrl`. `LinkAsResponseDto` contains `id`, `actualUrl`, `hashedKey`, `status`, `clickCnt`, `reportCnt`, and `creationTime`. `LinkAnalyticsResponseDto` contains totals, country/continent/device/browser/OS distributions, and up to 20 recent clicks. Domain errors use `ApiError` with `timestamp`, `message`, and `errorCode`.

## Testing the System

The checked-in test is `SmartLinkApplicationTests.contextLoads()`, a Spring context smoke test. It requires MongoDB to be reachable because application startup creates MongoDB infrastructure.

```bash
bash mvnw test
```

For an end-to-end local flow:

1.  Start MongoDB, Redis, Kafka, and the application.
2.  Open `/auth?mode=signup`, submit username/email/password, and read the current OTP from application logs.
3.  Verify the OTP, log in, and create a link from `/dashboard`.
4.  Wait for the Kafka creation consumer and VirusTotal scan to persist the link.
5.  Open the returned short URL and observe the verdict page, tracking request, and final navigation.
6.  Open `/analytics?shortHash=<hash>` after a visit to inspect click summaries.
7.  Use `/report-abuse-page` to request an OTP and submit a verified report.

Useful local inspection commands include `docker compose logs kafka`, `docker exec smart-link-redis redis-cli GET url_counter`, and `docker exec smart-link-redis redis-cli HGETALL smart-link:redirection-hash:<hash>`.

## Design Decisions

### Redis counter and Base62

**Problem:** Concurrent link creation needs a compact public identifier and a unique persistent ID.

**Decision:** Allocate with Redis `INCR`, store the numeric result as the MongoDB ID, and encode it with Base62.

**Why:** Redis provides an atomic increment and the reversible encoding lets services recover the numeric ID from a public hash.

**Tradeoff:** The counter and its continuity are on the creation availability path; codes are predictable and are not secrets.

### Redis Hash redirect state

**Problem:** Redirects repeatedly need only the destination URL and verdict, while object/JSON cache storage adds conversion overhead.

**Decision:** Store `longUrl` and the `Verdict.name()` string in a Redis Hash.

**Why:** Hash fields map directly to the two values required by redirect logic and keep enum representation stable for `Verdict.valueOf(...)`.

**Tradeoff:** The Hash has no TTL and can become stale after a MongoDB verdict change unless explicitly removed; MongoDB remains the fallback source.

### Kafka boundaries

**Problem:** VirusTotal polling and click enrichment are slow relative to HTTP requests.

**Decision:** Publish link-creation and click-analysis events to separate topics and consumer groups.

**Why:** Requests can hand work off while consumers perform external calls and persistence.

**Tradeoff:** Creation and analytics are eventually consistent, and callbacks do not provide durable workflow state or exactly-once processing.

### Tracking page

**Problem:** Screen size, viewport size, browser user-agent, and timezone exist only in the browser.

**Decision:** Render an intermediate Thymeleaf page that posts telemetry before navigating.

**Why:** It captures browser-only context without requiring a separate frontend application.

**Tradeoff:** Redirects take an extra page hop and tracking is best-effort.

## Reliability and Current Limitations

### Implemented

- Redis `INCR` provides atomic counter allocation while the key is available.
- OTP validation compares and deletes in one Redis Lua operation and OTP values expire after ten minutes.
- Kafka listener failures receive two one-second retries.
- VirusTotal polling repeats every five seconds and times out after two minutes; errors become `UNVERIFIED`.
- Redirect confirmation falls back to MongoDB when redirect state is absent.
- MongoDB `findAndModify` atomically increments abuse report counts.
- Authentication and domain exceptions are mapped to structured API errors in `GlobalExceptionHandler`.

### Current limitations

- Kafka sends are not awaited or checked, and there is no outbox, DLT, replay flow, event ID, or deduplication.
- The link-creation consumer subscribes to the reactive VirusTotal result; failures after the listener returns are not automatically Kafka-redelivered.
- Redis redirect state has no TTL and can be stale after status changes; Redis loss can affect counter continuity.
- Click counter and click-detail persistence are separate MongoDB writes.
- External calls have no circuit breaker; IPInfo rate limiting removes geo data but does not prevent the click record from being stored.
- Duplicate-report checking can race and no unique compound index enforces it.
- OTP plaintext is logged instead of sent by email in the current implementation.
- CSRF is disabled, JWTs cannot be revoked or refreshed, and rate limiting/observability are not implemented.
- Notification logic tracks harmful URLs for user email messaging, but authorization does not enforce an account block.

## Future Improvements

- Add an outbox or durable creation state around MongoDB/Kafka handoff.
- Add event IDs, idempotent consumers, producer-result handling, DLT/replay, and versioned event contracts.
- Add bounded external-service retries, circuit breakers, health checks, metrics, and tracing.
- Deliver OTPs through SendGrid and add abuse-resistant OTP/rate-limit controls.
- Add moderation/re-verification state and enforce any account restrictions represented by product policy.
- Version or invalidate redirect Hash entries when link verdicts change.
- Add Testcontainers integration tests and concurrency tests for allocation, reports, and click updates.

## Conclusion

SmartLink is a server-rendered URL shortener organized around three asynchronous boundaries: safety analysis during creation, click analysis during redirect, and abuse-report processing. Redis supplies atomic and short-lived state, MongoDB stores durable application records, and Kafka keeps slow external work out of the primary request paths.

The current implementation is a useful local engineering system with explicit limitations. Durable event handoff, idempotency, external-service resilience, OTP delivery, moderation enforcement, and production observability remain the clearest next steps.

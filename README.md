# SmartLink

**A verdict-aware URL shortening backend** — built on Spring Boot, with authenticated link ownership, external threat scanning, async analytics, and abuse-reporting workflows.

`Java 21` · `Spring Boot 3.4.10` · `MongoDB 7` · `Redis 7.4` · `Apache Kafka 3.9` · `JWT` · `VirusTotal API` · `Brevo SMTP`

---

## Table of Contents

- [Objective](#objective)
- [Features](#features)
- [Architecture](#architecture)
- [Lifecycle Walkthroughs](#lifecycle-walkthroughs)
  - [Link Creation](#link-creation)
  - [Redirection and Tracking](#redirection-and-tracking)
  - [Abuse Reports](#abuse-reports)
  - [Signup and Authentication](#signup-and-authentication)
- [Kafka](#kafka)
- [Redis](#redis)
- [VirusTotal Analysis](#virustotal-analysis)
- [Email Integration](#email-integration)
- [System Components and Data](#system-components-and-data)
- [Security](#security)
- [Technology Stack and Project Structure](#technology-stack-and-project-structure)
- [Local Setup and Configuration](#local-setup-and-configuration)
- [REST API](#rest-api)
- [Testing and Verification](#testing-and-verification)
- [Design Decisions](#design-decisions)
  - [Redis Counter Plus Base62](#decision-redis-counter-plus-base62)
  - [Direct Redirect Hash Fields](#decision-direct-redirect-hash-fields)
  - [Service-Owned Redirect Behavior](#decision-service-owned-redirect-behavior)
  - [Kafka Workflow Boundaries](#decision-kafka-workflow-boundaries)
  - [Atomic One-Time OTP Use](#decision-atomic-one-time-otp-use)
- [Reliability and Limitations](#reliability-and-limitations)
- [Future Improvements](#future-improvements)

---

## Objective

A short URL service must do more than map a short code to a destination. It must allocate identifiers safely under concurrent creation, keep ownership boundaries clear, avoid blocking redirect requests on analytics work, and give users a safety decision before navigating to a destination.

SmartLink combines authenticated link ownership, compact links, external threat analysis, verdict-aware navigation, click-context analytics, and abuse reporting. MongoDB is the durable application store; Redis supplies atomic allocation, OTP state, and a fast redirect representation; Kafka separates slow analysis workflows from user-facing requests.

> **Scope note:** The repository also contains a server-rendered Thymeleaf UI used to exercise the backend. The engineering scope described in this document is the backend and its supporting infrastructure — the UI is not treated as an architectural contribution.

[⬆ back to top](#table-of-contents)

---

## Features

| Category        | Capability                                                                                                             |
| --------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Link management | Authenticated creation, listing, detail lookup, deletion, and scan-detail lookup                                       |
| ID allocation   | Redis `INCR` allocation followed by reversible Base62 encoding                                                         |
| Link creation   | Asynchronous and synchronous creation endpoints                                                                        |
| Threat scanning | VirusTotal submission, polling, ratio-based verdict evaluation, persisted scan matrices                                |
| Navigation      | Verdict-aware pages — safe links proceed through tracking, uncertain links show a warning, malicious links are blocked |
| Event pipeline  | Kafka-backed link creation and click-analysis workflows                                                                |
| Analytics       | Browser/user-agent, viewport, timezone, IP, country, continent, browser, OS, and device tracking                       |
| Abuse handling  | OTP-protected signup and abuse reporting with one-time Redis/Lua validation                                            |
| Auth            | JWT authentication with BCrypt password storage                                                                        |
| Notifications   | Brevo email delivery for OTP, welcome, link-created, malicious-link, and successful-report events                      |
| Data integrity  | MongoDB atomic report-count updates and threshold transition to `PENDING_REVERIFICATION`                               |

[⬆ back to top](#table-of-contents)

---

## Architecture

```text
HTTP client / Thymeleaf UI
              |
              v
       Spring MVC controllers
              |
              +--> application services
              |       +--> MongoDB repositories / MongoTemplate
              |       +--> RedisService
              |       +--> Kafka producers
              |       +--> Brevo EmailService (WebClient)
              |
              +--> public verdict-aware pages
                           |
                           +--> Kafka click-analysis event

smart_link_link_creation
              |
              v
ConsumerLinkCreationEvent --> VirusTotal --> MongoDB link + scan matrix
                                      \--> Brevo notification

smart_link_existed_link_analysis
              |
              v
ConsumerExistedLinkAnalysisEvent --> IPInfo/user-agent classification --> MongoDB analytics
```

Controllers extract request data, authenticated identity, and page/API concerns. Services own business workflows such as link allocation, ownership checks, redirect-state lookup, OTP validation, report acceptance, account deletion, scanning, and notification composition. Redirect and tracking controllers delegate rather than assembling domain behavior themselves.

[⬆ back to top](#table-of-contents)

---

## Lifecycle Walkthroughs

### Link Creation

`POST /link/create` obtains the next `url_counter` value from Redis, encodes it with `Base62`, builds the configured `COMPANY_ENDPOINT` URL, and publishes `LinkCreationDto` to `smart_link_link_creation`. It returns `202 Accepted` with `PROCESSING`; the MongoDB `Link` does not exist yet.

`ConsumerLinkCreationEvent` extracts the hash, decodes it to the numeric ID, fetches the owner, blocks on the VirusTotal `Mono`, builds and saves the `Link`, and sends the appropriate Brevo notification. A malicious verdict also increments the owner's malicious-link count.

`POST /link/create/sync` performs allocation, VirusTotal scanning, and persistence in the request thread and returns `201 Created`. Scanner latency is consequently part of request latency.

### Redirection and Tracking

Public `GET /{hash}` first rejects a decoded counter greater than the latest Redis counter. `RedirectService` reads `longUrl` and `status` from `smart-link:redirection-hash:<hash>`. On a miss it loads MongoDB and populates the Hash. It renders:

| Verdict                                              | Page rendered             |
| ---------------------------------------------------- | ------------------------- |
| `SAFE`                                               | `track.html`              |
| `SUSPICIOUS`, `UNVERIFIED`, `PENDING_REVERIFICATION` | `suspicious-warning.html` |
| `MALICIOUS`                                          | `malicious-warning.html`  |

The tracking page posts browser telemetry to `POST /api/track`. `AnalysisService` adds the client IP and publishes `LinkAnalysisDto`; the browser continues to the destination without waiting for Kafka or analytics persistence. `GET /api/confirm/{shortCode}` delegates the same service-owned redirect-state preparation for the warning-page Proceed flow.

The analysis consumer resolves the link, looks up IPInfo Lite data, classifies browser/operating system/device, increments the click count, and saves a `LinkInformation` document. The click-count and click-detail writes are separate.

### Abuse Reports

1. `POST /verify/generate-otp` generates a four-digit OTP, hashes it with SHA-256/Base64, stores the digest under the email key in Redis for ten minutes, and sends the plaintext OTP through Brevo.
2. `POST /report-abuse` validates and consumes the OTP with a Redis Lua script, extracts the final short-code path segment, and rejects an existing report from the same reporter email for that hash.
3. `ReportLinkService` queues `AsyncReportService.acceptReport`; the controller returns before asynchronous work completes.
4. The async service atomically increments `reportCount` with MongoDB `findAndModify`, changes the link to `PENDING_REVERIFICATION` at count three or higher, removes the redirect Hash, saves `AbuseReport`, and sends a successful-report email to the reporter.

> There is currently no owner-notification call in this flow, no human moderation queue, and no automatic VirusTotal re-scan after reporting.

### Signup and Authentication

The current two-step flow is `POST /auth/signup/initiate` (username check, OTP delivery, and HTTP-session storage) followed by `POST /auth/signup/verify` (session retrieval, OTP consumption, BCrypt user save, welcome email, and pending-session removal). The backend also retains `POST /auth/signup`, which accepts signup fields and an OTP in one request but does not send the welcome email in `registerUser`.

`POST /auth/login` authenticates through Spring Security and returns a ten-minute HMAC-signed JWT in `AuthResponseDto.data`. Protected requests use `Authorization: Bearer <token>`; link and account operations obtain the username from the authenticated security context.

[⬆ back to top](#table-of-contents)

---

## Kafka

The application defines two domain-specific producer/consumer pairs. Topics are not declared as `NewTopic` beans or in Compose, so broker auto-creation must be enabled or topics must be provisioned separately.

### `smart_link_link_creation`

- **Producer:** `LinkService`
- **Consumer / Group:** `ConsumerLinkCreationEvent` / `link-creation-group`
- **Payload:** `LinkCreationDto`
- **Responsibility:** VirusTotal scan, link persistence, creation notification

### `smart_link_existed_link_analysis`

- **Producer:** `AnalysisService`
- **Consumer / Group:** `ConsumerExistedLinkAnalysisEvent` / `link-analysis-group`
- **Payload:** `LinkAnalysisDto`
- **Responsibility:** IP/user-agent enrichment, click increment, analytics persistence

### Delivery and Reliability Notes

Producers call `KafkaTemplate.send(topic, value)` without a key, so no application-defined key-partitioning strategy is present. JSON deserialization is restricted to `com.spring.springboot.smartlink.model` with explicit default payload types. Both listener factories use `DefaultErrorHandler` with two retries and a one-second fixed backoff. There is no DLT, outbox, event ID, producer-result handling, or consumer deduplication. Abuse reports use Spring `@Async` and the configured five-core/ten-maximum executor with queue capacity 100; they do not use Kafka.

[⬆ back to top](#table-of-contents)

---

## Redis

`RedisService` uses `StringRedisTemplate` operations directly for the active data paths:

| Key / Data Structure                        | Fields / Operation        | Purpose                                             |
| ------------------------------------------- | ------------------------- | --------------------------------------------------- |
| `url_counter` (string)                      | `INCR`                    | Atomic ID allocation and upper-bound validation     |
| `smart-link:redirection-hash:<hash>` (Hash) | `longUrl`, `status`       | Fast redirect lookup; status is `Verdict.name()`    |
| `<email>` (string, TTL)                     | SHA-256/Base64 OTP digest | Signup/report OTP state, expiring after ten minutes |

Redirect reads use Hash `multiGet` and fall back to MongoDB when the Hash is missing or incomplete. The Hash is removed when abuse reports move a link to pending reverification. Redirect Hashes have no configured TTL, so they can remain stale unless invalidated explicitly. MongoDB remains the durable source.

Repository history records an earlier cache-DTO/JSON-value representation. The current redirect path stores the two required fields directly in a Hash, removing the link-cache object's JSON conversion/deserialization from that path. `RedisConfig` still declares a generic JSON-serializer `RedisTemplate`; the service paths use the Spring-provided `StringRedisTemplate`, so this is not a claim that every Redis operation is globally JSON-free.

OTP validation is one Lua operation: compare the supplied digest and delete the key only on success. This prevents concurrent reuse of a valid OTP.

> Enum classes no longer use Lombok `@ToString`. Where reconstruction uses `Enum.valueOf`, code passes `name()` (for example `Verdict.name()` into Redis). `toString()` is not treated as a persistence contract.

[⬆ back to top](#table-of-contents)

---

## VirusTotal Analysis

`VirusTotalService` submits the URL with a WebClient form request, obtains the analysis self-link, polls every five seconds until the provider reports `completed`, and times out after two minutes. Errors and timeout become `UNVERIFIED`.

`VerdictEvaluationService` persists ratios and the reason in `link_scan_matrices`:

| Rule                                                     | Verdict      |
| -------------------------------------------------------- | ------------ |
| No returned engine signals                               | `UNVERIFIED` |
| Malicious ratio > 5%, or any malicious/suspicious result | `MALICIOUS`  |
| Harmless ratio ≥ 50% and timeout ratio < 20%             | `SAFE`       |
| Undetected ratio > 60% or timeout ratio > 40%            | `UNVERIFIED` |
| Otherwise                                                | `SUSPICIOUS` |

[⬆ back to top](#table-of-contents)

---

## Email Integration

Email behavior is separated into `EmailContentBuilder`, `EmailService`, and `EmailProvider`. The builder creates `EmailBody` DTOs containing sender, recipient, subject, and HTML content; the provider posts them to the configured Brevo SMTP endpoint with the `api-key` header using WebClient and calls `.block()`.

Delivery is synchronous inside its caller: link-creation delivery occurs inside the Kafka consumer, OTP and welcome delivery in the HTTP request path, and report confirmation inside the `@Async` report executor. No email-specific Kafka topic exists. `EmailProvider` catches and logs exceptions, so delivery failures are not propagated as failed API responses or listener failures.

| Flow              | Origin                                        | DTO / Template Data                               |
| ----------------- | --------------------------------------------- | ------------------------------------------------- |
| OTP               | `OtpService.sendOtp`                          | recipient email, generated OTP                    |
| Welcome           | `AuthService.completeSignup`                  | username and email                                |
| New link created  | `ConsumerLinkCreationEvent` after scan/save   | username, email, original URL, short URL, verdict |
| Malicious link    | same consumer, for `MALICIOUS`                | username, email, original URL, short URL          |
| Successful report | `AsyncReportService` after report persistence | reporter name/email and submitted link URL        |

> The current code does not send a "link reported" notification to the link owner; no owner lookup plus owner-email call exists in the report path. Subjects, sender name/email, Brevo base URL, and API key are injected from environment-backed `application.yml` properties.

[⬆ back to top](#table-of-contents)

---

## System Components and Data

Controllers: `AuthController`, `OtpController`, `UserController`, `LinkController`, `RedirectionController`, `IntermediateRedirectingController`, `AnalyticsController`, `ReportLinkController`, `UiController`.

Application services cover link/persistence, redirect state, analytics, threat scanning, identity, reporting, and email. Kafka consumers are `ConsumerLinkCreationEvent` and `ConsumerExistedLinkAnalysisEvent`.

| MongoDB Collection   | Entity             | Main Responsibility                                     |
| -------------------- | ------------------ | ------------------------------------------------------- |
| `users`              | `User`             | credentials, roles, creation date, malicious-link count |
| `links`              | `Link`             | ID, destination, hash, owner, verdict, reports, clicks  |
| `link_scan_matrices` | `LinkScanResponse` | scan ratios, verdict/reason, source URL, analysis time  |
| `link_analytics`     | `LinkInformation`  | click time, IP/geo data, timezone, browser, OS, device  |
| `link_reports`       | `AbuseReport`      | reporter, causes, description, hash, status, timestamp  |

MongoDB auto-index creation is enabled. User username/email, link owner, and analytics short hash have indexes. The duplicate-report check is a read followed by async work and is not enforced by a unique compound index.

[⬆ back to top](#table-of-contents)

---

## Security

- Passwords use **BCrypt**.
- JWTs contain subject/name, issued-at, and expiration claims and **expire after ten minutes**.
- `JwtFilter` authenticates bearer tokens and loads authorities.
- `/user/**` requires `ROLE_USER`; seven-alphanumeric root short-code GETs are public.
- Authentication, OTP, reporting, public pages, static resources, and `/api/**` are permitted according to `UrlSecurityConfig`.
- CSRF is disabled; there is no token refresh or server-side revocation endpoint.

[⬆ back to top](#table-of-contents)

---

## Technology Stack and Project Structure

**Stack:** Java 21 · Spring Boot 3.4.10 · Maven Wrapper · Spring MVC/WebFlux · Validation · Security · Thymeleaf · Data MongoDB · Data Redis · Spring Kafka · MongoDB 7 · Redis 7.4 Alpine · Apache Kafka 3.9 · JJWT 0.12.5 · Lombok · IPInfo API 3.4.0 · VirusTotal API · Brevo SMTP API

```text
SmartLink/
├── docker-compose.yml / pom.xml / .env.example
└── src/
    ├── main/java/com/spring/springboot/smartlink/
    │   ├── advices/ configurations/ controllers/ dto/
    │   ├── email/ entity/ enums/ filter/ kafka/ model/
    │   ├── repositories/ scripts/ services/
    └── main/resources/application.yml, static/, templates/
```

[⬆ back to top](#table-of-contents)

---

## Local Setup and Configuration

**Prerequisites:** JDK 21, Docker Engine/Compose, and credentials for the external services.

```bash
cp .env.example .env
# Replace provider placeholders and CHANGE_ME values.
docker compose up -d mongodb redis kafka
bash mvnw spring-boot:run
```

Or build a runnable jar:

```bash
bash mvnw clean package
java -jar target/smartlink-0.0.1-SNAPSHOT.jar
```

`application.yml` imports `.env` optionally. Compose service names are `mongodb`/`smart-link-mongodb`, `redis`/`smart-link-redis`, and `kafka`/`smart-link-kafka`. Compose does not declare application topics.

| Area                 | Variables                                                                                                                                                                          |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| MongoDB              | `MONGODB_URI`, `MONGODB_DATABASE`, `MONGODB_IMAGE_TAG`, `MONGODB_PORT`                                                                                                             |
| Redis                | `REDIS_HOST`, `REDIS_PORT`, `REDIS_IMAGE_TAG`                                                                                                                                      |
| Kafka client/Compose | `KAFKA_BOOTSTRAP_SERVERS`, security/SASL/session variables, serializer/deserializer variables, `KAFKA_CONSUMER_GROUP_ID`, `KAFKA_IMAGE_TAG`, `KAFKA_PORT`, `KAFKA_ADVERTISED_HOST` |
| Link/JWT             | `COMPANY_ENDPOINT`, `JWT_SECRET_KEY`                                                                                                                                               |
| VirusTotal/IPInfo    | `VT_API_KEY`, `VT_ANALYSIS_URL`, `IP_INFO_TOKEN`                                                                                                                                   |
| Brevo                | `EMAIL_PROVIDER_BASE_URL`, `EMAIL_PROVIDER_API_KEY`, `SENDER_NAME`, `SENDER_EMAIL`, and the five `EMAIL_*_SUBJECT` variables                                                       |

[⬆ back to top](#table-of-contents)

---

## REST API

**Base URL:** `http://localhost:8080`

| Method         | Endpoint                                                      | Access                       | Purpose                                  |
| -------------- | ------------------------------------------------------------- | ---------------------------- | ---------------------------------------- |
| POST           | `/auth/signup`                                                | Public                       | Single-request signup with OTP           |
| POST           | `/auth/signup/initiate`                                       | Public                       | Begin session-backed signup and send OTP |
| POST           | `/auth/signup/verify`                                         | Pending signup session       | Verify OTP and create account            |
| POST           | `/auth/login`                                                 | Public                       | Authenticate and return JWT in `data`    |
| POST           | `/verify/generate-otp`                                        | Public                       | Generate/store/send an OTP               |
| POST           | `/link/create`                                                | Authenticated                | Queue link creation; `202`               |
| POST           | `/link/create/sync`                                           | Authenticated                | Scan/save synchronously; `201`           |
| GET            | `/link/`                                                      | Authenticated                | List owned links                         |
| GET            | `/link/{hash}`                                                | Authenticated                | Get one owned link                       |
| DELETE         | `/link/{hash}`                                                | Authenticated                | Delete one owned link                    |
| DELETE         | `/link/`                                                      | Authenticated                | Delete all owned links                   |
| GET            | `/link/debug/verdict/{shortCode}`                             | Authenticated                | Get persisted scan details               |
| GET            | `/{hash}`                                                     | Public, 7 alphanumeric chars | Verdict-aware navigation                 |
| GET            | `/api/confirm/{shortCode}`                                    | Public                       | Warning-page confirmation flow           |
| POST           | `/api/track`                                                  | Public                       | Queue click telemetry                    |
| GET            | `/analytics/link?shortHash=...`                               | Authenticated                | Summarize analytics                      |
| POST           | `/report-abuse`                                               | Public                       | Validate OTP and queue a report          |
| GET/PUT/DELETE | `/user`, `/user/update-user-credentials`, `/user/delete-user` | `ROLE_USER`                  | User health, update, deletion            |

Creation requests contain `actualUrl`. Tracking requests contain `shortHash`, `screenWidth`, `viewportWidth`, `userAgent`, and `timezone`. Link list responses use `LinkQueryResponseDto`; domain errors use `ApiError` with timestamp, message, and error code.

[⬆ back to top](#table-of-contents)

---

## Testing and Verification

The checked-in automated tests are a Spring context smoke test and a JSON-shape test for `EmailBody`. The context test requires configured infrastructure reachable during startup; the JSON test does not call Brevo because its HTTP test block is commented out.

```bash
bash mvnw test
```

**Manual walkthrough:** start MongoDB/Redis/Kafka → complete signup and login → create a link → wait for the creation consumer and VirusTotal result → open the short URL → inspect analytics after tracking → submit an OTP-protected report.

Useful inspection commands:

```bash
docker compose logs kafka
docker exec smart-link-redis redis-cli HGETALL smart-link:redirection-hash:<hash>
```

[⬆ back to top](#table-of-contents)

---

## Design Decisions

### Decision: Redis Counter Plus Base62

|               |                                                                                                            |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| **Problem**   | Concurrent creation needs a unique persistent ID and compact public representation.                        |
| **Decision**  | Allocate with Redis `INCR`, reuse the number as MongoDB ID, and encode it with Base62.                     |
| **Why**       | Increment is atomic and encoding is reversible for lookups and upper-bound validation.                     |
| **Trade-off** | Redis availability and counter continuity affect creation; codes are predictable identifiers, not secrets. |
| **Result**    | Async and sync creation share one allocation scheme.                                                       |

### Decision: Direct Redirect Hash Fields

|               |                                                                                               |
| ------------- | --------------------------------------------------------------------------------------------- |
| **Problem**   | Redirect decisions need only destination and verdict, while a cache DTO adds conversion work. |
| **Decision**  | Store `longUrl` and `status` directly in a Redis Hash and reconstruct with `Verdict.valueOf`. |
| **Why**       | The shape matches redirect reads and keeps enum representation stable.                        |
| **Trade-off** | Invalidation is explicit and redirect entries have no TTL.                                    |
| **Result**    | Redirect reads avoid cache-DTO JSON conversion and fall back to MongoDB on a miss.            |

### Decision: Service-Owned Redirect Behavior

|               |                                                                                                      |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **Problem**   | Redirect controllers contained cache lookup, MongoDB fallback, model population, and page selection. |
| **Decision**  | Move that behavior into `RedirectService`; tracking publication is delegated to `AnalysisService`.   |
| **Why**       | Business/integration behavior is reusable independently of MVC entry points.                         |
| **Trade-off** | The service still knows Thymeleaf model/page names.                                                  |
| **Result**    | Endpoint paths remain stable while controller methods are smaller.                                   |

### Decision: Kafka Workflow Boundaries

|               |                                                                                     |
| ------------- | ----------------------------------------------------------------------------------- |
| **Problem**   | VirusTotal polling and click enrichment are slow and external-service dependent.    |
| **Decision**  | Use separate topics and consumer groups for creation and existing-link analysis.    |
| **Why**       | Requests can hand off slow work and return.                                         |
| **Trade-off** | Persistence is eventually consistent and not exactly-once at the application level. |
| **Result**    | Creation returns `PROCESSING`, and navigation does not wait for analytics.          |

### Decision: Atomic One-Time OTP Use

|               |                                                                     |
| ------------- | ------------------------------------------------------------------- |
| **Problem**   | Separate OTP read/compare/delete operations allow concurrent reuse. |
| **Decision**  | Compare and delete in one Redis Lua script, with a ten-minute TTL.  |
| **Why**       | Redis executes the sequence atomically.                             |
| **Trade-off** | Correctness depends on Redis; rate limiting is not implemented.     |
| **Result**    | A successful OTP is consumed in the validation operation.           |

[⬆ back to top](#table-of-contents)

---

## Reliability and Limitations

**Implemented mechanisms:** Redis atomic counter allocation · atomic OTP validation/deletion · MongoDB atomic report increments · invalid-hash upper-bound checks · VirusTotal timeout/error fallback · redirect MongoDB fallback · Kafka listener retries.

**Current limitations:**

- Kafka sends are fire-and-forget with no outbox or result handling.
- Listener retries do not cover failures occurring later inside a reactive callback.
- No DLT, replay, event idempotency, or deduplication.
- Redirect Hashes can be stale.
- Click writes are separate.
- External calls lack circuit breakers.
- Duplicate-report checks can race.
- JWTs cannot be revoked/refreshed.
- CSRF and rate limiting are disabled/not implemented.
- Brevo failures are logged and swallowed.

[⬆ back to top](#table-of-contents)

---

## Future Improvements

- Add outbox/durable workflow state, event IDs, idempotent consumers, producer-result handling, DLT, replay, and versioned contracts.
- Add bounded external-call retries, circuit breakers, health checks, metrics, tracing, and integration/concurrency tests.
- Add a unique report constraint or atomic duplicate-report workflow.
- Add redirect-entry versioning/invalidation and define counter recovery behavior.
- Add moderation/reverification processing and the missing link-owner report notification if that is part of the product contract.
- Add server-side JWT revocation/refresh policy, CSRF protection where appropriate, and OTP rate limiting.

[⬆ back to top](#table-of-contents)

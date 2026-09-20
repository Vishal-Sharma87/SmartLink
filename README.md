# SmartLink

SmartLink is a Spring Boot URL-shortening application. Authenticated users can create short links, inspect links they own, view click analytics, and delete links. A created URL is analyzed by VirusTotal before the link is persisted on the asynchronous path. Public visitors receive a verdict-specific page before the application redirects them to the original URL.

The repository contains one Spring Boot application with a Thymeleaf-rendered frontend, vanilla JavaScript, MongoDB persistence, Redis state/cache operations, and Kafka-backed asynchronous processing.

## Capabilities

- Account signup with email OTP verification.
- Login with short-lived JWT access tokens and a refresh-token cookie.
- Asynchronous link creation with VirusTotal analysis and email notification.
- Synchronous link creation endpoint for callers that wait for the scan result.
- Verdict-aware public short-link handling.
- Owner-scoped link listing, single-link lookup, deletion, and delete-all.
- Click counting and analytics for country, continent, device, browser, operating system, timezone, and recent clicks.
- OTP-verified abuse reporting with asynchronous report processing.
- Thymeleaf pages for the public site, authentication, dashboard, shortening, links, analytics, reporting, and redirect warnings.

## Architecture

```text
Browser
  |
  v
Spring Boot controllers
  |-- Thymeleaf pages and static JavaScript/CSS
  |-- REST endpoints
  |
  |-- MongoDB: users, links, scan results, analytics, abuse reports
  |-- Redis: URL counter, redirect cache, OTPs, refresh-token mappings
  |-- Kafka: link-creation and link-analysis events
  |-- VirusTotal: URL submission and analysis polling
  |-- IPinfo: click IP enrichment
  |-- Brevo-compatible email API: OTP and notification delivery
```

This is a feature-oriented monolith, not a set of independently deployable services. The application uses Spring MVC, Spring Data MongoDB, Spring Data Redis, Spring Kafka, Spring Security, WebClient, Thymeleaf, and JWT libraries. Java 21 is configured in `pom.xml`.

## Core flows

### Asynchronous link creation

`POST /link/create` requires an authenticated user and accepts a JSON `originalUrl`. The application:

1. Increments the Redis URL counter.
2. Encodes the counter with the repository's Base62 utility.
3. Builds a short URL from `COMPANY_ENDPOINT`.
4. Publishes a `LinkCreationPayload` to the configured link-creation Kafka topic.
5. Returns `202 Accepted` with a `LinkCreationResponseDto` whose status is `PENDING`.

The Kafka consumer verifies that the owner exists, submits the URL to VirusTotal, polls the returned analysis resource until it is complete, evaluates the engine statistics, and saves the `Link` in MongoDB. The consumer then sends either a normal link-created email or a malicious-link email. VirusTotal failures and polling timeouts resolve to `UNVERIFIED`.

The asynchronous request can therefore return a pending short URL before the link exists in MongoDB. A visitor may receive an error page until the consumer finishes persisting it.

### Synchronous link creation

`POST /link/create/sync` follows the same counter and Base62 allocation approach but waits for `VirusTotalService.scanUrl(...).block()`, saves the link, and returns `201 Created` with status `ACTIVE` in the response DTO. The returned `status` describes the creation response; the saved link's safety verdict is the scan result.

### Safety verdicts and public redirection

The persisted `Link.status` is one of:

`SAFE`, `SUSPICIOUS`, `UNVERIFIED`, `MALICIOUS`, or `PENDING_REVERIFICATION`.

VirusTotal verdict evaluation uses the returned harmless, malicious, undetected, and timeout counts. The current rules mark a URL malicious when any malicious or suspicious signal is present, safe when the harmless ratio is at least 50% and the timeout ratio is below 20%, unverified when the undetected ratio is above 60% or timeout ratio above 40%, and suspicious otherwise. A zero-signal result is unverified.

For a public seven-alphanumeric-character short code, `GET /{hash}` loads `originalUrl` and `status` from a Redis hash when available, otherwise loads the link from MongoDB and repopulates the cache. It returns:

| Verdict | Page returned |
| --- | --- |
| `SAFE` | `track.html`, which posts tracking data and then redirects |
| `SUSPICIOUS`, `UNVERIFIED`, `PENDING_REVERIFICATION` | `suspicious-warning.html` |
| `MALICIOUS` | `malicious-warning.html` |

The warning pages do not automatically send the visitor to the destination. The confirmation action calls `GET /api/confirm/{shortCode}`, which resolves the destination and returns the tracking page.

The redirect cache stores only `originalUrl` and `status`. It has no configured TTL; status-changing flows explicitly remove it when required.

### Click tracking and analytics

The tracking page sends `POST /api/track` with the short code, screen dimensions, user-agent, and timezone. The endpoint extracts the client IP from forwarding headers or the remote address, publishes a `LinkAnalysisPayload`, and returns `204 No Content` without waiting for analytics enrichment.

The Kafka analysis consumer increments the link click count. If the short code cannot be resolved, it drops the event. Otherwise it classifies the browser, operating system, and device from the user-agent and screen width, looks up IP information through IPinfo, and saves a `LinkInformation` document in MongoDB.

Authenticated owners can call `GET /analytics/link?shortCode=...`. The response summarizes total clicks, unique countries, top country, country/continent/device/browser/operating-system breakdowns, and up to 20 recent clicks.

### Abuse reports

The report page first calls `POST /verify/generate-otp` with an email address. The generated six-digit OTP is stored in Redis as a SHA-256/Base64 digest for ten minutes and sent through the configured email provider.

The report is submitted to `POST /report-abuse/` with reporter information, the short-link URL, one or more `ReportCause` values, an optional description, and the six-digit OTP. The service validates and consumes the OTP, checks whether that email has already reported the short code, and returns `202 Accepted` before asynchronous processing completes.

The asynchronous worker increments the link's report count atomically. At three or more reports it changes the verdict to `PENDING_REVERIFICATION` and removes the redirect cache entry. It then saves an abuse-report document and emails the reporter. Duplicate-report prevention is a query followed by asynchronous processing; it is not a database uniqueness constraint.

## Authentication and authorization

Spring Security permits the public pages, authentication routes, static assets, OTP generation, abuse-report routes, tracking routes, and seven-character public short-code GETs. Other requests require authentication. Link and analytics service methods scope owner operations by the authenticated email from the security context.

Login and signup verification return an `AuthResponse` containing `jwtToken` and `message` inside the common `ApiResponse` envelope. The frontend stores the access token in `sessionStorage` and sends it as `Authorization: Bearer ...` for protected requests.

The server also sets a `refresh_token` cookie with `HttpOnly`, `Secure`, and path `/auth/refresh-token`. `POST /auth/refresh-token` validates the refresh JWT, looks up the active token mapping in Redis, rotates the token, and returns a new access token and cookie. Redis keeps one current refresh-token mapping per email and removes the previous reverse mapping during rotation.

Password values are BCrypt-encoded before persistence. CSRF protection is disabled in `UrlSecurityConfig`; the application uses JSON endpoints and bearer authentication for its frontend requests.

## Redis responsibilities

Redis is configured as a standalone host/port connection and is used for:

- An initial URL counter seeded at `56,800,235,584`, incremented atomically with `INCR`.
- Counter-range validation before public short-code lookup.
- Redirect hashes keyed by the configured prefix plus short code, containing the original URL and status.
- Hashed OTP values with a ten-minute TTL. A Lua script compares and deletes a valid OTP atomically.
- Email-to-refresh-token and refresh-token-to-email mappings used for refresh-token rotation.

## Kafka responsibilities

The application has two configured topics and two listener groups:

| Topic configuration | Consumer | Purpose |
| --- | --- | --- |
| `KAFKA_TOPIC_LINK_CREATION` | `LinkCreationConsumer` | VirusTotal scan, link persistence, and creation email |
| `KAFKA_TOPIC_LINK_ANALYSIS` | `LinkAnalysisConsumer` | Click count, IP enrichment, classification, and analytics persistence |

The producer uses the configured key/value serializers and the consumer uses a JSON message converter. Listener errors use a fixed one-second backoff with two retries. There is no outbox, dead-letter, replay, or application-level deduplication implementation.

## MongoDB data

MongoDB is the durable store. The main document types are:

- `User`: name, email, BCrypt password, roles, creation date, and malicious-link count.
- `Link`: short code, original URL, owner email, creation time, verdict, click count, report count, and abuse reports.
- `LinkScanResponse`: VirusTotal-derived ratios, engine count, verdict, reason, original URL, and analysis time.
- `LinkInformation`: click time, short code, IP information, timezone, and device/browser/operating-system classification.
- `AbuseReport`: reporter data, short code, causes, description, status, and creation time.

MongoDB auto-index creation is enabled in `application.yml`; the link owner, short code, and analytics short code have indexes defined in the model/repository code.

## External integrations

- VirusTotal URL analysis is called through Spring WebClient using `VT_API_KEY` and `VT_ANALYSIS_URL`. Polling interval and timeout are configurable.
- IPinfo Lite enriches click events with country and continent fields using `IP_INFO_TOKEN`. Rate-limit failures return no IP information.
- Email delivery uses the configured Brevo-compatible HTTP endpoint and API key. The email service is used for signup OTPs, welcome mail, link-created notifications, malicious-link notifications, and accepted-report notifications.

## API overview

Successful JSON responses normally use:

```json
{
  "data": "endpoint-specific value",
  "timestamp": "ISO-8601 instant"
}
```

Failures use an `ApiError` containing `timestamp`, `message`, `code`, and `httpStatus`. Validation and JSON deserialization failures are mapped to the configured invalid-request error. The exact response messages come from configuration.

### Authentication

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public | Authenticate with `email` and `password`; returns JWT and sets refresh cookie |
| `POST` | `/auth/signup/initiate` | Public | Accept `firstName`, `lastName`, `email`, `password`; stores pending signup in the HTTP session and sends OTP |
| `POST` | `/auth/signup/verify` | Public session | Accept six-digit `otp`; creates the account and returns JWT plus refresh cookie |
| `POST` | `/auth/refresh-token` | Refresh cookie | Rotate the refresh token and return a new access token |

### Links

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/link/create` | JWT | Queue asynchronous creation; body is `{ "originalUrl": "..." }`; returns `202` |
| `POST` | `/link/create/sync` | JWT | Scan and persist synchronously; same request body; returns `201` |
| `GET` | `/link/` | JWT | Return the authenticated user's links |
| `GET` | `/link/{hash}` | JWT | Return one link owned by the authenticated user |
| `DELETE` | `/link/{hash}` | JWT | Delete one link owned by the authenticated user |
| `DELETE` | `/link/` | JWT | Delete all links owned by the authenticated user |
| `GET` | `/link/debug/verdict/{shortCode}` | JWT | Return the stored scan details for a short code |

`originalUrl` is required, must be a URL, and is limited to 2,048 characters. Link response records include the original URL, short code, verdict, click count, report count, and creation time.

### Analytics, tracking, and reports

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/analytics/link?shortCode=...` | JWT | Return owner-scoped analytics for a short code |
| `POST` | `/api/track` | Public | Queue click analytics; returns `204` |
| `GET` | `/api/confirm/{shortCode}` | Public | Return tracking page after a visitor confirms a warning |
| `POST` | `/verify/generate-otp` | Public | Send a six-digit OTP for an email; returns `204` |
| `POST` | `/report-abuse/` | Public | Validate an OTP and queue an abuse report; returns `202` |

The report body requires `reporterName`, `reporterEmail`, `linkToReport`, a non-empty `cause` list, and a six-digit numeric `otp`; `description` is optional and limited to 2,000 characters.

### User and public pages

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/user` or `/user/` | JWT role `USER` | Health-style authenticated user response: `Working` |
| `DELETE` | `/user/delete-user` | JWT | Delete the authenticated user |
| `GET` | `/{hash}` | Public for seven alphanumeric characters | Verdict-aware public link page |

Thymeleaf page routes include `/`, `/home`, `/about`, `/login`, `/signup`, `/signup/verify`, `/dashboard`, `/shorten`, `/links`, `/analytics`, `/report-abuse-page`, and `/report`.

There is no Swagger/OpenAPI dependency or generated API documentation in the repository.

## Frontend

The UI uses server-rendered Thymeleaf templates under `src/main/resources/templates`, shared fragments for the head, navigation, footer, and alerts, and shared CSS under `src/main/resources/static/css/style.css`.

Vanilla JavaScript handles JWT-authenticated requests, refresh-token recovery, login/signup flows, OTP/report interactions, link listing, analytics rendering, copy actions, password visibility, navigation, and the shorten-link success modal. The frontend guidance in `docs/frontend-architecture.md` and `docs/frontend-design.md` is the source of truth for its lightweight architecture and visual conventions.

## Configuration

`application.yml` imports an optional root `.env` file. Copy `.env.example` as a starting point and replace placeholders; do not commit real credentials.

Important configuration groups are:

| Group | Variables |
| --- | --- |
| Application URLs | `COMPANY_ENDPOINT`, `COMPANY_DOMAIN` |
| MongoDB | `MONGODB_URI`, `MONGODB_DATABASE` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, redirect/OTP/token key names |
| Kafka | `KAFKA_BOOTSTRAP_SERVERS`, security/SASL settings, serializers, topic names, consumer group IDs, session timeout |
| Containers | `MONGODB_IMAGE_TAG`, `MONGODB_PORT`, `REDIS_IMAGE_TAG`, `KAFKA_IMAGE_TAG`, `KAFKA_PORT`, `KAFKA_ADVERTISED_HOST` |
| JWT | `JWT_SECRET_KEY`, `JWT_EXPIRY_DURATION_MINUTES`, `JWT_REFRESH_EXPIRY_DAYS` |
| VirusTotal | `VT_API_KEY`, `VT_ANALYSIS_URL`, `VT_POLLING_INTERVAL_SECONDS`, `VT_POLLING_TIMEOUT_MINUTES` |
| IPinfo | `IP_INFO_TOKEN` |
| Email | `EMAIL_PROVIDER_BASE_URL`, `EMAIL_PROVIDER_API_KEY`, sender settings, email subjects |
| Application text | `API_RESPONSE_*`, `EXC_MSG_*`, redirection-page names, MongoDB field names, log levels |

The names and example values are listed in `.env.example`, but the example is not currently a complete one-to-one list of the placeholders in `application.yml`. Before starting the application, also provide `API_RESPONSE_REFRESH_TOKEN_ROTATED`, `LOG_LEVEL_APACHE_KAFKA`, `LOG_LEVEL_SPRING_KAFKA`, `SMART_LINK_USERS_EMAIL`, `SMART_LINK_OWNER_EMAIL`, and `SMART_LINK_SHORT_CODE`. The example also contains older field-name variables such as `SMART_LINK_ACTUAL_URL`, `SMART_LINK_HASHED_KEY`, and `SMART_LINK_OWNER_USERNAME` that are not referenced by the current `application.yml`. `COMPANY_ENDPOINT` should match the URL from which users access the application: the Docker stack exposes Nginx on port 80, while direct Maven startup uses the Spring Boot port unless changed elsewhere.

## Local development

### Prerequisites

- Java 21.
- Docker and Docker Compose for MongoDB, Redis, and Kafka, unless those services are provided separately.
- A VirusTotal API key, IPinfo token, and Brevo-compatible email API credentials for the corresponding features.

### Start dependencies

```bash
cp .env.example .env
# Replace the placeholder secrets and adjust host/URL values for the chosen run mode.
docker compose up -d mongodb redis kafka
```

### Run Spring Boot directly

```bash
./mvnw spring-boot:run
```

The application is packaged for Java 21. The frontend is served by Spring Boot; open the configured application URL, normally `http://localhost:8080` for a direct run. If the generated short-link prefix should include that port, set `COMPANY_ENDPOINT` accordingly.

### Run the full Docker stack

The compose file includes the application, MongoDB, Redis, Kafka, and Nginx services:

```bash
cp .env.example .env
# Replace the placeholder secrets and keep container service names such as mongodb, redis, and kafka.
docker compose up --build
```

Nginx listens on host ports 80 and 443 and proxies to the application on port 8080. The compose file uses named volumes for MongoDB, Redis, and Kafka data. The Nginx configuration in this repository does not define TLS certificates; the 443 port is exposed by the compose file but no TLS configuration is provided there.

`fetch_secrets.sh` is also present for repository-specific secret retrieval, but its external secret source is not defined by the application code; inspect that script and the surrounding environment before using it.

## Testing

The repository contains tests for signup behavior, JWT service behavior, email content construction, and JSON conversion. Run them with:

```bash
./mvnw test
```

The tests and the application require their configured dependencies or test-specific setup where applicable. The repository does not include a separate frontend test suite.

## Project structure

```text
src/main/java/com/spring/springboot/smartlink/
├── analytics/       analytics controller, DTOs, entities, repositories, services
├── advices/         error codes, exceptions, API and security error handlers
├── configurations/  application, MVC, security, page, and message configuration
├── email/           provider, email service, content builder, subjects, DTOs
├── geoip/           IPinfo configuration and model/service
├── jwt/             JWT configuration, filter, and services
├── kafka/           topics, connection/producer config, payloads, publisher, consumers
├── link/            link controller, DTOs, entity, repository, keys, services
├── otp/             OTP controller, DTO, and service
├── redis/           Redis connection/configuration, repositories, keys, Lua scripts, service
├── redirection/     public redirect controllers, tracking DTO, redirect service
├── report/          abuse report controller, DTO, entity, repository, keys, services
├── user/            user model, repository/service, and authentication flow
└── utils/           Base62 utility

src/main/resources/
├── application.yml
├── templates/       Thymeleaf pages and fragments
└── static/          CSS, JavaScript, and favicon
```

## Security and validation notes

- Access tokens are signed JWTs; protected requests require a bearer token.
- Refresh tokens are signed JWTs, stored in an HttpOnly/Secure cookie, and tracked in Redis for rotation.
- User passwords are BCrypt-encoded.
- Signup, login, URL creation, tracking, and abuse-report inputs use Jakarta validation constraints.
- OTP values are hashed before storage, expire after ten minutes, and are consumed atomically on successful verification.
- Public redirect routing accepts only GET paths matching seven alphanumeric characters through the security matcher.
- CSRF is disabled in the current security configuration.
- There is no application rate-limiter implementation in the repository. External provider limits, Kafka behavior, and infrastructure capacity still affect operation.

## Important implementation limitations

- Asynchronous creation and analytics depend on Kafka consumers completing successfully; the HTTP response does not represent durable completion.
- Kafka publishing has no outbox, dead-letter, replay, or deduplication workflow.
- Link-creation and analytics side effects are not idempotency-key protected.
- Redirect cache entries have no TTL and rely on explicit invalidation or cache misses.
- Abuse-report duplicate prevention is not an atomic uniqueness guarantee.
- VirusTotal errors/timeouts become `UNVERIFIED`; IPinfo rate-limit failures omit IP enrichment.
- Email provider failures are handled by logging in the email service and are not a durable notification queue.
- The refresh-token cookie is marked `Secure`, so direct local HTTP testing may require an HTTPS-capable setup or a development-specific cookie configuration.
- The full compose file exposes an HTTPS port, but the checked-in Nginx configuration only contains an HTTP listener and proxy configuration.

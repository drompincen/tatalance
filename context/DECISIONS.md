# Architecture Decisions

<!-- Format:
## [Date] Decision Title
**Context:** Why this decision was needed
**Decision:** What was decided
**Consequences:** Trade-offs accepted
-->

## Current stack (2026-06)

| Layer | As deployed |
|---|---|
| Backend | Spring Boot 3.3.5, Java 21, Maven |
| Database | MongoDB Atlas (per-env database) |
| Local/test DB | Flapdoodle embedded MongoDB |
| Frontend | Plain HTML/JS in `static/` (not React) |
| Hosting | AWS Elastic Beanstalk (branch-per-env) |
| HTTPS / CDN | AWS CloudFront — one distribution per EB env (mobile HTTPS; EB origins are HTTP-only) |
| CI/CD | GitHub Actions (OIDC deploy) |
| Auth | Spring Security form login + optional Google OAuth; static `login.html` |
| Errors | `GlobalExceptionHandler` + `ApiMessageResolver` (EN/ES via `Accept-Language`) |

Entries below marked **[SUPERSEDED]** are historical — kept for context, not current guidance.

---

## 2026-04-27 DocumentDB over RDS PostgreSQL [SUPERSEDED]
**Superseded by:** MongoDB Atlas (2026 deploy)
**Context:** Need a managed database on AWS. Project is a chauffeur dispatch tool with flexible, evolving domain objects.
**Decision:** AWS DocumentDB (MongoDB-compatible). Spring Data MongoDB. No schema migrations.
**Consequences:** No SQL joins (denormalize clientName into Job documents). Flapdoodle tests run against real MongoDB, not DocumentDB — known minor incompatibilities documented. Simpler schema evolution going forward.

## 2026-04-27 UI bundled in Spring Boot JAR [SUPERSEDED — React path]
**Superseded by:** Plain HTML/JS in `backend/src/main/resources/static/` (no frontend-maven-plugin)
**Was:** React bundle via `frontend-maven-plugin`.
**Decision (current):** Static HTML/JS served from Spring Boot JAR. Single artifact: one JAR serves API + UI.
**Consequences:** Simpler CI/CD. Same-origin API calls — no CORS in prod.

## 2026-04-27 Server-side session over JWT [PARTIAL — session model still valid]
**Context:** Need auth state management. App served from one domain.
**Decision:** Spring Security server-side session cookie (not JWT).
**Consequences:** No JWT complexity. Session invalidation is immediate.
**Note:** Early notes referenced ElastiCache/Fargate — **superseded by EB** (see below). Sessions are in-memory per EB instance; acceptable for current scale.

## 2026-04-27 Thymeleaf login page [SUPERSEDED]
**Superseded by:** Static `login.html` / `register.html` in `static/` with CSRF token from cookie/meta
**Was:** Thymeleaf template for `/login`.
**Consequences (current):** Auth pages are plain HTML + `i18n.js`; no Thymeleaf dependency for login.

## 2026-04-29 ElastiCache Redis for session store [SUPERSEDED]
**Superseded by:** 2026-04-30 ALB sticky sessions over Redis
**Was:** Spring Session Redis to survive rolling-deploy task switches.
**Why superseded:** Single admin user; low deploy frequency; Redis adds ~$20/month and significant config complexity for a problem that ALB sticky sessions solves for free.

## 2026-04-30 ALB sticky sessions over Redis [SUPERSEDED — Fargate/CDK path]
**Superseded by:** AWS Elastic Beanstalk deploy (2026). No Fargate/CDK in current pipeline.
**Was:** ALB sticky sessions on Fargate target groups.

## 2026-04-29 ProblemDetail (RFC 9457) for API errors [SUPERSEDED]
**Superseded by:** `GlobalExceptionHandler` returning `Map` with `errors` array; `ApiMessageResolver` for i18n (#106)
**Was:** `ProblemDetail` type in handler.
**Consequences (current):** Static JS parses `errors[].message`; EN/ES via `Accept-Language`.

## 2026-04-30 Bean Validation on DTOs only [PARTIAL — domain annotations in use]
**Context:** Spring Data MongoDB does NOT validate documents on save. Validation must run at controller boundary.
**Decision:** Use `@Valid @RequestBody` on controllers. Domain classes (`Client`, `Ride`) currently carry `@NotBlank` for request-body validation — effective at HTTP boundary, not on Mongo save.
**Consequences:** Do not assume MongoDB enforces constraints; always validate at API entry.

## 2026-04-30 BigDecimal → DECIMAL128 in MongoDB
**Context:** Spring Data MongoDB serializes `BigDecimal` to `String` by default, losing numeric type in the database.
**Decision:** Annotate the `price` field on `Job.java` with `@Field(targetType = FieldType.DECIMAL128)`.
**Consequences:** Price is stored as MongoDB `Decimal128` (a 128-bit decimal float). Numeric queries and sorting work correctly. Future range queries on price are possible.

## 2026-04-30 keytool CA import for DocumentDB TLS (not JVM system properties)
**Context:** DocumentDB requires TLS. The MongoDB Java driver 4.x manages its own SSL context and does not reliably use `-Djavax.net.ssl.trustStore`. Also `trustStoreType=PEM` is not a valid JVM trust store type (JVM only accepts JKS/PKCS12).
**Decision:** Import the Amazon RDS CA bundle into the JRE's default `cacerts` keystore during the Docker image build using `keytool -importcert`. The connection string only needs `?tls=true`. No JVM system properties needed.
**Consequences:** Clean connection string. No extra JVM flags. CA import is a one-time Docker build step. If the CA bundle URL is blocked in CI, the PEM file must be committed to `backend/src/main/resources/` and `COPY`'d instead.

## 2026-04-30 OAuth2 conditional on property presence
**Context:** In `demo` profile, `GOOGLE_CLIENT_ID` is not set. Spring Boot's OAuth2 auto-configuration fails to start if `client-id` is empty.
**Decision:** `SecurityConfig` inspects `spring.security.oauth2.client.registration.google.client-id` via `@Value`. Only registers `.oauth2Login()` on the `SecurityFilterChain` when the property is non-blank. In `demo` mode without Google credentials, only `formLogin()` is active. Prod requires both — app fails fast if `GOOGLE_CLIENT_ID` is absent in prod.
**Consequences:** `demo` profile works without any Google credentials. Prod is still secure — missing credentials cause startup failure, not silent fallback.

## 2026-06-21 One app, two surfaces for chauffeur + freelance (Option B)
**Context:** Luciano bills Tatalance dev work hourly (timer, pause/resume, daily invoice). David uses chauffeur ride dispatch. Need one deployable app without duplicating backend models.
**Decision:** Single Spring Boot app with two static UIs. `businessMode` on `AppUser` (`CHAUFFEUR` | `FREELANCE`). Login redirects to `/freelance.html` or `/index.html`. Freelance reuses `Ride` with `pricingMode=HOURLY`, `jobTitle`, and `workSegments` for pause/resume audit. `TimerService` handles server-side timer state; `GET /api/rides/{id}/timer` for recovery after refresh.
**Consequences:** Chauffeur UI unchanged. Freelance book flow does not auto-start timer — user explicitly presses Start. Two HTML surfaces to maintain until a unified redesign. Mockups in `docs/freelance-jobs-mockup.html`.

## 2026-06-21 EN/ES i18n for Tata (client-side toggle, mockup port)
**Context:** Tata prefers Spanish. Production UI (`index.html`) is English-only; `docs/js/i18n.js` already prototypes EN/ES for the redesign mockup. Help overlay (`?`) is English hardcoded in `helpPanels`.
**Decision:** Epic 13 (#102): port `docs/js/i18n.js` pattern to production static UI. `data-i18n` keys + `localStorage` for language. Default to Spanish when `navigator.language` starts with `es`. Translate chauffeur UI, ? help (6 panels), and auth pages. API validation in Spanish (#106) via `Accept-Language` + `ApiMessageResolver` — **shipped 2026-06-25**. Freelance surface out of scope for v1.
**Consequences:** Duplicate string maintenance in `i18n.js` until/unless extracted. No Spring MessageSource in v1.

## 2026-06-25 Spanish API errors via Accept-Language (#106)
**Context:** ES UI showed English validation text from server when only client-side i18n was translated.
**Decision:** `i18n.js` fetch wrapper sends `Accept-Language: es|en` on `/api/*`. `ApiMessageResolver` translates field messages and known `ResponseStatusException` strings.
**Consequences:** Server and UI locale stay aligned for booking errors. Add new messages to `KNOWN_ES` map when introducing user-facing API errors.

## 2026-06-21 Embedded in-app map picker over external Google Maps redirect
**Context:** Users want to pick pickup/dropoff by dropping a pin on Google Maps instead of typing addresses. Epic 9 #73 already links table text to `google.com/maps/search/...`.
**Decision:** Build an in-app `LocationPicker` modal using Google Maps JavaScript API + Places. Reverse-geocode pin/search result to human-readable text stored in existing `pickupLocation` / `dropoffLocation` strings. Optional `lat/lng` on `Ride` (#100) for precise navigation links. API key in EB env (`GOOGLE_MAPS_API_KEY`), never in git. External Google Maps tab cannot return picked coordinates to the web app.
**Consequences:** Requires Google Cloud billing (free-tier usually sufficient). Drom sets up API key (#97); Luciano builds UI (#98–#99). Freelance `freelance.html` out of scope for v1. E2E must mock Maps APIs (#101).

## 2026-06-27 Configurable invoice tax via account + profile (#116)
**Context:** MVP hardcoded 8% tax on chauffeur invoices; freelance (#115) needed $0 tax. Hardcoded `isFreelanceInvoice()` was not user-configurable.
**Decision:** `AppUser.defaultTaxRate` (decimal fraction) set via `PATCH /api/users/me/settings` as `defaultTaxRatePercent` (0–100). Optional `Profile.taxRate` override. `TaxRateResolver` priority: profile → account → legacy fallbacks (HOURLY/ENGINEER=0, else 8%). FREELANCE mode auto-sets 0% when unset.
**Consequences:** Users control tax without code changes. Legacy accounts without a saved rate keep old behavior until they save settings.

## 2026-04-30 Flapdoodle spring30x artifact for Spring Boot 3 tests
**Context:** The legacy `de.flapdoodle.embed.mongo` artifact does not auto-configure with Spring Boot 3. `@DataMongoTest` silently fails to start an embedded MongoDB, causing all repository tests to fail with connection refused.
**Decision:** Use `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x` in `test` scope.
**Consequences:** `@DataMongoTest` slices work correctly. `@SpringBootTest` also auto-configures embedded MongoDB. No test profile MongoDB URI override needed.

## 2026-07-20 CloudFront in front of each EB env for mobile HTTPS
**Context:** Elastic Beanstalk's `*.eba-*.us-east-1.elasticbeanstalk.com` domains serve **HTTP only** — there is no TLS listener, so `https://<env>.elasticbeanstalk.com` simply times out. Mobile Safari (and most real-device testing) requires HTTPS. We needed HTTPS without buying a domain or managing a certificate.
**Decision:** Put **one CloudFront distribution in front of each EB environment**. CloudFront terminates TLS with its free default `*.cloudfront.net` certificate and forwards to the EB origin. The distributions are created **manually in the console — they are NOT in the repo / IaC**.

```
Mobile browser ──HTTPS──▶ CloudFront (d……..cloudfront.net) ──HTTP:80──▶ EB nginx ──▶ Spring Boot
     (viewer)             TLS terminated here                origin (no TLS)          API + static UI
```

Distribution behavior (all three identical; verified 2026-07-20 via `aws cloudfront get-distribution-config`):
- **Viewer protocol:** `redirect-to-https` — plain `http://…cloudfront.net` is redirected up to HTTPS.
- **Origin protocol:** `http-only`, port 80 — EB has no HTTPS for CloudFront to talk to.
- **Allowed methods:** ALL (`GET/HEAD/OPTIONS/PUT/POST/PATCH/DELETE`) — the REST API needs writes.
- **Cache policy:** AWS-managed **CachingDisabled** — nothing is cached (the app is dynamic).
- **Origin request policy:** AWS-managed **AllViewer** — forwards **all** viewer headers, cookies, and query strings to the origin, including `Authorization`, the `JSESSIONID` cookie, and the CSRF token. This is why HTTP Basic, form-login sessions, and CSRF all keep working through CloudFront.

Current domains (they are not in code — look them up with `aws cloudfront list-distributions --profile drom-admin`):

| Env | HTTPS URL (open `/login.html`) | Distribution Id | Origin |
|---|---|---|---|
| drom | https://d22fckr1nry9y2.cloudfront.net | `ERMUPVT68E4VF` | tatalance-drom |
| luciano | https://d1azhf85ydpcl8.cloudfront.net | `EQ5A0IC19GWVE` | tatalance-luciano |
| prod | https://d233sbm7obwqjh.cloudfront.net | `E1U9J0OLW93VDR` | tatalance-prod |

**Consequences:**
- **`*.cloudfront.net` domains are random and assigned at creation.** Delete + recreate a distribution and the domain changes, breaking old bookmarks. This bit us on **2026-06-30**: an AWS billing lapse deleted the distributions, and recreating them produced the domains above — which is why previously-saved mobile links stopped working ("links not working after I upgraded my plan").
- **No IaC** — these distributions are undocumented "pets." Recreating one is a manual console step; when you do, update the table above and `CLAUDE.md` → Cloud Infrastructure.
- **CloudFront passes the origin's `401` through unchanged.** `/`, `/index.html`, and `/favicon.ico` return `401 WWW-Authenticate: Basic` (Spring Security `httpBasic`), and browsers auto-request `/favicon.ico` on every page — so the **native Basic-auth dialog can pop even on the public login/register pages**. Enter the app at **`/login.html`**. (See `docs/troubleshooting.md` → CloudFront / HTTPS.)
- **Durable fix (not done yet):** a custom domain (Route 53 + an ACM certificate as a CloudFront alias) gives a stable URL that survives distribution recreation.

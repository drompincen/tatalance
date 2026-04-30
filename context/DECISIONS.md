# Architecture Decisions

<!-- Format:
## [Date] Decision Title
**Context:** Why this decision was needed
**Decision:** What was decided
**Consequences:** Trade-offs accepted
-->

## 2026-04-27 DocumentDB over RDS PostgreSQL
**Context:** Need a managed database on AWS. Project is a chauffeur dispatch tool with flexible, evolving domain objects.
**Decision:** AWS DocumentDB (MongoDB-compatible). Spring Data MongoDB. No schema migrations.
**Consequences:** No SQL joins (denormalize clientName into Job documents). Flapdoodle tests run against real MongoDB, not DocumentDB — known minor incompatibilities documented. Simpler schema evolution going forward.

## 2026-04-27 UI bundled in Spring Boot JAR
**Context:** Need to deploy the React frontend. Options were S3+CloudFront or bundling in the JAR.
**Decision:** Bundle via `frontend-maven-plugin` + Spring static resources. Single artifact: one JAR serves both API and UI.
**Consequences:** Simpler CI/CD (one build, one deploy). Same-origin API calls — no CORS in prod. Slightly larger JAR. Build is slower when UI changes (mitigated by Maven dep caching layer in Docker).

## 2026-04-27 Server-side session over JWT
**Context:** Need auth state management. App is a single-admin tool served from one domain.
**Decision:** Spring Security server-side session cookie. Session stored in ElastiCache Redis in prod so all Fargate tasks share state.
**Consequences:** No JWT complexity. Redis adds one infrastructure dependency. Session invalidation is immediate (no token expiry race). Requires sticky sessions to be off and Redis to be available.

## 2026-04-27 Thymeleaf login page (not a React route)
**Context:** Demo login uses `POST /login` (Spring Security form login), which requires a CSRF token. React routes can't easily inject CSRF tokens into a form without backend involvement.
**Decision:** Login page is a Thymeleaf template. Spring Security injects the CSRF token automatically via `th:action`. The SPA catch-all controller explicitly excludes `/login`.
**Consequences:** Login page is not part of the React bundle. Requires `spring-boot-starter-thymeleaf` dependency. Login page styled separately (inline CSS or non-bundled stylesheet).

## 2026-04-29 ElastiCache Redis for session store
**Context:** ECS Fargate rolling deploys (min 100%, max 200%) can run two tasks simultaneously. In-memory Spring sessions would be lost on task switches.
**Decision:** Spring Session with Redis backend (`spring-session-data-redis`). In-memory only in dev profile.
**Consequences:** Additional AWS resource (ElastiCache, ~$15-25/month for t3.micro). Session continuity across task restarts. All tasks share session state — no sticky sessions needed on ALB.

## 2026-04-29 ProblemDetail (RFC 9457) for API errors
**Context:** Need consistent JSON error responses. Spring Boot 3 has built-in support.
**Decision:** `@ControllerAdvice GlobalExceptionHandler` using `ProblemDetail`. Validation errors map to 400 with `errors` array. Unknown entities return 404. Unhandled exceptions return 500 with no stack trace.
**Consequences:** Standardized contract for the React UI to parse. No custom error DTOs needed — framework type is sufficient.

## 2026-04-30 Bean Validation on DTOs only, not on domain documents
**Context:** Spring Data MongoDB does NOT run Bean Validation before saving documents (unlike JPA). Putting `@NotBlank` on `Client.java` or `Job.java` is dead code that gives false confidence.
**Decision:** All `@NotBlank` / `@NotNull` annotations live only on `CreateClientRequest` / `CreateJobRequest` DTOs. `@Valid` on the `@RequestBody` parameter in the controller enforces them. Domain documents have no validation annotations.
**Consequences:** Clear separation — the controller boundary is the only validation point. Services trust their inputs are valid. Domain classes stay clean.

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

## 2026-04-30 Flapdoodle spring30x artifact for Spring Boot 3 tests
**Context:** The legacy `de.flapdoodle.embed.mongo` artifact does not auto-configure with Spring Boot 3. `@DataMongoTest` silently fails to start an embedded MongoDB, causing all repository tests to fail with connection refused.
**Decision:** Use `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x` in `test` scope.
**Consequences:** `@DataMongoTest` slices work correctly. `@SpringBootTest` also auto-configures embedded MongoDB. No test profile MongoDB URI override needed.

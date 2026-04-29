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

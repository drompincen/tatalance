---
title: Tatalance v1 — Client, Job/Drive, Auth, CI/CD
status: pending
created: 2026-04-27
updated: 2026-04-27
current_chapter: 0
---

# Plan: Tatalance v1 — Full Stack Build

Deliver the first two user stories (Add Client, Add Job/Drive) on a real Java backend + React UI,
with Google OAuth2 sign-in and AWS CI/CD deployment from GitHub.

Journey delivered: **Add Client → Add Job/Drive → View Job/Drive in List**

### Architecture overview
```
Internet → ALB → ECS Fargate (Spring Boot JAR — serves API + bundled React UI)
                            → DocumentDB (MongoDB-compatible, private subnet)
```
Single deployable artifact: the Spring Boot JAR contains the compiled React app as static resources.
No S3, no CloudFront, no separate frontend deployment step.

---

## Chapter 0: UI Reorganization — Mock to Structured Project
**Status:** pending

### Goal
Move the existing vanilla-JS mock (`docs/`) into a proper React + Vite project at `ui/`.
The mock becomes a reference; the new `ui/` is the real product.

### Steps
- [ ] Create `ui/` directory with Vite + React 18 scaffold (`npm create vite@latest ui -- --template react`)
- [ ] Install dev dependencies: `vitest`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`
- [ ] Configure Vite proxy: `/api` → `http://localhost:8080` (dev only — avoids CORS during development)
- [ ] Copy `docs/css/styles.css` → `ui/src/styles/` as baseline styles
- [ ] Port `docs/js/i18n.js` → `ui/src/i18n/` as ES module with `useTranslation` hook
- [ ] Create `ui/src/components/layout/` — Sidebar, Topbar, AppLayout components
- [ ] Create `ui/src/views/` — one file per view (Dashboard, Clients, Jobs, Drivers, etc.)
- [ ] Create `ui/src/services/api.js` — thin axios wrapper (relative URLs — works for both dev proxy and bundled prod)
- [ ] Create `ui/src/store/` — simple React Context for app state (clients list, jobs list, auth user)
- [ ] Stub out mock data layer in `ui/src/mocks/` to keep the UI runnable before backend is ready
- [ ] Verify `npm run dev` renders the same views as the original mock
- [ ] Add `ui/node_modules`, `ui/dist` to root `.gitignore`

### Outcome
`ui/` is a proper Vite/React project. All existing views render. Backend calls use relative URLs —
so in dev the Vite proxy forwards them to Spring Boot, and in prod they hit the same origin.

---

## Chapter 1: Backend Scaffolding
**Status:** pending

### Goal
Create a Spring Boot 3 project wired to DocumentDB (MongoDB), able to serve static resources,
with a local dev setup using a MongoDB Docker container.

### Steps
- [ ] Create `backend/` using Spring Initializr: Java 21, Spring Boot 3.3, Maven
  - Dependencies: Spring Web, Spring Data MongoDB, Spring Security, OAuth2 Client, Validation, Lombok, Spring Boot Actuator
  - No Flyway, no JPA, no H2 — DocumentDB is schemaless
- [ ] Add `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x` in `test` scope — this is the Spring Boot 3-specific artifact; the old `de.flapdoodle.embed.mongo` artifact does NOT auto-configure with Spring Boot 3 and will cause all `@DataMongoTest` tests to fail with a connection error
- [ ] Set up project packages under root `com.tatalance`: `domain`, `application`, `infrastructure.web`, `infrastructure.persistence`, `config`
- [ ] Create `docker-compose.yml` at repo root — `mongo:6` service on port 27017 for local dev
- [ ] Create `.env.example` at repo root documenting all required env vars:
  ```
  # Local dev (default profile — no Google credentials needed with demo profile)
  MONGODB_URI=mongodb://localhost:27017/tatalance
  SPRING_PROFILES_ACTIVE=demo

  # Production (all required — app fails to start if missing)
  # MONGODB_URI=mongodb://user:pass@docdb-host:27017/tatalance?tls=true
  # GOOGLE_CLIENT_ID=
  # GOOGLE_CLIENT_SECRET=
  # ALLOWED_EMAILS=david@example.com
  # SPRING_PROFILES_ACTIVE=prod
  ```
- [ ] Create `backend/src/main/resources/application.yml`:
  ```yaml
  spring:
    data:
      mongodb:
        uri: ${MONGODB_URI:mongodb://localhost:27017/tatalance}
        auto-index-creation: true   # dev only; disabled in prod profile
    jackson:
      serialization:
        write-dates-as-timestamps: false   # Instant → ISO 8601 string, not Unix nanos
  server:
    servlet:
      session:
        cookie:
          http-only: true
          secure: false  # overridden to true in prod profile
          same-site: strict
  management:
    endpoints:
      web:
        exposure:
          include: health, info
    endpoint:
      health:
        show-details: when-authorized
  ```
- [ ] Create `backend/src/main/resources/application-prod.yml`:
  ```yaml
  server.servlet.session.cookie.secure: true
  spring:
    data:
      mongodb:
        auto-index-creation: false  # never auto-create indexes in prod
  ```
- [ ] Create `backend/src/main/resources/application-test.yml` — override MongoDB URI to flapdoodle embedded port (handled automatically by `@DataMongoTest`)
- [ ] Configure CORS in `SecurityConfig` **only when `dev` profile is active** — permits `http://localhost:5173`; prod profile has no CORS config (same-origin, not needed)
- [ ] Add SPA catch-all controller: `@Controller` with two mappings to cover all React Router paths:
  - `@RequestMapping("/{path:[^\\.]*}")` — matches single segments like `/clients`, `/jobs`
  - `@RequestMapping("/**/{path:[^\\.]*}")` — matches deep segments like `/clients/abc/jobs`
  - Both return `forward:/index.html`
  - Spring MVC picks more specific `@GetMapping("/login")` and `/api/**` routes first; Security filter chain intercepts `/oauth2/**` and `/login/oauth2/**` before MVC — the catch-all never fires for those
- [ ] Configure Spring Boot to serve static resources from `classpath:/static/`
- [ ] Add **deep health-check** endpoint `GET /api/health` — pings MongoDB (issue a `ping` command via `MongoTemplate`); returns `{ status: "ok", db: "ok" }` on success, `503` if DB unreachable
- [ ] Add global exception handler: `@ControllerAdvice GlobalExceptionHandler` using Spring Boot 3's `ProblemDetail`:
  - `MethodArgumentNotValidException` → 400, body `{ type, title, status, errors: [{field, message}] }`
  - `NotFoundException` (custom) → 404
  - Unhandled `Exception` → 500 (no stack trace in response body)
- [ ] Write `HealthControllerTest` (`@WebMvcTest`, mock `MongoTemplate`) — 200 when DB ok, 503 when DB throws
- [ ] Write `GlobalExceptionHandlerTest` — 400 with field errors, 404, 500
- [ ] Add to `application.yml`: `spring.threads.virtual.enabled: true` — enables Java 21 virtual threads for all request handling threads; one-line opt-in, significant throughput gain for MongoDB I/O
- [ ] Add `frontend-maven-plugin` skip configuration to `pom.xml`:
  ```xml
  <configuration>
    <skip>${skipFrontend}</skip>
  </configuration>
  ```
  with `<skipFrontend>false</skipFrontend>` as default property — makes `-DskipFrontend=true` actually work in CI
- [ ] Verify `mvn test` passes and `mvn spring-boot:run` starts cleanly (with `docker-compose up -d`)

### Notes
- DocumentDB in AWS is MongoDB 5.0-compatible. Use Spring Data MongoDB — the driver is compatible.
- **DocumentDB incompatibilities to watch:** `$expr` in queries, `$lookup` (no joins anyway), `listCollections` filter. None affect v1's simple CRUD, but note for future features.
- No schema migrations needed — documents are created on first write.
- Index creation: use `@Indexed` and `@CompoundIndex` on `@Document` classes; Spring Data MongoDB
  creates them on startup when `spring.data.mongodb.auto-index-creation=true` (dev only; manage
  indexes manually in prod via DocumentDB console or a startup `CommandLineRunner`).
- Flapdoodle runs real MongoDB (not DocumentDB) — tests pass against MongoDB. If a feature fails only on DocumentDB, it will only surface in prod. Known incompatibilities are minor for v1's use of simple find/save operations.

### Outcome
`backend/` builds, tests pass, app starts with local MongoDB, health endpoint probes DB, errors return structured ProblemDetail JSON.

---

## Chapter 2: Story 1 — Add Client (TDD, Backend)
**Status:** pending

### Goal
Full TDD cycle for the Client domain. Red → Green → Refactor for each layer.

### Domain model (MongoDB document)
```
Client { _id (ObjectId), name (required), phone (required), email, notes, createdAt }
```

### Test annotation guide for this chapter
- **`@DataMongoTest`** — slice test, loads only MongoDB beans (repositories). Use for repository tests. Fast.
- **`@WebMvcTest(ClientController.class)`** — slice test, loads web + security layers only, service is mocked with `@MockBean`. Use for controller tests. Add `@WithMockUser` on tests that expect 2xx responses (security is active in this slice).
- **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** — full context with flapdoodle embedded MongoDB. Use for end-to-end integration tests. Slower; run fewer.

### Steps
- [ ] **Document** `Client.java`:
  - `@Document(collection="clients")`, `@Id String id`, `Instant createdAt`
  - **NO Bean Validation annotations** — Spring Data MongoDB does not enforce `@NotBlank` on documents before save; validation belongs on the DTO, not the domain object
- [ ] **Repository** `ClientRepository extends MongoRepository<Client, String>`
- [ ] **DTOs**: `CreateClientRequest` with `@NotBlank String name`, `@NotBlank String phone`, `String email`, `String notes`; `ClientResponse`
- [ ] **[RED]** Write `ClientServiceTest` (unit, no DB, mock repository):
  - create with valid data → returns ClientResponse with id
  - list → returns all clients (service delegates to repository, tests the mapping)
  - Note: validation is NOT tested here — it fires at the controller layer, not the service
- [ ] **[GREEN]** Implement `ClientService` — `create(CreateClientRequest)`, `list()`
- [ ] **[RED]** Write `ClientControllerTest` (`@WebMvcTest`, `@MockBean ClientService`, `@WithMockUser` on authenticated tests):
  - `@WithMockUser` — `POST /api/clients` with valid body → 201 + ClientResponse
  - `@WithMockUser` — `POST /api/clients` with missing name → 400 + field error `name`
  - `@WithMockUser` — `POST /api/clients` with missing phone → 400 + field error `phone`
  - `@WithMockUser` — `GET /api/clients` → 200 + array
  - (401 test belongs in the security integration test, not here)
- [ ] **[GREEN]** Implement `ClientController` — `POST /api/clients` with `@Valid @RequestBody CreateClientRequest`, `GET /api/clients`, `GET /api/clients/{id}`
- [ ] **Integration test** `ClientIntegrationTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, flapdoodle auto-configured, `@WithMockUser` or `TestRestTemplate` with session):
  - POST → verify 201 → GET list → verify appears → GET by id → verify fields
  - GET non-existent id → 404
- [ ] Run `mvn test` — all green

### Acceptance criteria covered
- Missing name → 400 `{ field: "name", message: "name is required" }`
- Missing phone → 400 `{ field: "phone", message: "phone is required" }`
- Valid save → 201 + `ClientResponse` with generated id
- List → 200 + array
- `GET /api/clients/{id}` with unknown id → 404

---

## Chapter 3: Story 1 — Add Client (UI + Integration)
**Status:** pending

### Goal
Wire the React client form to the real backend. Replace mock data with API calls.

### Steps
- [ ] **[RED]** Write `ClientForm.test.jsx`:
  - renders name + phone fields (required), email + notes (optional)
  - shows inline error when submitted with empty name
  - shows inline error when submitted with empty phone
  - calls `onSave` with `{ name, phone, email, notes }` on valid submit
- [ ] **[GREEN]** Implement `ClientForm` component
- [ ] **[RED]** Write `ClientList.test.jsx`:
  - renders one row per client with name, phone, email
  - "Add Client" button is present
  - new client appears after add (via prop update)
- [ ] **[GREEN]** Implement `ClientList` component
- [ ] **[RED]** Write `clientService.test.js` (mock axios):
  - `createClient(data)` → `POST /api/clients`
  - `listClients()` → `GET /api/clients`
- [ ] **[GREEN]** Implement `clientService.js`
- [ ] Wire `ClientsView` — load list on mount, modal on Add Client click, refresh after save
- [ ] **Post-save hook**: after saving a client the view shows an "Add Job for [name]" CTA button
- [ ] Manual smoke test: Add Client → appears in list → CTA navigates to job form with client pre-selected

### Outcome
David clicks Add Client, fills name + phone, saves, sees the new client in the list.

---

## Chapter 4: Story 2 — Add Job/Drive (TDD, Backend)
**Status:** pending

### Goal
Full TDD cycle for the Job domain. A Job references a Client by id.

### Domain model (MongoDB document)
```
Job {
  _id (ObjectId),
  clientId (String, required),        -- references Client._id
  clientName (String),                -- denormalized for fast list reads
  pickupDateTime (Instant, required),
  pickupLocation (String, required),
  dropoffLocation (String, required),
  price (BigDecimal),
  notes (String),
  status (enum: BOOKED),              -- default BOOKED, only value in v1
  createdAt (Instant)
}
```

Denormalize `clientName` on write — avoids a join on list queries (MongoDB has no joins).
On `POST /api/jobs`, the service looks up the client, copies `client.name` into `job.clientName`.

### Steps
- [ ] **Document** `Job.java`:
  - `@Document(collection="jobs")`, `@Id String id`, `Instant createdAt`, `JobStatus status`
  - `@Indexed String clientId` — explicit index on this field; `findByClientId` does a full scan without it
  - `@Field(targetType = FieldType.DECIMAL128) BigDecimal price` — without this, MongoDB stores `BigDecimal` as a String, breaking any future numeric queries; `DECIMAL128` preserves precision and stores as a proper numeric type
  - **NO `@NotBlank` / `@NotNull` on the document** — same rule as `Client.java`; validation is on the DTO
- [ ] **Enum** `JobStatus { BOOKED }`
- [ ] **Repository** `JobRepository extends MongoRepository<Job, String>` — add `findByClientId(String)`
- [ ] **DTOs**: `CreateJobRequest` with `@NotBlank String clientId`, `@NotNull Instant pickupDateTime`, `@NotBlank String pickupLocation`, `@NotBlank String dropoffLocation`, `BigDecimal price`, `String notes`; `JobResponse` (includes `clientName`)
- [ ] **[RED]** Write `JobServiceTest` (unit, mock repository and `ClientRepository`):
  - create with valid data → returns JobResponse with status BOOKED and clientName populated
  - create with unknown clientId → throws `ClientNotFoundException` (service explicitly checks; this is business logic, not validation)
  - list all → returns list
  - list by client → returns only that client's jobs
  - Note: DTO field-level validation (missing pickupDateTime etc.) is tested in `JobControllerTest`, not here
- [ ] **[GREEN]** Implement `JobService` — `create(CreateJobRequest)`, `list()`, `listByClient(String clientId)`
- [ ] **[RED]** Write `JobControllerTest` (`@WebMvcTest`, `@MockBean JobService`, `@WithMockUser`):
  - `@WithMockUser` — `POST /api/jobs` valid → 201, status=BOOKED
  - `@WithMockUser` — `POST /api/jobs` missing `clientId` → 400 with field error
  - `@WithMockUser` — `POST /api/jobs` missing `pickupDateTime` → 400
  - `@WithMockUser` — `POST /api/jobs` missing `pickupLocation` → 400
  - `@WithMockUser` — `POST /api/jobs` missing `dropoffLocation` → 400
  - `@WithMockUser` — `GET /api/jobs` → 200 + array
  - `@WithMockUser` — `GET /api/clients/{id}/jobs` → 200 + filtered array
- [ ] **[GREEN]** Implement `JobController`
- [ ] **Integration test** `JobIntegrationTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`):
  - insert client → POST job for that client → GET list → verify clientName denormalized → verify status BOOKED
  - POST job with non-existent clientId → 404
- [ ] Run `mvn test` — all green

---

## Chapter 5: Story 2 — Add Job/Drive (UI + Integration)
**Status:** pending

### Goal
Wire the React job form to the real backend.

### Steps
- [ ] **[RED]** Write `JobForm.test.jsx`:
  - client dropdown populated with available clients
  - shows errors for missing pickupDateTime, pickupLocation, dropoffLocation
  - calls `onSave` with correct payload on valid submit
  - price + notes are optional (no error when blank)
- [ ] **[GREEN]** Implement `JobForm` component
- [ ] **[RED]** Write `JobList.test.jsx`:
  - renders client name, formatted date, pickup → dropoff, status badge (BOOKED)
  - new job appears after save
- [ ] **[GREEN]** Implement `JobList` component
- [ ] **[RED]** Write `jobService.test.js`: `createJob`, `listJobs`, `listJobsByClient`
- [ ] **[GREEN]** Implement `jobService.js`
- [ ] Wire `JobsView` — load on mount, Add Job opens form, refresh after save
- [ ] Pre-select client in `JobForm` when navigating from "Add Job for [name]" CTA
- [ ] Manual smoke test: Add Client → Add Job → job appears in Jobs list with status BOOKED

### Outcome
Full first journey complete: Add Client → Add Job/Drive → View in List.

---

## Chapter 6: Google OAuth2 Sign-In
**Status:** pending

### Goal
Protect the app behind Google Sign-In. Only authenticated users can access the API and UI.

### Architecture decision
Spring Security OAuth2 Login handles the entire OAuth flow server-side and issues a session cookie.
The React UI (now served by the same origin) calls `GET /api/me` on load; if it gets a 401,
it redirects to `/oauth2/authorization/google`. After Google callback, Spring redirects to `/`.
No JWT, no token handling in the frontend — just a session cookie from the same domain.

### Steps

#### Backend
- [ ] Configure `SecurityConfig`:
  - Permit: `GET /api/health`, `GET /oauth2/**`, `GET /login**`, all static assets (`/assets/**`, `/index.html`, `/`)
  - Require auth: all other `GET /api/**`, `POST /api/**`
  - Enable both `formLogin()` (demo) and `oauth2Login()` (Google) — Spring Security supports both simultaneously
  - Success URL `/` for both paths; failure URL `/login?error`
  - Logout: `POST /logout` clears session, redirects to `/`
  - Disable CSRF for `/api/**` (API calls from SPA use same-origin cookies — CSRF mitigated by SameSite cookie)
- [ ] Add Google OAuth2 properties to `application.yml` as empty defaults:
  ```yaml
  spring.security.oauth2.client.registration.google:
    client-id: ${GOOGLE_CLIENT_ID:}
    client-secret: ${GOOGLE_CLIENT_SECRET:}
  ```
  Spring Boot's OAuth2 auto-config will **fail to start** if `client-id` is empty. Guard the `oauth2Login()` call in `SecurityConfig` with `@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.google.client-id", matchIfMissing = false)` on the OAuth2 `SecurityFilterChain` bean — or restructure `SecurityConfig` to only call `.oauth2Login()` when the property is non-blank (check via `@Value`). In `demo` profile without Google credentials, only `formLogin()` is registered. In prod, both are required — app fails fast if `GOOGLE_CLIENT_ID` is missing.
- [ ] Add `OAuth2UserService` to restrict access by email:
  - Load `ALLOWED_EMAILS` from env (comma-separated)
  - If authenticated email not in list → throw `OAuth2AuthenticationException` → `/login?error=unauthorized`
- [ ] Add **demo login** `UserDetailsService` bean:
  - Fixed credentials: username `tata`, password `tata` (BCrypt-hashed in config — never plain text in code)
  - Returns a `UserDetails` with role `ROLE_USER` and display name "Demo"
  - Enabled only when Spring profile `demo` is active — inactive in production by default
  - `application-demo.yml` sets `demo.user.enabled=true`; prod profile leaves it false
- [ ] **CSRF on the demo login form**: `POST /login` is subject to CSRF. Add Thymeleaf (`spring-boot-starter-thymeleaf`) for the login page so Spring Security injects the CSRF token into the form automatically. The login page is a Thymeleaf template at `templates/login.html`, not a React route — it sits outside the SPA. `SpaController` must explicitly exclude `/login` from forwarding.
- [ ] Add `GET /api/me` → `{ name, email, picture }` — works for both auth paths:
  - Google: reads from `OAuth2AuthenticationToken`
  - Demo: reads from `UsernamePasswordAuthenticationToken`, returns `{ name: "Demo", email: "demo@local", picture: null }`
- [ ] **Register OAuth redirect URIs in Google Cloud Console** (one-time manual step, required before any OAuth flow works):
  - Dev: `http://localhost:8080/login/oauth2/code/google`
  - Prod: `https://{prod-domain}/login/oauth2/code/google`
  - Add both to the Google OAuth app's "Authorised redirect URIs" list
- [ ] Write `AuthControllerTest`:
  - `/api/me` unauthenticated → 401
  - `/api/me` with mock `OAuth2AuthenticationToken` → 200 + Google user info
  - `/api/me` with mock `UsernamePasswordAuthenticationToken` (demo) → 200 + `{ name: "Demo" }`
- [ ] Write security integration test: unauthenticated `GET /api/clients` → 401

#### Frontend
- [ ] Create `AuthContext` — `currentUser` state, loaded via `GET /api/me`
- [ ] `AuthGuard`: on 401, redirect to `/login`
- [ ] Login page is a **Thymeleaf template** (`templates/login.html`) served by Spring Boot at `GET /login`:
  - "Sign in with Google" button → `/oauth2/authorization/google`
  - Username / password form → `POST /login` with CSRF token (injected by Thymeleaf `th:action`)
  - Error message shown when `?error` is in the URL
  - Styled to match the app (inline CSS or a separate non-bundled stylesheet)
- [ ] Show user name in Topbar (replaces hardcoded "David"); no avatar if demo user (`picture` is null)
- [ ] Sign Out button → `POST /logout` then reload
- [ ] Wrap `AppLayout` in `AuthGuard`

### Outcome
- **Demo mode** (profile `demo`): navigate to `/login`, enter `tata` / `tata`, full app access. No Google credentials needed.
- **Production**: Google Sign-In only. Demo `UserDetailsService` bean is not loaded.
- Both paths share the same session mechanism and `/api/me` contract.

---

## Chapter 7: UI Bundled into Spring Boot JAR
**Status:** pending

### Goal
The Maven build compiles the React app and copies it into `backend/src/main/resources/static/`
so the final JAR serves both the API and the UI from the same origin.

### How it works
1. `frontend-maven-plugin` runs `npm install` + `npm run build` in `../ui` during the Maven `generate-resources` phase
2. `maven-resources-plugin` copies `ui/dist/**` → `backend/target/classes/static/`
3. Spring Boot serves `classpath:/static/` automatically — `/assets/...`, `/index.html`, etc.
4. `SpaController` (`@RequestMapping("/{path:[^\\.]*}")`) forwards unmatched routes to `/index.html` so React Router works
5. API calls from the React app use relative paths (`/api/...`) — same origin, no CORS needed in prod

### Steps
- [ ] Add `frontend-maven-plugin` to `backend/pom.xml`:
  - `workingDirectory`: `../ui`
  - Executions: `install-node-and-npm` (Node 20 LTS), `npm install`, `npm run build`
  - Phase: `generate-resources`
- [ ] Add `maven-resources-plugin` execution to copy `../ui/dist` → `${project.build.outputDirectory}/static`
- [ ] Implement `SpaController.java` — catch-all forwarding `/{path:[^\\.]*}` to `index.html`
- [ ] Verify `mvn package` produces a JAR where `java -jar` serves both `GET /` (React app) and `GET /api/health`
- [ ] Add `ui/dist/` and `ui/node_modules/` to `.gitignore`
- [ ] Local dev workflow is unchanged: run `npm run dev` in `ui/` (Vite proxy to `:8080`) OR run the packaged JAR directly

### Notes
- `SpaController` must NOT forward requests that match `/api/**`, `/oauth2/**`, `/login`, `/logout`, or `/assets/**`
  (static files are served by Spring's resource handler before the controller is reached, so assets are fine)
- During `mvn test`, the UI build is skipped via `-DskipFrontend=true` profile to keep CI fast —
  the UI has its own test job that runs in parallel

---

## Chapter 8: CI/CD & AWS Deployment
**Status:** pending

### Goal
Every push to `main` builds, tests, and deploys to AWS. PRs run tests only.

### AWS Architecture (simplified — single deployable unit)
```
Internet
    │
    ▼
  ALB (HTTPS, ACM cert)
    │
    ▼
ECS Fargate (Spring Boot JAR — serves API + bundled React UI)
    │
    ▼
DocumentDB cluster (MongoDB-compatible, private subnet, Secrets Manager)
```

No S3. No CloudFront. One service, one artifact.

### Infrastructure as Code: AWS CDK (TypeScript)
Directory: `infrastructure/`

### Steps

#### Infrastructure (CDK) — pre-requisites (one-time, manual)
- [ ] **CDK bootstrap**: `npx cdk bootstrap aws://{account-id}/{region}` — required once before any `cdk deploy`
- [ ] **Request ACM certificate** in AWS Console (or CDK `Certificate` construct with DNS validation):
  - Domain: `{prod-domain}` (e.g. `tatalance.example.com`)
  - Validation: DNS (add CNAME to your DNS registrar or Route 53)
  - Certificate must be in the **same region** as the ALB
  - Note: cert ARN is needed in `BackendStack` — either hardcode after issue or use a CDK lookup
- [ ] **Google Cloud Console**: register both redirect URIs (see Chapter 6) before first OAuth test

#### Infrastructure (CDK) — stacks
- [ ] `npx cdk init app --language typescript` in `infrastructure/`
- [ ] `VpcStack` — VPC, 2 AZs, public + private subnets, NAT gateway (single AZ for cost)
  - Security groups: `albSg` (0.0.0.0/0 → 443, 80), `ecsSg` (albSg → 8080), `dbSg` (ecsSg → 27017)
- [ ] `DatabaseStack` — DocumentDB cluster (db.t3.medium, 1 instance for v1), private subnet group,
  Secrets Manager secret for `{ username, password, host, port }`, security group `dbSg`
  - Bundle Amazon RDS CA bundle (`rds-combined-ca-bundle.pem`) in Docker image (see Dockerfile section)
- [ ] `BackendStack`:
  - ECR repository for Spring Boot image
  - ECS Cluster (Fargate)
  - **Task execution role**: grants `ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `secretsmanager:GetSecretValue`, `logs:CreateLogStream`, `logs:PutLogEvents`
  - **Task role**: grants `secretsmanager:GetSecretValue` for DB + Redis secrets (runtime access)
  - Task definition: 512 CPU / 1024 MB, image from ECR, env vars from Secrets Manager
  - Fargate service: desired 1 task, rolling deploy (min 100%, max 200%)
  - ALB (internet-facing), HTTPS listener (ACM cert ARN from pre-requisites), HTTP → HTTPS redirect
  - ALB target group: port 8080, health check `GET /api/health`, **sticky sessions enabled** (`stickinessCookieDuration: Duration.days(1)`) — the free ALB `AWSALB` cookie pins David's requests to the same task, so in-memory sessions survive the ~30s deploy window when two tasks briefly overlap. No Redis needed.
- [ ] `cdk deploy --all` to a fresh account — verify stacks create cleanly

#### GitHub Actions — CI (`.github/workflows/ci.yml`)
Triggers: push to any branch, PR to `main`
- [ ] Job `test-backend`: `mvn verify -DskipFrontend=true` — unit + integration tests with flapdoodle embedded MongoDB
- [ ] Job `test-ui`: `cd ui && npm ci && npm run test -- --run` — Vitest
- [ ] Both jobs run in parallel

#### GitHub Actions — CD (`.github/workflows/deploy.yml`)
Triggers: push to `main` only, requires CI to pass
- [ ] Job `build-and-deploy`:
  1. `mvn package` — runs `npm install` + `npm run build` via frontend-maven-plugin, then packages JAR
  2. Build Docker image (multi-stage: Maven build already done, just copy JAR)
  3. Push image to ECR with tag `${{ github.sha }}`
  4. Update ECS service: `aws ecs update-service --force-new-deployment`
  5. Wait for service stability: `aws ecs wait services-stable`
- [ ] GitHub Secrets required: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_EMAILS`

#### Dockerfile (multi-stage, with dependency and npm layer caching)
```dockerfile
# Stage 1a: resolve Maven dependencies (re-runs only when pom.xml changes)
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /app
COPY backend/pom.xml backend/pom.xml
RUN cd backend && mvn dependency:go-offline -DskipFrontend=true -B

# Stage 1b: resolve npm dependencies (re-runs only when package-lock.json changes)
FROM node:20-alpine AS npm-deps
WORKDIR /ui
COPY ui/package.json ui/package-lock.json ./
RUN npm ci

# Stage 2: full build
FROM deps AS build
COPY backend/ backend/
COPY ui/ ui/
COPY --from=npm-deps /ui/node_modules ui/node_modules
# Skip npm install — already done; skip frontend-maven-plugin's install-node phase
RUN cd backend && mvn package -DskipTests -B

# Stage 3: run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Import Amazon RDS CA into the JRE default cacerts — the MongoDB Java driver uses
# the JRE trust store for TLS. Using -Djavax.net.ssl.trustStoreType=PEM is NOT valid
# (JVM only accepts JKS/PKCS12); keytool import is the correct approach.
RUN apk add --no-cache wget && \
    wget -q -O /tmp/rds-ca.pem \
      https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem && \
    keytool -importcert -noprompt -alias rds-ca \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -storepass changeit \
      -file /tmp/rds-ca.pem && \
    rm /tmp/rds-ca.pem
COPY --from=build /app/backend/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
MongoDB connection string only needs `?tls=true` — no extra JVM flags. The `keytool` import puts the Amazon CA into the JRE's default trust store which the MongoDB Java driver uses automatically.

#### Environment management
- [ ] `application-prod.yml` — all secrets read from env vars, no defaults (see Chapter 1 for full content)
- [ ] DocumentDB URI: `mongodb://{user}:{pass}@{host}:{port}/tatalance?tls=true` — `tls=true` is sufficient because the Amazon CA is already imported into the JRE cacerts in the Docker image
- [ ] ECS task definition env vars (set in `BackendStack` CDK, sourced from Secrets Manager):
  - `MONGODB_URI` — full DocumentDB connection string
  - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_EMAILS`
  - **`SPRING_PROFILES_ACTIVE=prod`** — activates `application-prod.yml` (secure cookies, no auto-index, no CORS). MUST be set or the app runs in default profile with insecure cookies in production.

### Outcome
- PR → tests run in parallel (backend + UI), no deploy
- Merge to `main` → CI → full Maven build (includes UI) → Docker image → ECR → ECS rolling deploy
- App live on ALB DNS / custom domain, protected by Google Sign-In

---

## Chapter 9: Operational Readiness
**Status:** pending

### Goal
The app is running in production. This chapter covers the operational concerns that make it
observable, debuggable, and demo-ready — things that aren't features but are required for
the app to be trustworthy once deployed.

### Structured logging (CloudWatch-friendly)
- [ ] Add `logback-spring.xml` to `backend/src/main/resources/`:
  - Profile `prod`: JSON format (using `logstash-logback-encoder`) — one JSON line per log event
  - Profile `dev`/`default`: human-readable console format
  - Log fields: `timestamp`, `level`, `logger`, `message`, `requestId` (MDC), `traceId` (MDC)
- [ ] Add `RequestIdFilter` (`OncePerRequestFilter`): generates a UUID per request, stores in MDC as `requestId`, adds `X-Request-Id` to response headers
- [ ] Set CloudWatch log group retention to 30 days in CDK `BackendStack`
- [ ] Verify: in prod, `docker run` emits JSON; CloudWatch Logs Insights query `fields @message | filter level = "ERROR"` works

### Demo seed data
- [ ] Create `backend/src/main/java/.../config/DataSeeder.java` — a `CommandLineRunner` annotated `@Profile("demo")`:
  - Runs only in the `demo` profile
  - Checks if the `clients` collection is empty; if so, inserts 3 sample clients
  - Checks if the `jobs` collection is empty; if so, inserts 2 sample jobs for those clients
  - Idempotent: safe to restart without duplicating data
- [ ] Seed data matches names from the existing mock (`Maria Gonzalez`, `James Mitchell`, `Sofia Ramirez`)
- [ ] Write `DataSeederTest` — verify seeder inserts data when collections empty, skips when already populated

### Smoke test for production deploy
- [ ] Add `smoke-test.sh` to `scripts/`:
  ```bash
  BASE_URL=$1  # e.g. https://tatalance.example.com
  # 1. health check
  curl -sf "$BASE_URL/api/health" | grep '"db":"ok"'
  # 2. unauthenticated API call returns 401 (not 200, not 500)
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/clients")
  [ "$STATUS" = "401" ] || exit 1
  echo "Smoke test passed."
  ```
- [ ] Add smoke test as final step in `deploy.yml` CD workflow after `ecs wait services-stable`

### Known limitations (documented, not blocked)
- **Pagination**: `GET /api/clients` and `GET /api/jobs` return all records. Acceptable for v1 (expected < 100 records in demos). Pagination will be needed before real-user growth.
- **DocumentDB vs MongoDB**: Flapdoodle tests run against real MongoDB 6; DocumentDB is MongoDB 5.0-compatible with some operator gaps. Simple find/save in v1 is not affected. Aggregation-heavy features (reports) should be tested against a DocumentDB instance before adding.
- **Single Fargate task in prod**: 1 desired task means zero-downtime is provided by rolling deploy (new task starts before old stops). There is a brief window where both tasks serve traffic. Redis sessions ensure continuity.
- **No staging environment**: deploys go straight to production. Acceptable for a solo-user tool. Add a staging stack in CDK before onboarding other users.

---

## Summary — Delivery order

| Chapter | Deliverable | Depends on |
|---|---|---|
| 0 | UI reorganization (React + Vite) | — |
| 1 | Backend scaffold (Spring Boot + MongoDB, error handler, health, logging) | — |
| 2 | Add Client backend — TDD | 1 |
| 3 | Add Client UI — wired to API | 0, 2 |
| 4 | Add Job/Drive backend — TDD | 2 |
| 5 | Add Job/Drive UI — wired to API | 3, 4 |
| 6 | Google OAuth2 + demo login (tata/tata) | 1 |
| 7 | UI bundled in Spring Boot JAR | 0, 1 |
| 8 | CI/CD + AWS deployment (CDK, ECS, DocumentDB, ALB sticky sessions) | 0, 1, 6, 7 |
| 9 | Operational readiness (logging, seed data, smoke test) | 8 |

**Chapters 0 and 1 start in parallel.**
**Chapters 2 and 6 start in parallel once Chapter 1 is done.**
**Chapter 7 (bundle) can start once Chapters 0 and 1 are done — independently of stories.**
**Chapter 9 runs after first successful deploy (Chapter 8).**

---

## Technology choices

| Layer | Choice | Reason |
|---|---|---|
| Backend language | Java 21 | LTS, virtual threads available |
| Backend framework | Spring Boot 3.3 | Ecosystem, OAuth2 support, mature |
| Build tool | Maven | Standard, good CI caching |
| Database (prod) | AWS DocumentDB (MongoDB 5.0-compatible) | Managed, no schema migrations, flexible document model |
| Database (local dev) | MongoDB 6 via Docker Compose | Same driver, same behavior |
| Database (tests) | Flapdoodle embedded MongoDB | Fast, no Docker required in CI |
| UI bundle | `frontend-maven-plugin` + Spring static resources | Single JAR artifact, same-origin API, no CORS in prod |
| Frontend | React 18 + Vite | Component model, testable, fast dev server |
| UI testing | Vitest + Testing Library | Co-located with Vite, fast |
| Auth | Spring Security OAuth2 Login + Google (server-side session) | No JWT complexity for single-admin v1 |
| Session store | In-memory + ALB sticky sessions | Free; ALB `AWSALB` cookie pins single user to same task; Redis unjustified for solo-admin tool |
| Login page | Thymeleaf template | CSRF token auto-injection for form login; keeps login outside the React SPA |
| Error responses | Spring Boot 3 ProblemDetail (RFC 9457) | Standardized JSON errors; built into framework |
| Logging | Logback JSON (prod) / console (dev) | CloudWatch Logs Insights queryable |
| Container | Docker (multi-stage, dep caching) + ECR | Standard AWS path; cached deps layer keeps builds fast |
| Hosting | ECS Fargate + ALB | Serverless containers, no EC2 management |
| IaC | AWS CDK (TypeScript) | Type-safe, composable |
| CI/CD | GitHub Actions | Already on GitHub, free tier |

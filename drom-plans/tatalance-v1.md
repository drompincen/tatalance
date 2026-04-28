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
  - Dependencies: Spring Web, Spring Data MongoDB, Spring Security, OAuth2 Client, Validation, Lombok
  - No Flyway, no JPA, no H2 — DocumentDB is schemaless
- [ ] Add `de.flapdoodle.embed:de.flapdoodle.embed.mongo` in `test` scope (embedded MongoDB for tests)
- [ ] Set up project packages: `domain`, `application`, `infrastructure.web`, `infrastructure.persistence`, `config`
- [ ] Create `docker-compose.yml` at repo root — `mongo:6` service on port 27017 for local dev
- [ ] Create `backend/src/main/resources/application.yml`:
  ```yaml
  spring:
    data:
      mongodb:
        uri: ${MONGODB_URI:mongodb://localhost:27017/tatalance}
  ```
- [ ] Create `backend/src/main/resources/application-test.yml` — override MongoDB URI to flapdoodle embedded port (handled automatically by `@DataMongoTest`)
- [ ] Configure CORS in `SecurityConfig` to allow `http://localhost:5173` (Vite dev proxy origin)
- [ ] Add SPA catch-all controller: `@Controller` returning `forward:/index.html` for all non-API, non-asset routes (enables React Router client-side navigation)
- [ ] Configure Spring Boot to serve static resources from `classpath:/static/`
- [ ] Add health-check endpoint `GET /api/health` → `{ status: "ok" }`
- [ ] Write `HealthControllerTest` (`@WebMvcTest`) — `GET /api/health` returns 200
- [ ] Verify `mvn test` passes and `mvn spring-boot:run` starts cleanly (with `docker-compose up -d`)

### Notes
- DocumentDB in AWS is MongoDB 5.0-compatible. Use Spring Data MongoDB — the driver is compatible.
- No schema migrations needed — documents are created on first write.
- Index creation: use `@Indexed` and `@CompoundIndex` on `@Document` classes; Spring Data MongoDB
  creates them on startup when `spring.data.mongodb.auto-index-creation=true` (dev only; manage
  indexes manually in prod via DocumentDB console or a startup script).

### Outcome
`backend/` builds, tests pass, app starts with local MongoDB, health endpoint responds.

---

## Chapter 2: Story 1 — Add Client (TDD, Backend)
**Status:** pending

### Goal
Full TDD cycle for the Client domain. Red → Green → Refactor for each layer.

### Domain model (MongoDB document)
```
Client { _id (ObjectId), name (required), phone (required), email, notes, createdAt }
```

### Steps
- [ ] **Document** `Client.java` — `@Document(collection="clients")`, `@NotBlank` on name + phone, `@Id String id`, `Instant createdAt`
- [ ] **Repository** `ClientRepository extends MongoRepository<Client, String>`
- [ ] **[RED]** Write `ClientServiceTest` (unit, no DB):
  - create with valid data → returns ClientResponse with id
  - create with missing name → throws ConstraintViolationException
  - create with missing phone → throws ConstraintViolationException
  - list → returns all clients
- [ ] **[GREEN]** Implement `ClientService` — `create(CreateClientRequest)`, `list()`
- [ ] **[RED]** Write `ClientControllerTest` (`@WebMvcTest`, mock service):
  - `POST /api/clients` with valid body → 201 + ClientResponse
  - `POST /api/clients` with missing name → 400 + field error `name`
  - `POST /api/clients` with missing phone → 400 + field error `phone`
  - `GET /api/clients` → 200 + array
- [ ] **[GREEN]** Implement `ClientController` — `POST /api/clients`, `GET /api/clients`, `GET /api/clients/{id}`
- [ ] **DTOs**: `CreateClientRequest` (name, phone, email, notes), `ClientResponse`
- [ ] **Integration test** `ClientIntegrationTest` (`@SpringBootTest`, uses flapdoodle embedded MongoDB):
  - create → verify persisted → list → verify appears → fetch by id
- [ ] Run `mvn test` — all green

### Acceptance criteria covered
- Missing name → 400 `{ field: "name", message: "name is required" }`
- Missing phone → 400 `{ field: "phone", message: "phone is required" }`
- Valid save → 201 + `ClientResponse` with generated id
- List → 200 + array

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
- [ ] **Document** `Job.java` — `@Document(collection="jobs")`, `@NotBlank` on clientId/pickupLocation/dropoffLocation, `@NotNull` on pickupDateTime
- [ ] **Enum** `JobStatus { BOOKED }`
- [ ] **Repository** `JobRepository extends MongoRepository<Job, String>` — add `findByClientId(String)`
- [ ] **[RED]** Write `JobServiceTest`:
  - create with valid data → returns JobResponse with status BOOKED and clientName populated
  - create with unknown clientId → throws ClientNotFoundException
  - create missing pickupDateTime → throws ConstraintViolationException
  - create missing pickupLocation → throws ConstraintViolationException
  - create missing dropoffLocation → throws ConstraintViolationException
  - list all → returns list
  - list by client → returns only that client's jobs
- [ ] **[GREEN]** Implement `JobService` — `create(CreateJobRequest)`, `list()`, `listByClient(String clientId)`
- [ ] **[RED]** Write `JobControllerTest` (`@WebMvcTest`):
  - `POST /api/jobs` valid → 201, status=BOOKED
  - `POST /api/jobs` missing required → 400 with field errors
  - `GET /api/jobs` → 200 + array
  - `GET /api/clients/{id}/jobs` → 200 + array filtered
- [ ] **[GREEN]** Implement `JobController`
- [ ] **DTOs**: `CreateJobRequest`, `JobResponse` (includes clientName)
- [ ] **Integration test** `JobIntegrationTest`:
  - insert client → create job for that client → list jobs → verify clientName denormalized → verify status BOOKED
  - create job with non-existent clientId → 404
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
- [ ] Add env vars to `application.yml`: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (no defaults — fail fast if missing in prod)
- [ ] Add `OAuth2UserService` to restrict access by email:
  - Load `ALLOWED_EMAILS` from env (comma-separated)
  - If authenticated email not in list → throw `OAuth2AuthenticationException` → `/login?error=unauthorized`
- [ ] Add **demo login** `UserDetailsService` bean:
  - Fixed credentials: username `tata`, password `tata` (BCrypt-hashed in config — never plain text in code)
  - Returns a `UserDetails` with role `ROLE_USER` and display name "Demo"
  - Enabled only when Spring profile `demo` is active — inactive in production by default
  - `application-demo.yml` sets `demo.user.enabled=true`; prod profile leaves it false
- [ ] Add `GET /api/me` → `{ name, email, picture }` — works for both auth paths:
  - Google: reads from `OAuth2AuthenticationToken`
  - Demo: reads from `UsernamePasswordAuthenticationToken`, returns `{ name: "Demo", email: "demo@local", picture: null }`
- [ ] Write `AuthControllerTest`:
  - `/api/me` unauthenticated → 401
  - `/api/me` with mock `OAuth2AuthenticationToken` → 200 + Google user info
  - `/api/me` with mock `UsernamePasswordAuthenticationToken` (demo) → 200 + `{ name: "Demo" }`
- [ ] Write security integration test: unauthenticated `GET /api/clients` → 401

#### Frontend
- [ ] Create `AuthContext` — `currentUser` state, loaded via `GET /api/me`
- [ ] `AuthGuard`: on 401, redirect to `/login`
- [ ] Create `/login` page — rendered by Spring Boot (Thymeleaf template or served as static HTML):
  - "Sign in with Google" button → `/oauth2/authorization/google`
  - Username / password form → `POST /login` (Spring Security form login endpoint)
  - Error message shown when `?error` is in the URL
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

#### Infrastructure (CDK)
- [ ] `npx cdk init app --language typescript` in `infrastructure/`
- [ ] `VpcStack` — VPC, 2 AZs, public + private subnets, NAT gateway (single AZ for cost)
- [ ] `DatabaseStack` — DocumentDB cluster (db.t3.medium, 1 instance for v1), private subnet group,
  Secrets Manager secret for `{ username, password, host, port }`, security group allowing only ECS task SG
- [ ] `BackendStack`:
  - ECR repository for Spring Boot image
  - ECS Cluster (Fargate)
  - Task definition: 512 CPU / 1024 MB, image from ECR, env vars from Secrets Manager
  - Fargate service: 1 desired task, rolling deploy (min 50%, max 200%)
  - ALB (internet-facing), HTTPS listener (ACM cert), HTTP → HTTPS redirect
  - ALB target group: port 8080, health check `GET /api/health`
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
- [ ] GitHub Secrets required: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`,
  `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_EMAILS`

#### Dockerfile (multi-stage)
```dockerfile
# Stage 1: build (Maven + Node via frontend-maven-plugin)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/ backend/
COPY ui/ ui/
RUN cd backend && mvn package -DskipTests

# Stage 2: run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Environment management
- [ ] `application-prod.yml` — all secrets read from env vars, no defaults
- [ ] DocumentDB URI constructed from Secrets Manager secret: `mongodb://{user}:{pass}@{host}:{port}/tatalance?tls=true&tlsAllowInvalidCertificates=false`
  (DocumentDB requires TLS — include `rds-combined-ca-bundle.pem` in the Docker image or use the JVM default trust store)
- [ ] DocumentDB TLS: add `--tlsCAFile` or configure MongoDB driver to use `ssl=true`
- [ ] `MONGODB_URI` injected as env var in ECS task definition from Secrets Manager

### Outcome
- PR → tests run in parallel (backend + UI), no deploy
- Merge to `main` → CI → full Maven build (includes UI) → Docker image → ECR → ECS rolling deploy
- App live on ALB DNS / custom domain, protected by Google Sign-In

---

## Summary — Delivery order

| Chapter | Deliverable | Depends on |
|---|---|---|
| 0 | UI reorganization (React + Vite) | — |
| 1 | Backend scaffold (Spring Boot + MongoDB) | — |
| 2 | Add Client backend — TDD | 1 |
| 3 | Add Client UI — wired to API | 0, 2 |
| 4 | Add Job/Drive backend — TDD | 2 |
| 5 | Add Job/Drive UI — wired to API | 3, 4 |
| 6 | Google OAuth2 Sign-In | 1 |
| 7 | UI bundled in Spring Boot JAR | 0, 1 |
| 8 | CI/CD + AWS deployment | 0, 1, 6, 7 |

**Chapters 0 and 1 start in parallel.**
**Chapters 2 and 6 start in parallel once Chapter 1 is done.**
**Chapter 7 (bundle) can start once Chapters 0 and 1 are done — independently of stories.**

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
| Container | Docker (multi-stage) + ECR | Standard AWS path |
| Hosting | ECS Fargate + ALB | Serverless containers, no EC2 management |
| IaC | AWS CDK (TypeScript) | Type-safe, composable |
| CI/CD | GitHub Actions | Already on GitHub, free tier |

---
title: "Issue #6 — Simple login to protect test data"
status: in-progress
created: 2026-05-09
updated: 2026-05-09
current_chapter: 3
---

# Plan: Simple login to protect test data

## Architecture Decision
Spring Security form login, single in-memory user from `APP_USERNAME`/`APP_PASSWORD` env vars
(dev defaults: admin/admin). BCrypt encoding. CSRF disabled (JSON API, same-origin UI).
Protect all routes; permit `/login` and `/logout` only.

## Chapter 1: RED — Write failing security test
**Status:** completed
**Depends on:** none

- [x] Add `spring-boot-starter-security` to `pom.xml`
- [x] Write `SecurityConfigTest.java` — unauthenticated → 302 /login, login page → 200, authenticated → 200
- [x] Run `mvn test` — expect new tests FAIL (SecurityConfig not yet written)

**Notes:**
> `@WebMvcTest` does not scan `@Configuration` classes — needs `@Import(SecurityConfig.class)` in tests.

## Chapter 2: GREEN — Implement SecurityConfig
**Status:** completed
**Depends on:** Chapter 1

- [x] Create `SecurityConfig.java` — form login + httpBasic, BCrypt, env-var credentials, CSRF disabled
- [x] Add `app.security.*` properties to `application.yml`
- [x] Fix `ClientControllerTest.java` — add `@WithMockUser` + `@Import(SecurityConfig.class)`
- [x] Fix `ClientIntegrationTest.java` — use `withBasicAuth("admin","admin")` in setUp
- [x] Run `mvn test` — all 14 tests GREEN

**Notes:**
> httpBasic enabled alongside formLogin: REST clients (TestRestTemplate, curl) use Basic auth;
> browsers use form login. unauthenticated REST requests → 401; browser requests → 302 to /login.

## Chapter 3: Deploy and verify
**Status:** pending
**Depends on:** Chapter 2

- [ ] Push to `drom` branch
- [ ] Verify pipeline deploys to `tatalance-drom`
- [ ] Confirm login screen appears at EB endpoint
- [ ] Set `APP_USERNAME` / `APP_PASSWORD` in EB env vars for each environment

**Notes:**
>

---

## Risks
- CSRF disabled — acceptable for demo; same-origin UI, no cross-site write risk
- Shared credentials per environment — fine for test data protection, not for prod users
- `ClientIntegrationTest` uses `TestRestTemplate` — needs `withBasicAuth` or it gets 302
- Existing `ClientControllerTest` (@WebMvcTest) will fail without `@WithMockUser` once security activates

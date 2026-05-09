---
title: "Issue #4 — Split client name into firstName and lastName"
status: in-progress
created: 2026-05-09
updated: 2026-05-09
current_chapter: 5
---

# Plan: Split client name into firstName and lastName

## Chapter 1: Test Infrastructure Setup
**Status:** completed
**Depends on:** none

- [x] Create `TatalanceApplicationTest.java` — context loads smoke test
- [x] Run `mvn test` — confirm baseline GREEN

**Notes:**
> Baseline passed. Flapdoodle works with Windows Maven (Java 25, os.arch override to amd64).

## Chapter 2: RED — Write Failing Tests
**Status:** completed
**Depends on:** Chapter 1

- [x] Add `spring-boot-starter-validation` to `pom.xml`
- [x] Create `ClientControllerTest.java` (`@WebMvcTest`) — POST with firstName/lastName returns 201, blank firstName returns 400, blank lastName returns 400, GET returns list with firstName/lastName
- [x] Run `mvn test` — confirm new tests FAIL (RED)

**Notes:**
> MockBean import is `org.springframework.boot.test.mock.mockito.MockBean` in Spring Boot 3.3.x (not `.mock.bean`).

## Chapter 3: GREEN — Update Production Code
**Status:** completed
**Depends on:** Chapter 2

- [x] Update `Client.java` — replace `name` with `firstName` + `lastName`, add `@NotBlank`
- [x] Update `ClientController.java` — add `@Valid` on `@RequestBody`
- [x] Update `index.html` — split form field, table columns, POST body, validation
- [x] Run `mvn test` — confirm all tests PASS (GREEN)

**Notes:**
> All 5 tests green (1 context + 4 controller).

## Chapter 4: Data Migration + Integration Test
**Status:** completed
**Depends on:** Chapter 3

- [x] Create `ClientNameMigration.java` — `CommandLineRunner` that splits old `name` into `firstName`/`lastName`
- [x] Create `ClientIntegrationTest.java` (`@SpringBootTest`) — full round-trip test + migration test
- [x] Run `mvn test` — all 10 tests pass

**Notes:**
> Migration is idempotent. Handles single-word names (entire value → firstName, empty lastName).

## Chapter 5: Deploy and Verify
**Status:** in-progress
**Depends on:** Chapter 4

- [ ] Push to `luciano` branch
- [ ] Verify pipeline deploys to `tatalance-luciano`
- [ ] Verify UI and migrated data at EB endpoint

**Notes:**
>

---

## Architecture Decisions

1. **Interim `@NotBlank` on Client document** — No DTOs exist yet. `Client` IS the request body. `@NotBlank` goes on document fields until Chapter 2 introduces DTOs.
2. **No service layer for this issue** — Service layer + DTOs arrive in v1 Chapter 2. Adding them for a field rename is premature.
3. **CommandLineRunner migration** — Teaches the pattern, works across all environments automatically, idempotent.
4. **Single-word name split** — Entire value goes to `firstName`, `lastName` gets empty string.

## Risks
- Flapdoodle tests must run from Windows Maven or IntelliJ (not WSL2 native on ARM64)
- Existing data in Atlas has old `name` field — migration handles this

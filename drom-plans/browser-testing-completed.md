---
title: "Browser Testing — Completed Stories"
status: completed
created: 2026-05-23
updated: 2026-05-24
current_chapter: done
---

# Plan: Browser Testing — Completed Stories

Verify all completed stories work correctly on the live Luciano environment.

**Target:** `http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com`

## Completed stories to test

| Story | Issue | What to verify |
|---|---|---|
| POC: Spring Boot + static UI | — | App loads, Swagger UI works, API spec tab renders |
| Client name split | #4 | firstName/lastName in form, table, API |
| Phone validation (E.164) | #5 | NOT YET DEPLOYED — must push first |

---

## Chapter 1: Push phone validation and wait for deploy
**Status:** completed
**Depends on:** none

- [x] Commit phone validation changes on `luciano` branch
- [x] Push to origin
- [x] Wait for GitHub Actions deploy to complete (run 26341396979)
- [x] Verify deploy succeeded via `gh run list`

---

## Chapter 2: API endpoint tests
**Status:** completed
**Depends on:** Chapter 1

Test the backend API directly with curl/WebFetch. **Result: 11/11 PASS**

### 2.1 Health & basic endpoints
- [x] `GET /` — returns 200, HTML contains "Tatalance"
- [x] `GET /v3/api-docs` — returns OpenAPI JSON with Client schema
- [x] `GET /swagger-ui/index.html` — returns 200

### 2.2 Client CRUD — happy path
- [x] `POST /api/clients` with valid E.164 phone — returns 201 with id, firstName, lastName, createdAt
- [x] `GET /api/clients` — returns array containing the created client

### 2.3 Client validation — rejection tests
- [x] `POST /api/clients` with blank firstName — returns 400
- [x] `POST /api/clients` with blank lastName — returns 400
- [x] `POST /api/clients` with blank phone — returns 400
- [x] `POST /api/clients` with phone "+123" (too short) — returns 400
- [x] `POST /api/clients` with phone "12125551234" (no plus) — returns 400
- [x] `POST /api/clients` with phone "+1212555abcd" (letters) — returns 400

---

## Chapter 3: UI smoke tests
**Status:** completed
**Depends on:** Chapter 1

Fetch the live HTML and verify the UI reflects completed stories. **Result: 8/8 PASS**

### 3.1 Page structure
- [x] index.html loads with "Tatalance" in title
- [x] Clients tab and API Spec tab present
- [x] Form has First Name, Last Name, Phone, Email fields
- [x] Phone placeholder shows "+1 555 000 0000"
- [x] Phone error message mentions "E.164"

### 3.2 API Spec tab
- [x] Swagger UI link points to `/swagger-ui/index.html`
- [x] OpenAPI schema has phone pattern constraint (`^\+[1-9]\d{9,14}$`)
- [x] Client schema shows firstName, lastName, phone, email

---

## Chapter 4: Clean up test data
**Status:** completed
**Depends on:** Chapter 2, 3

- [x] Test client created: "TestBot Browser" (+12125559999) — left in Atlas for visibility
- [x] Test results documented in Google Sheet "Test Results" tab

---

## Architecture Decisions

1. **Test against live EB env** — not localhost. This verifies the full deploy pipeline.
2. **API tests via curl** — fastest way to verify backend validation.
3. **UI tests via WebFetch** — fetch HTML and check content/structure.
4. **No Selenium/Playwright** — overkill for current scope. Manual-equivalent checks via HTTP.

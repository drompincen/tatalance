---
title: "Tatalance v1 — MVP: Book a Ride, Complete It, Get Paid"
status: in-progress
created: 2026-04-27
updated: 2026-06-21
current_chapter: epic-12
---

# Tatalance v1 Plan

## Outcome

David can add clients, book rides, and get paid — end to end in the browser.

## Tech stack (as deployed)

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21, Maven |
| Database | MongoDB Atlas |
| Database (test) | Flapdoodle embedded MongoDB |
| Frontend | Plain HTML/JS in `static/index.html` |
| Hosting | AWS Elastic Beanstalk (branch-per-env) |
| CI/CD | GitHub Actions |
| E2E testing | Playwright (planned) |

---

# Epic 1: David can add clients and book rides
**Status:** completed (core flow)
**Outcome:** David opens the app, adds a client, adds a driver, books a ride, assigns a driver, and marks it complete.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Phone validation (E.164) | completed | luciano | #5 |
| 2 | Complete Client management | completed | luciano | #10 |
| 3 | Add Driver management | completed | luciano | #11 |
| 4 | Create Ride | completed | luciano | #12 |
| 5 | Assign Driver to Ride | completed | luciano | #13 |
| 6 | Complete Ride | completed | luciano | #14 |

## Polish stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 7 | User-friendly validation error messages | completed | luciano | #25 |
| 8 | Edit and delete Clients | completed | luciano | #26 |
| 9 | Edit and delete Drivers | completed | luciano | #27 |
| 10 | Edit and cancel Rides | completed | luciano | #28 |
| 11 | Inline ride completion form (replace popups) | completed | luciano | #29 |

## QA stories (Playwright E2E)

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 12 | Set up Playwright infrastructure | completed | luciano | #17 |
| 13 | E2E: Add client and verify in list | completed | luciano | #18 |
| 14 | E2E: Add driver and toggle availability | completed | luciano | #19 |
| 15 | E2E: Book ride and assign driver | completed | luciano | #20 |
| 16 | E2E: Complete ride | completed | luciano | #21 |

## Dependencies

```
#10 (Client) ──┐
               ├──> #12 (Create Ride) --> #13 (Assign) --> #14 (Complete)  ✅ ALL DONE
#11 (Driver) ──┘

#25 (Error messages) — independent, improves all forms
#26 (Edit/delete Client) — independent
#27 (Edit/delete Driver) — independent
#28 (Edit/cancel Ride) — depends on #13
#29 (Inline completion) — improves #14 UX

#17 (Playwright setup) --> #18, #19, #20, #21
```

---

# Epic 2: David can invoice and get paid
**Status:** completed
**Outcome:** David generates an invoice from a completed ride, sends it, and records payment.
**Depends on:** Epic 1 (#14 Complete Ride) — done

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Generate Invoice | completed | luciano | #15 |
| 2 | Record Payment | completed | luciano | #16 |

## QA stories (Playwright E2E)

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 3 | E2E: Invoice and record payment | completed | luciano | #22 |

## Dependencies

```
#14 (Complete Ride) ✅ --> #15 (Invoice) --> #16 (Payment)
#17 (Playwright) --> #22
```

---

# Epic 3: UX evolution — flexible client management
**Status:** completed
**Outcome:** Tatalance becomes a flexible tool David can customize, not just a fixed-tab app.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Remove payment processing from UI | completed | luciano | #39 |
| 2 | Custom tables ("+" button on nav bar) | completed | luciano | #40 |
| 3 | Help page with UI guide panels | completed | luciano | #41 |
| 4 | Column CRUD on existing tables | completed | luciano | #42 |
| 5 | Inline column management (+ on header) | completed | luciano | #43 |
| 6 | Boolean columns with custom labels | completed | luciano | #44 |
| 7 | Linked columns (reference another table) | completed | luciano | #45 |

## Dependencies

```
#39 (Remove payments) — independent, quick win
#40 (Custom tables) — independent, largest story
#41 (Help page) — depends on #40 (needs to document custom tables)
#42 (Column CRUD) — depends on #40
#43 (Inline columns) — depends on #42
#44 (Boolean labels) — depends on #42, #43
#45 (Linked columns) — depends on #42, #43
```

---

# Foundation (completed)

| Story | Status | Issue |
|---|---|---|
| POC: Spring Boot + Flapdoodle + static UI | completed | — |
| CI/CD + Elastic Beanstalk | completed | — |
| Client name split (firstName/lastName) | completed | #4 |
| Phone validation (E.164) | completed | #5 |

---

# Issue index

| Issue | Title | Epic | Type | Status |
|---|---|---|---|---|
| #5 | Phone validation | Epic 1 | feature | completed |
| #10 | Complete Client management | Epic 1 | feature | completed |
| #11 | Add Driver management | Epic 1 | feature | completed |
| #12 | Create Ride | Epic 1 | feature | completed |
| #13 | Assign Driver to Ride | Epic 1 | feature | completed |
| #14 | Complete Ride | Epic 1 | feature | completed |
| #15 | Generate Invoice | Epic 2 | feature | completed |
| #16 | Record Payment | Epic 2 | feature | completed |
| #17 | Playwright setup | Epic 1 | qa | completed |
| #18 | E2E: Add client | Epic 1 | qa | completed |
| #19 | E2E: Add driver | Epic 1 | qa | completed |
| #20 | E2E: Book ride + assign | Epic 1 | qa | completed |
| #21 | E2E: Complete ride | Epic 1 | qa | completed |
| #22 | E2E: Invoice + payment | Epic 2 | qa | completed |
| #25 | User-friendly error messages | Epic 1 | polish | completed |
| #26 | Edit and delete Clients | Epic 1 | polish | completed |
| #27 | Edit and delete Drivers | Epic 1 | polish | completed |
| #28 | Edit and cancel Rides | Epic 2 | polish | completed |
| #29 | Inline ride completion form | Epic 2 | polish | completed |
| #39 | Remove payment processing | Epic 3 | feature | completed |
| #40 | Custom tables ("+" button) | Epic 3 | feature | completed |
| #41 | Help page with UI guide | Epic 3 | feature | completed |
| #42 | Column CRUD | Epic 3 | feature | completed |
| #43 | Inline column management | Epic 3 | feature | completed |
| #44 | Boolean columns with labels | Epic 3 | feature | completed |
| #45 | Linked columns | Epic 3 | feature | completed |
| #46 | Search/filter clients | Epic 4 | feature | completed |
| #47 | Filter rides by status | Epic 4 | feature | completed |
| #48 | Filter rides by date range | Epic 4 | feature | completed |
| #49 | Filter invoices by status | Epic 4 | feature | completed |
| #50 | Sort tables by column | Epic 4 | feature | completed |
| #52 | User registration and management | Epic 5 | feature | completed |
| #53 | Polling wipes form data while editing | Epic 5 | bug | completed |
| #54 | Per-user data isolation (epic) | Epic 6 | epic | completed |
| #55 | Foundation: userId + auth helper + migration | Epic 6 | feature | completed |
| #56 | Scope Client & Driver by userId | Epic 6 | feature | completed |
| #57 | Scope Ride by userId | Epic 6 | feature | completed |
| #58 | Scope Invoice by userId | Epic 6 | feature | completed |
| #59 | Scope Custom Tables by userId | Epic 6 | feature | completed |
| #60 | Update all tests for data isolation | Epic 6 | qa | completed |
| #61 | Security hardening (epic) | Epic 7 | epic | completed |
| #62 | Logout button + username display | Epic 7 | feature | completed |
| #63 | Password change | Epic 7 | feature | completed |
| #64 | Forgot password / password reset | Epic 7 | feature | completed |
| #65 | Re-enable CSRF protection | Epic 7 | feature | completed |
| #66 | Practical gaps (epic) | Epic 8 | epic | completed |
| #67 | Prevent booking rides in the past | Epic 8 | bug | completed |
| #68 | Prevent duplicate clients (same phone) | Epic 8 | bug | completed |
| #69 | Pagination for all list endpoints | Epic 8 | feature | completed |
| #70 | Google OAuth + Maps links (epic) | Epic 9 | epic | completed |
| #71 | Google OAuth2 login | Epic 9 | feature | completed |
| #72 | Link existing account to Google | Epic 9 | feature | completed |
| #73 | Pickup/dropoff as Google Maps links | Epic 9 | feature | completed |
| #74 | Time-based pricing (epic) | Epic 10 | epic | completed |
| #75 | Pricing mode + hourly rate on rides | Epic 10 | feature | completed |
| #76 | Live stopwatch on driver queue | Epic 10 | feature | completed |
| #77 | Duration + cost summary on completion | Epic 10 | feature | completed |
| #78 | Invoice shows time breakdown | Epic 10 | feature | completed |
| #93 | Freelance mode — hourly timer UI (epic) | Epic 11 | epic | completed |
| #96 | Map-based location picker (epic) | Epic 12 | epic | pending |
| #97 | Google Maps Platform API key + EB env | Epic 12 | infra | pending |
| #98 | LocationPicker modal (search + pin) | Epic 12 | feature | pending |
| #99 | Wire picker into book/edit ride form | Epic 12 | feature | pending |
| #100 | Store lat/lng on Ride for precise links | Epic 12 | enhancement | pending |
| #101 | E2E: book ride using LocationPicker | Epic 12 | qa | pending |

---

# Epic 4: David finds things fast
**Status:** completed
**Outcome:** David can search clients, filter rides by status and date, filter invoices, and sort any table.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Search/filter clients by name or phone | completed | luciano | #46 |
| 2 | Filter rides by status | completed | luciano | #47 |
| 3 | Filter rides by date range | completed | luciano | #48 |
| 4 | Filter invoices by status | completed | luciano | #49 |
| 5 | Sort tables by column headers | completed | luciano | #50 |

---

# Epic 5: Bugfixes & User Management
**Status:** completed
**Outcome:** Multiple users can register and log in; editing forms no longer wipes data on auto-refresh.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Add user registration and management | completed | luciano | #52 |
| 2 | Fix: polling wipes form data while editing | completed | luciano | #53 |

## Dependencies

```
#52 (User registration) — independent, changes SecurityConfig + adds AppUser document
#53 (Polling fix) — independent, UI-only change (fix already stashed)
```

---

# Epic 6: Per-user data isolation (multi-tenancy)
**Status:** completed
**Outcome:** Each user sees only their own data. User A's clients, drivers, rides, invoices, and custom tables are invisible to User B.
**Depends on:** Epic 5 (#52 — user registration)

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Foundation: userId field + auth helper + data migration | completed | luciano | #55 |
| 2 | Scope Client & Driver by userId | completed | luciano | #56 |
| 3 | Scope Ride by userId | completed | luciano | #57 |
| 4 | Scope Invoice by userId | completed | luciano | #58 |
| 5 | Scope Custom Tables by userId | completed | luciano | #59 |
| 6 | Update all tests for data isolation | completed | luciano | #60 |

## Dependencies

```
#55 (Foundation) — first, adds userId field + AuthHelper
  |
  ├──> #56 (Client & Driver) — independent from #59
  |       |
  |       └──> #57 (Ride) — needs Client & Driver scoped
  |               |
  |               └──> #58 (Invoice) — needs Ride scoped
  |
  └──> #59 (Custom Tables) — independent from #56-#58

#60 (Tests) — last, after all scoping stories
```

## Key decisions
- Return 404 (not 403) when accessing another user's data — no information leakage
- Invoice numbers are per-user (INV-YYYY-001, 002...) to avoid collisions
- Existing data migrated to admin user on startup (idempotent)
- LINK columns in custom tables can only reference the current user's tables

---

# Epic 7: Security hardening
**Status:** completed
**Outcome:** Users can sign out, change passwords, recover accounts, and the app is protected against CSRF.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Logout button + username display in header | completed | luciano | #62 |
| 2 | Password change | completed | luciano | #63 |
| 3 | Forgot password / password reset | completed | luciano | #64 |
| 4 | Re-enable CSRF protection | completed | luciano | #65 |

## Dependencies

```
#62 (Logout + username) — first, establishes user menu in header
  ├──> #63 (Password change) — adds to user menu
  └──> #64 (Forgot password) — adds to login page
#65 (CSRF) — independent, can be done anytime
```

---

# Epic 8: Practical gaps
**Status:** completed
**Outcome:** The app handles real-world edge cases — no past dates, no duplicate clients, no performance cliffs.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Prevent booking rides in the past | completed | luciano | #67 |
| 2 | Prevent duplicate clients (same phone) | completed | luciano | #68 |
| 3 | Pagination for all list endpoints | completed | luciano | #69 |

## Dependencies

```
#67 (Past dates) — independent
#68 (Duplicate clients) — independent (compound index with userId after Epic 6)
#69 (Pagination) — best done after Epic 6 (queries already scoped by userId)
```

---

# Epic 9: Google OAuth + Maps links
**Status:** completed
**Outcome:** Tata can sign in with Google (in addition to username/password), and pickup/dropoff locations are clickable Google Maps links.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Google OAuth2 login ("Sign in with Google" button) | completed | luciano | #71 |
| 2 | Link existing account to Google identity | completed | luciano | #72 |
| 3 | Pickup/dropoff as clickable Google Maps links | completed | luciano | #73 |

## Dependencies

```
#71 (Google OAuth) — first, adds Spring Security OAuth2 client + Google Console setup
  └──> #72 (Link account) — maps Google identity to existing AppUser
#73 (Maps links) — independent, small UI change
```

---

# Epic 10: Time-based pricing
**Status:** completed
**Outcome:** Tata can book rides with flexible pricing (flat, hourly, or flat + hourly). Drivers see a live stopwatch. Invoices show the time breakdown.

## Pricing modes

| Mode | Formula | Example |
|---|---|---|
| FLAT | basePrice | $100 flat |
| HOURLY | duration × hourlyRate | 2h 15m × $7.50 = $16.88 |
| FLAT_PLUS_HOURLY | basePrice + (duration × hourlyRate) | $50 + 2h 15m × $7.50 = $66.88 |

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Pricing mode + hourly rate on rides | completed | luciano | #75 |
| 2 | Live stopwatch on driver queue | completed | luciano | #76 |
| 3 | Duration + cost summary on completion | completed | luciano | #77 |
| 4 | Invoice shows time breakdown | completed | luciano | #78 |

## Dependencies

```
#75 (Pricing mode) — first, adds pricingMode + hourlyRate to Ride model + booking form
  ├──> #76 (Stopwatch) — needs actualStart from start-ride flow
  └──> #77 (Completion summary) — needs hourlyRate + duration to calculate cost
        └──> #78 (Invoice breakdown) — needs duration + rate stored on completed ride
```

---

# Epic 11: Freelance mode — book job, track time, invoice (issue #93)
**Status:** completed
**Outcome:** Luciano can bill Tatalance dev work hourly: book a job without starting the timer, start/pause/resume with server-side segments, see billable amount, complete the day, and generate an invoice. Chauffeur ops (`index.html`) stays unchanged for David.
**Depends on:** Epic 10 (#75 hourly pricing on rides) — done
**Design reference:** `docs/freelance-jobs-mockup.html`, `docs/ui-redesign-mockups.html`
**Production UI:** `backend/src/main/resources/static/freelance.html`

## User journey

1. Sign in → redirected to `/freelance.html` when `businessMode=FREELANCE`
2. Set hourly rate in sidebar (saved to profile)
3. **Jobs** → book job (client, title, date) — timer does **not** start
4. Press **Start timer** on the job card (Jobs tab or Dashboard “ready” list)
5. Pause / Resume during the day (segments logged server-side)
6. **Complete day** → invoice auto-generated → **Invoices** → Mark paid

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | `BusinessMode` + `defaultHourlyRate` on `AppUser`; `PATCH /api/users/me/settings` | completed | luciano | #93 |
| 2 | `jobTitle`, `workSegments`, `RideStatus.PAUSED` on `Ride` model | completed | luciano | #93 |
| 3 | `TimerService` — start, pause, resume, billable seconds/amount | completed | luciano | #93 |
| 4 | Timer REST API: `POST …/timer/pause`, `…/resume`, `GET …/timer` | completed | luciano | #93 |
| 5 | Freelance UI (`freelance.html`) wired to APIs | completed | luciano | #93 |
| 6 | Login redirect by `businessMode`; link between freelance ↔ chauffeur | completed | luciano | #93 |
| 7 | Mark paid / unpaid on invoices in freelance UI | completed | luciano | #93 |
| 8 | Start timer discoverability (dashboard ready-jobs + auto-navigate after book) | completed | luciano | #93 |
| 8b | Fix booking failures (past pickup time, silent API errors) | completed | luciano | #93 |
| 9 | E2E: freelance book → start → pause → complete → invoice | completed | luciano | #93 |
| 10 | Deploy + verify on `tatalance-luciano` EB sandbox | completed | luciano | #93 |

## Dependencies

```
#75 (HOURLY pricing on Ride) ✅
  └──> #93 stories 1–4 (backend timer + user settings)
        └──> #93 stories 5–8 (freelance.html)
              └──> #93 story 9 (Playwright E2E)
                    └──> #93 story 10 (cloud verify)
```

## Architecture (Option B — one app, two surfaces)

| Surface | URL | User | Billing |
|---|---|---|---|
| Freelance | `/freelance.html` | Luciano | `HOURLY` + `jobTitle` + `workSegments` |
| Chauffeur | `/index.html` | David | FLAT / HOURLY / FLAT_PLUS_HOURLY rides |

Same `Ride` collection and APIs; freelance uses deferred timer start (book ≠ start).

---

# Epic 12: Map-based location picker (issue #96)
**Status:** pending
**Outcome:** David picks pickup and dropoff on an embedded Google Map (search or drop a pin) instead of typing addresses. The ride table shows simple location text; clicking it opens Google Maps for navigation (extends Epic 9 #73).
**Depends on:** Epic 9 #73 (clickable Maps links) — done; Epic 1 #12 (Create Ride form) — done
**Blocked by:** #97 (Google Maps API key in EB — drom)

## User journey

1. Book/edit ride → tap **Pickup** or **Dropoff**
2. Location picker modal opens (embedded map + search)
3. Search or tap map to place pin → reverse-geocode to address text
4. Confirm → text fills form; saved to `pickupLocation` / `dropoffLocation`
5. Rides table shows text; click → Google Maps (existing `mapsLink`)

## Design note

Opening external `google.com/maps` in another tab cannot return the picked pin to the app. The picker must be **in-app** (Maps JavaScript API + Places).

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Google Maps Platform API key + EB env config | pending | drom | #97 |
| 2 | `LocationPicker` modal — search + pin drop → address | pending | luciano | #98 |
| 3 | Wire picker into chauffeur book/edit form (`index.html`) | pending | luciano | #99 |
| 4 | Store `pickupLat/Lng`, `dropoffLat/Lng` on `Ride` (optional) | pending | luciano | #100 |
| 5 | E2E: book ride using LocationPicker | pending | luciano | #101 |

## Dependencies

```
#97 (API key + EB env) — drom, blocks frontend work
  └──> #98 (LocationPicker component)
        └──> #99 (wire into book/edit form)
              └──> #101 (E2E)
#100 (lat/lng) — optional follow-up after #99
```

## Out of scope

- Freelance `freelance.html` (keeps `Remote` default for now)
- GPS capture on driver Start/Complete
- In-app map view when clicking table links (keep external Maps)

---

# What's NOT in v1 (nice-to-haves for later)

| Feature | Why deferred |
|---|---|
| Dashboard stats (rides, revenue, outstanding) | Needs data volume to be meaningful |
| Export invoices to PDF/CSV | Useful but not blocking core workflow |
| Ride reminders / notifications | Needs notification infrastructure |
| Activity log (who changed what) | Audit trail — add when multi-user is stable |
| Driver self-service UI | David manages everything in MVP |
| Driver payouts | Not needed for core book-to-pay loop |
| VIP preferences | Nice-to-have fields for later |
| Reports/analytics | Needs data volume |
| i18n (EN/ES) | Port when UI is stable |
| React/Vite | Plain HTML/JS works for MVP scope |
| In-app payment processing | David collects payment directly from clients — app is for management, not transactions |

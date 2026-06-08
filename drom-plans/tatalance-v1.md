---
title: "Tatalance v1 — MVP: Book a Ride, Complete It, Get Paid"
status: in-progress
created: 2026-04-27
updated: 2026-06-08
current_chapter: epic-5
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
| #52 | User registration and management | Epic 5 | feature | not started |
| #53 | Polling wipes form data while editing | Epic 5 | bug | not started |

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
**Status:** in-progress
**Outcome:** Multiple users can register and log in; editing forms no longer wipes data on auto-refresh.

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Add user registration and management | not started | luciano | #52 |
| 2 | Fix: polling wipes form data while editing | not started | luciano | #53 |

## Dependencies

```
#52 (User registration) — independent, changes SecurityConfig + adds AppUser document
#53 (Polling fix) — independent, UI-only change (fix already stashed)
```

---

# What's NOT in v1

| Feature | Why deferred |
|---|---|
| Driver self-service UI | David manages everything in MVP |
| Driver payouts | Not needed for core book-to-pay loop |
| VIP preferences | Nice-to-have fields for later |
| Reports/analytics | Needs data volume |
| i18n (EN/ES) | Port when UI is stable |
| React/Vite | Plain HTML/JS works for MVP scope |
| In-app payment processing | David collects payment directly from clients — app is for management, not transactions |

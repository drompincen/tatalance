---
title: "Tatalance v1 — MVP: Book a Ride, Complete It, Get Paid"
status: in-progress
created: 2026-04-27
updated: 2026-05-24
current_chapter: epic-2
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
| 2 | Complete Client management | pending | — | #10 |
| 3 | Add Driver management | completed | luciano | #11 |
| 4 | Create Ride | completed | luciano | #12 |
| 5 | Assign Driver to Ride | completed | luciano | #13 |
| 6 | Complete Ride | completed | luciano | #14 |

## Polish stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 7 | User-friendly validation error messages | pending | — | #25 |
| 8 | Edit and delete Clients | pending | — | #26 |
| 9 | Edit and delete Drivers | pending | — | #27 |
| 10 | Edit and cancel Rides | pending | — | #28 |
| 11 | Inline ride completion form (replace popups) | pending | — | #29 |

## QA stories (Playwright E2E)

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 12 | Set up Playwright infrastructure | pending | — | #17 |
| 13 | E2E: Add client and verify in list | pending | — | #18 |
| 14 | E2E: Add driver and toggle availability | pending | — | #19 |
| 15 | E2E: Book ride and assign driver | pending | — | #20 |
| 16 | E2E: Complete ride | pending | — | #21 |

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
**Status:** in-progress (next up)
**Outcome:** David generates an invoice from a completed ride, sends it, and records payment.
**Depends on:** Epic 1 (#14 Complete Ride) — done

## Feature stories

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Generate Invoice | pending | — | #15 |
| 2 | Record Payment | pending | — | #16 |

## QA stories (Playwright E2E)

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 3 | E2E: Invoice and record payment | pending | — | #22 |

## Dependencies

```
#14 (Complete Ride) ✅ --> #15 (Invoice) --> #16 (Payment)
#17 (Playwright) --> #22
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
| #10 | Complete Client management | Epic 1 | feature | pending |
| #11 | Add Driver management | Epic 1 | feature | completed |
| #12 | Create Ride | Epic 1 | feature | completed |
| #13 | Assign Driver to Ride | Epic 1 | feature | completed |
| #14 | Complete Ride | Epic 1 | feature | completed |
| #15 | Generate Invoice | Epic 2 | feature | pending |
| #16 | Record Payment | Epic 2 | feature | pending |
| #17 | Playwright setup | Epic 1 | qa | pending |
| #18 | E2E: Add client | Epic 1 | qa | pending |
| #19 | E2E: Add driver | Epic 1 | qa | pending |
| #20 | E2E: Book ride + assign | Epic 1 | qa | pending |
| #21 | E2E: Complete ride | Epic 1 | qa | pending |
| #22 | E2E: Invoice + payment | Epic 2 | qa | pending |
| #25 | User-friendly error messages | Epic 1 | polish | pending |
| #26 | Edit and delete Clients | Epic 1 | polish | pending |
| #27 | Edit and delete Drivers | Epic 1 | polish | pending |
| #28 | Edit and cancel Rides | Epic 2 | polish | pending |
| #29 | Inline ride completion form | Epic 2 | polish | pending |

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

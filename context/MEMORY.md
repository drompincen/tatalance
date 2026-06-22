# Session Memory

## Current Focus

Merged `main` (PR #94: multi-profile + Job refactor + CSRF login fix) into `luciano` — resolving conflicts with freelance timer UI, OAuth fix, Epic 12/13 plans.

Epic 13 (#102): Spanish UI for Tata — planned.
Epic 12 (#96): map location picker — planned; blocked on #97 / #109 (Google credentials).
Epic 11 (#93): freelance mode completed on luciano; PR #95 for main merge.

## Recent Decisions

- Option B: one app, two surfaces (`freelance.html` + `index.html`)
- `Ride extends Job` (main) + `workSegments` / `jobTitle` on Ride (luciano freelance)
- Google OAuth hidden when `GOOGLE_CLIENT_ID` missing (#109 for drom)
- Multi-profile: `profileId` on jobs, profile switcher in header (from main)

## Key Findings

- **main merge:** drom's Category A Job refactor + multi-profile + login CSRF fix integrated with luciano TimerService + freelance.html
- **Google sign-in:** requires EB env vars; buttons hidden until configured
- **Booking fix:** past pickup datetime + error toasts on freelance/chauffeur forms

## Session Log

- 2026-06-21: Freelance mode shipped on luciano (Epic 11). PR #95, CI green.
- 2026-06-21: Epic 12/13 formalized (#96–#108). Google OAuth UX fix `f1e315b`, issue #109 for drom.
- 2026-06-21: Merged origin/main into luciano (multi-profile + Job model + CSRF login).
- 2026-06-21 (drom): Added clear profile update + "Manage Profiles" settings modal (list, edit via PUT, create); type labels distinguish DRIVER (rides) vs hourly (ENGINEER etc); updateTabVisibility hides #btn-rides for non-DRIVER; fixed ride create to pass ?profileId for scoping; mvn test 166/0 + targeted m6 e2e 9/9 green; smoke API verified create/update/scoped list; pushed to drom (commit 66db138).
- 2026-06-21: Started jacoco-coverage-gate plan (issue #112). Added jacoco-maven-plugin 0.8.12 to pom with 60% LINE/BRANCH gate on verify. Baseline run: 166 tests 0 failures, overall Instructions 62%, Branches 37% (failed gate as expected). Profile package low coverage (25% line, 0% branch). Iteration 1: added ProfileControllerTest + StatsControllerTest + SearchControllerTest (171 tests), coverage to 39% branch. CI updated with test job. Gate active on verify. Closed loop in progress.
- 2026-06-21 (drom): **jacoco-coverage-gate COMPLETED.** Build was broken by commit `e6be4f5` which set flapdoodle to `de.flapdoodle.embed.mongo.spring4x:4.12.2` — a non-existent coordinate (`spring4x` versions start at 4.22.0; it also targets **Spring Boot 4.x**, needing `org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration` absent in Boot 3.3.5). Reverted to **`spring3x:4.12.2`** (correct module for Boot 3.3.5, prior known-good), kept `runtime` scope in `dev` profile so localhost run keeps embedded Mongo. `mvn clean verify` (dev profile active): **BUILD SUCCESS, 283 tests 0 failures/0 errors**, gate "All coverage checks have been met" — **LINE 85.8%, BRANCH 66.3%, INSTRUCTION 84.1%** (gate 60%). Plan note corrected: run `mvn clean verify` (NOT `-P-dev`, which disables Flapdoodle and drops branch coverage below gate).
- 2026-06-21 (closed-loop Iter2): Added full UserControllerTest (20 tests covering me/settings/register/forgot/change-pw branches). Enhanced StatsControllerTest (rich stream data for revenue/payout filters) + SearchControllerTest (match data + sections). Tests now 196/0. Branch from 39% -> 53%. Re-ran verify (still <60 as expected). CI workflow switched to mvn verify for gate enforcement on GH.
- 2026-06-21 (closed-loop Iter3): Enhanced TimerServiceTest (conflict paths: start on completed/cancelled, pause/resume invalid states, billable modes) + added 5 RideControllerTest cases (profileId scoping create/list, past-date 400, non-scheduled update 400). Total tests ~200+. Branch 53% -> 55-56% (check: 0.55). No regressions. Ride/Timer/User still top remaining. Plan loop continuing to >=60% + GH pass.
- 2026-06-21 (closed-loop cont.): Added more unit tests (Ride: listByClient, createJob w/ profile, update w/ jobTitle, listByDriver, cancel frees driver; User: zero rate error, forgot no user; Driver: update payout, invalid avail patch; Client: create w/ email). Still 56.08% branch / 65.2% line in unit-only runs (flapdoodle not starting, ints skipped). Full flapdoodle (user local im-mem) required to hit >=60% branch as before. Extensive notes added to plan/pom/workflow/tests/MEMORY as requested. Plan in-progress until stable gate in full run. Continuing closed-loop with more units.
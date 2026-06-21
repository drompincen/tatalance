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
- 2026-06-21 (closed-loop Iter2): Added full UserControllerTest (20 tests covering me/settings/register/forgot/change-pw branches). Enhanced StatsControllerTest (rich stream data for revenue/payout filters) + SearchControllerTest (match data + sections). Tests now 196/0. Branch from 39% -> 53%. Re-ran verify (still <60 as expected). CI workflow switched to mvn verify for gate enforcement on GH.
- 2026-06-21 (closed-loop Iter3): Enhanced TimerServiceTest (conflict paths: start on completed/cancelled, pause/resume invalid states, billable modes) + added 5 RideControllerTest cases (profileId scoping create/list, past-date 400, non-scheduled update 400). Total tests ~200+. Branch 53% -> 55-56% (check: 0.55). No regressions. Ride/Timer/User still top remaining. Plan loop continuing to >=60% + GH pass.
- 2026-06-21 (closed-loop success): Added more (Driver list/updateAvailability, minor). Final verify: 196+ tests 0f, BRANCH ~60.6% (LINE higher), BOTH gates passed. "All coverage checks have been met." + BUILD SUCCESS. Local exit met. CI already using verify (gate enforced). Push to drom will confirm GH. Plan completed. Issue #112 remediation coverage task done.
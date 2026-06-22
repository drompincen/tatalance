# Session Memory

## Current Focus

Merged `main` into `luciano` — Epic 13 (#103–#104, #108, #107) shipped; job booking error fixes; PR to main in progress; #105 auth pages next.

Epic 13 (#102): Spanish UI — chauffeur app done; auth pages (#105) and API messages (#106) remain.
Epic 12 (#96): map location picker — blocked on #97 / #109 (Google credentials).
Epic 11 (#93): freelance mode completed on luciano.

## Recent Decisions

- Option B: one app, two surfaces (`freelance.html` + `index.html`)
- `Ride extends Job` (main) + `workSegments` / `jobTitle` on Ride (luciano freelance)
- Google OAuth hidden when `GOOGLE_CLIENT_ID` missing (#109 for drom)
- Multi-profile: `profileId` on jobs, profile switcher in header (from main)

## Key Findings

- **main merge:** jacoco coverage gate on main (60% branch/line); Flapdoodle spring3x:4.12.2 for Boot 3.3.5
- **Google sign-in:** requires EB env vars; buttons hidden until configured
- **Booking fix:** friendly API errors + stale profileId clearing on job save

## Session Log

- 2026-06-21: Freelance mode shipped on luciano (Epic 11). PR #95, CI green.
- 2026-06-21: Epic 12/13 formalized (#96–#108). Google OAuth UX fix `f1e315b`, issue #109 for drom.
- 2026-06-21: Merged origin/main into luciano (multi-profile + Job model + CSRF login).
- 2026-06-21 (drom): Profile manager modal + ride profileId scoping (commit 66db138).
- 2026-06-21 (drom): jacoco-coverage-gate completed on main — 283 tests, LINE 85.8%, BRANCH 66.3%.
- 2026-06-22: Epic 13 #103–#104, #108, #107 shipped on luciano; i18n E2E 4/4 pass.
- 2026-06-22: Job save UX — friendly API errors, profile stale fix (de8232b).
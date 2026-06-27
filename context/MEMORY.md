# Session Memory

## Current Focus

**Epic 13 (#102): completed on `luciano`** — PR #114 open (`luciano` → `main`); includes #103–#108 and #106 (`ApiMessageResolver` + `Accept-Language`). Awaiting drom review + merge.

**Next build:** Epic 12 (#96) map location picker — **blocked on drom** #97 / #109 (Google Maps API key on EB). Luciano stories: #98 → #99 → #101; optional #100.

**Shipped locally (not yet in PR):** Epic 14 — configurable tax (#116) + mobile freelance UX (#117). Commit + PR when CI green.

**v1 plan:** `drom-plans/tatalance-v1.md` — `current_chapter: epic-14` (billing polish done); next execution epic-12 when unblocked.

## Recent Decisions

- Option B: one app, two surfaces (`freelance.html` + `index.html`)
- `Ride extends Job` (main) + `workSegments` / `jobTitle` on Ride (luciano freelance)
- Google OAuth hidden when `GOOGLE_CLIENT_ID` missing (#109 for drom)
- Multi-profile: `profileId` on jobs, profile switcher in header (from main)
- Spanish API errors: `Accept-Language` header from `i18n.js` fetch wrapper; `ApiMessageResolver` on backend (#106)

## Key Findings

- **main merge:** jacoco coverage gate on main (60% branch/line); Flapdoodle spring3x:4.12.2 for Boot 3.3.5
- **Google sign-in:** requires EB env vars; buttons hidden until configured
- **Plan sync:** Epic 13 marked completed in `tatalance-v1.md` 2026-06-25 (was drifted)

## Session Log

- 2026-06-21: Freelance mode shipped on luciano (Epic 11). PR #95, CI green.
- 2026-06-21: Epic 12/13 formalized (#96–#108). Google OAuth UX fix `f1e315b`, issue #109 for drom.
- 2026-06-21: Merged origin/main into luciano (multi-profile + Job model + CSRF login).
- 2026-06-21 (drom): Profile manager modal + ride profileId scoping (commit 66db138).
- 2026-06-21 (drom): jacoco-coverage-gate completed on main — 283 tests, LINE 85.8%, BRANCH 66.3%.
- 2026-06-22: Epic 13 #103–#104, #108, #107 shipped on luciano; i18n E2E pass.
- 2026-06-22: PR #114 opened for Epic 13 + booking UX fixes.
- 2026-06-25: #106 Spanish API validation messages shipped (`b7dd23c`). Plan + MEMORY synced.
- 2026-06-27: Epic 14 stories created — #116 configurable tax, #117 mobile freelance UX. Issue bodies in `docs/github-issue-*.md`; `tatalance-v1.md` epic table updated.
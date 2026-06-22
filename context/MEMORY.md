# Session Memory

## Current Focus

Merged `main` (PR #94: multi-profile + Job refactor + CSRF login fix) into `luciano` — resolving conflicts with freelance timer UI, OAuth fix, Epic 12/13 plans.

Epic 13 (#102): Spanish UI for Tata — in progress (#103–#104, #108, #107 done; #105 auth pages, #106 API messages remain).
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
- 2026-06-22: Epic 13 #103 — i18n foundation: `backend/src/main/resources/static/js/i18n.js` (t/getLang/setLang/toggleLang/applyI18n, localStorage, es browser detect); EN/ES toggle in index.html header; data-i18n on nav tabs + account menu + search.
- 2026-06-22: Epic 13 #104 — full chauffeur UI translation in index.html (~250 i18n keys); data-filter on filter buttons; tf()/statusLabel()/langchange refresh for active tab.
- 2026-06-22: Epic 13 #108 — help overlay 6 panels translated via help.* keys in i18n.js; renderHelpPanel uses tf().
- 2026-06-22: Epic 13 #107 — tests/e2e/i18n-language-toggle.spec.ts (4 tests); all pass locally against localhost:8080.
# Session Memory

## Current Focus

Epic 13 (issue #102): **planned** — Spanish UI for Tata (EN/ES toggle, ? help in Spanish). Stories #103–#108. Reference: `docs/js/i18n.js`.

Epic 12 (issue #96): **planned** — map-based location picker for pickup/dropoff. Blocked on #97 (Google Maps API key — drom). Stories #98–#101 for Luciano.

Epic 11 (issue #93): **completed** — freelance mode; PR #95 open for merge to main.

## Recent Decisions

- Option B: one app, two surfaces (`freelance.html` + `index.html`) — see `context/DECISIONS.md`
- Book job ≠ start timer; Start is explicit on job card or dashboard ready list
- WebMvcTest slices import real `TimerService` (Java 25 cannot mock it)

## Key Findings

- **Booking failed silently:** pickup datetime defaulted to page-load time; by submit it was in the past → 400. Freelance `api()` treated error JSON as success. Fixed: bump to ≥1 min future, show error toasts, require `created.id`.
- **Where to Start timer:** Jobs tab → job card on the right → **Start timer** button (SCHEDULED jobs only). Dashboard also lists ready jobs with Start after UX fix.
- Mark paid was missing from freelance UI — added `POST /api/invoices/{id}/mark-paid` button per row
- Hourly rate: sidebar input on freelance page; chauffeur ops via **Freelance** link in header

## Open Questions

- Remaining UI redesign questionnaire (theme, HYBRID mode, etc.) — deferred; following mockup for now
- Seed default "Tatalance development" job on first login?

## Session Log

- 2026-06-21: Implemented freelance mode (TimerService, workSegments, freelance.html). Commits `51c530f`, `d87118c`. Local smoke passed. Plan updated with Epic 11.
- 2026-06-21: Fixed mark paid in freelance UI; improved Start timer discoverability on dashboard + after book.
- 2026-06-21: Shipped Epic 11 story 10 — fixed mobile header overflow (`1a6a766`), m5 login E2E for freelance redirect (`1ad5193`). CI deploy success; cloud smoke verified.
- 2026-06-21: Formalized Epic 12 map location picker — GitHub #96–#101, plan + journey docs updated.
- 2026-06-21: Formalized Epic 13 Spanish i18n for Tata — GitHub #102–#108, plan + decisions updated.
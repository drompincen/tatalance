# Session Memory

## Current Focus

Epic 11 (issue #93): Freelance mode shipped on `luciano` — E2E added; awaiting EB deploy verify after push.

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
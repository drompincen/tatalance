---
title: "Mobile iPhone Browser Journey — M1-M5"
status: pending
created: 2026-05-24
updated: 2026-05-24
current_chapter: 1
journey: docs/journeys/mobile-iphone-journey.md
issues: [31, 32, 33, 34, 35]
---

# Plan: Mobile iPhone Browser Journey

Make Tatalance fully usable from **mobile Safari on iPhone**. Two personas
operate from the phone: **David** (books on the go) and **drivers** (queue
view, start/complete from the field). No native app, no PWA install.

Source of truth for stories and acceptance criteria: `docs/journeys/mobile-iphone-journey.md`.

## Architecture Decision

- Same SPA-less HTML/JS stack as the rest of the app — no React, no router.
- Single CSS breakpoint at **768px** (desktop ↔ mobile); secondary at **480px** if a screen needs to compact further.
- Off-canvas sidebar pattern already prototyped in `docs/index.html` — port to `backend/src/main/resources/static/index.html`.
- Driver queue is a plain page at `/driver-queue.html?id={driverId}` — no router, no SPA shell, minimal bundle.
- No PWA (manifest/service worker) in scope — Safari support is weak; revisit if usage justifies.
- All new endpoints follow existing `DriverController` / `RideController` patterns — no new framework code.
- Auth for driver queue in MVP: obscure URL + existing login. Harden in phase 2.

## Chapter 1: Responsive Shell (M1)
**Status:** pending
**Issue:** #31
**Depends on:** none

- [ ] Add `<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">` to `backend/src/main/resources/static/index.html` (already on `docs/index.html`)
- [ ] Port off-canvas sidebar + hamburger + overlay from `docs/index.html` to the served app
- [ ] Sidebar: `transform: translateX(-100%)` under 768px, slides in on `.open`; tap-overlay or nav-link closes
- [ ] Topbar sticky, padding-top respects `env(safe-area-inset-top)`
- [ ] Body container uses `min-height: 100dvh` (replace any `100vh`)
- [ ] All `<table>` lists get a sibling card-list render under 640px (or convert to flex card layout via CSS)
- [ ] Audit tap targets — buttons / nav links ≥ 44×44 px
- [ ] Set form input `font-size: 16px` minimum across login + add-client + add-driver + add-ride forms
- [ ] Manual check on iPhone SE (375 px) — no horizontal scroll, keyboard does not hide submit

**Notes:**
>

## Chapter 2: Login on iPhone (M5)
**Status:** pending
**Issue:** #35
**Depends on:** Chapter 1 (uses the same 16px input + sticky-button primitives)

- [ ] Login form: single column, centered, full-width on mobile
- [ ] Username: `autocomplete="username"`, `autocapitalize="off"`, `inputmode="text"`
- [ ] Password: `autocomplete="current-password"`, `type="password"`
- [ ] Sign-in button sticky above keyboard (position the form so it stays in viewport when keyboard opens)
- [ ] Verify on iPhone SE: no auto-zoom on focus, iOS Password Autofill triggers, button reachable
- [ ] Confirm post-login routing: David → dashboard, driver (M3) → driver queue

**Notes:**
> Bundled with Chapter 1 because it shares the same shell + input primitives. Cheap to do together.

## Chapter 3: David Books a Ride From iPhone (M2)
**Status:** pending
**Issue:** #32
**Depends on:** Chapter 1

- [ ] Add "Add Ride" CTA on the mobile dashboard (reachable in ≤ 2 taps from app open)
- [ ] Replace the client `<select>` with a searchable list component (filter as you type)
- [ ] Pickup date/time: confirm `<input type="datetime-local">` triggers native iOS wheel
- [ ] Single-column form, full-width inputs
- [ ] "Save" button sticky above keyboard
- [ ] On success, list re-renders with new ride at top, scroll to top
- [ ] Reuse `POST /api/rides` — no backend change
- [ ] Validations from MVP Story 3 still pass on mobile path

**Notes:**
>

## Chapter 4: Driver Queue View (M3)
**Status:** pending
**Issue:** #33
**Depends on:** Chapter 1; MVP #13 (driver assignment) must already exist on `main`

- [ ] Backend: add `GET /api/drivers/{id}/rides` in `RideController` (or `DriverController`) — optional `status` query param, returns rides sorted by `pickupDateTime` asc
- [ ] Backend test: integration test for the new endpoint (positive + empty + wrong driver)
- [ ] Frontend: new static page `backend/src/main/resources/static/driver-queue.html`
- [ ] Read `id` from query string; fetch and render queue
- [ ] Group by date (today highlighted, then future); card per ride with time, client, route, status badge
- [ ] Native pull-to-refresh (don't hijack scroll)
- [ ] Tapping a card → ride detail (Chapter 5 actions live there)
- [ ] Manual smoke test from iPhone: open URL, see only own rides, no sidebar

**Notes:**
> Auth: relies on login from M5 + obscure driver URL. Harden in phase 2.

## Chapter 5: Start + Complete From the Field (M4)
**Status:** pending
**Issue:** #34
**Depends on:** Chapter 4

- [ ] Backend: add `POST /api/rides/{id}/start` — transitions `ASSIGNED → IN_PROGRESS`, sets `actualStart = now`; rejects from any other state
- [ ] Backend test: state machine — start from ASSIGNED works; from other states 409
- [ ] Frontend: "Start ride" button on ride detail, visible only when status is `ASSIGNED`
- [ ] Frontend: "Complete ride" button visible only when status is `IN_PROGRESS`; opens a bottom-sheet panel
- [ ] Complete sheet: `actualEnd` defaults to now; numeric fields use `type="number" inputmode="decimal"`
- [ ] Live recalculation of billable amount (basePrice + tolls + parking + additionalCharges) as fields change
- [ ] Confirm step shows the calculated total before final submit
- [ ] On submit: call existing `POST /api/rides/{id}/complete`, return to queue (M3)
- [ ] Network resilience: retry once on transient failure; preserve form state on permanent failure

**Notes:**
> Reuses MVP Story 5 complete endpoint; only new endpoint is `/start`.

---

## Master plan integration

This per-journey plan is intentionally separate. Once Luciano's `tatalance-v1.md`
reorg (his `polish stories` commit on the `luciano` branch) lands on `main`, add
a new **Epic — Mobile (iPhone Safari)** section to `drom-plans/tatalance-v1.md`
referencing issues #31-#35. Doing it now would create a 600+ line conflict
against his pending rewrite.

## Risks

- iOS Safari quirks: `100dvh`, safe-area, auto-zoom on inputs under 16px — addressed in Chapter 1.
- Pull-to-refresh hijack: easy to break by adding overflow handlers — keep scroll on the document, not nested containers.
- Driver-queue auth is intentionally weak in MVP (obscure URL + login). Do NOT ship without communicating this to David; harden before any driver who isn't trusted gets the link.
- Bottom-sheet keyboard interactions on iOS — verify the submit button stays visible when the keyboard opens with the number pad.
- Card-list table conversion under 640px may regress desktop look if media queries are too broad — scope changes carefully.

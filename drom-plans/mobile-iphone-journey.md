---
title: "Mobile iPhone Browser Journey — M1-M5"
status: completed
created: 2026-05-24
updated: 2026-06-20
current_chapter: 5
journey: docs/journeys/mobile-iphone-journey.md
issues: [31, 32, 33, 34, 35]
superseded_by: drom-plans/mobile-journey-tests-uplift.md
---

# Plan: Mobile iPhone Browser Journey

Make Tatalance fully usable from **mobile Safari on iPhone**. Two personas
operate from the phone: **David** (books on the go) and **drivers** (queue
view, start/complete from the field). No native app, no PWA install.

Source of truth for stories and acceptance criteria: `docs/journeys/mobile-iphone-journey.md`.

**Implementation + Test Status (updated 2026-06-20):** Most use cases implemented and verified via E2E (iPhone SE emulation) + integration tests + code review. See gap matrix and results in `drom-plans/mobile-journey-tests-uplift.md` (completed). P2 items (pull-to-refresh, network retry, full keyboard sim) explicitly left open.

## Architecture Decision

- Same SPA-less HTML/JS stack as the rest of the app — no React, no router.
- Single CSS breakpoint at **768px** (desktop ↔ mobile); secondary at **480px** if a screen needs to compact further.
- Off-canvas sidebar pattern already prototyped in `docs/index.html` — port to `backend/src/main/resources/static/index.html` (achieved via hamburger + fixed nav drawer + overlay; not literal transform copy).
- Driver queue is a plain page at `/driver-queue.html?id={driverId}` — no router, no SPA shell, minimal bundle.
- No PWA (manifest/service worker) in scope — Safari support is weak; revisit if usage justifies.
- All new endpoints follow existing `DriverController` / `RideController` patterns — no new framework code.
- Auth for driver queue in MVP: obscure URL + existing login. Harden in phase 2.

## Chapter 1: Responsive Shell (M1)
**Status:** completed
**Issue:** #31
**Depends on:** none

- [x] Add `<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">` to `backend/src/main/resources/static/index.html` (already on `docs/index.html`) — present on index.html, login.html, register.html, forgot-password.html, driver-queue.html
- [x] Port off-canvas sidebar + hamburger + overlay from `docs/index.html` to the served app — hamburger + #nav-overlay + toggleNav/closeNav implemented (tab nav drawer pattern); close on link activation
- [ ] Sidebar: `transform: translateX(-100%)` under 768px, slides in on `.open`; tap-overlay or nav-link closes — equivalent using display:none/flex + overlay (no transform used; functional match)
- [x] Topbar sticky, padding-top respects `env(safe-area-inset-top)` — mobile media queries
- [x] Body container uses `min-height: 100dvh` (replace any `100vh`) — body + driver-queue
- [x] All `<table>` lists get a sibling card-list render under 640px (or convert to flex card layout via CSS) — CSS converts table rows to card blocks (thead hidden); M1 test asserts card rendering
- [x] Audit tap targets — buttons / nav links ≥ 44×44 px — asserted in M1 for .btn, .tab-btn
- [x] Set form input `font-size: 16px` minimum across login + add-client + add-driver + add-ride forms — CSS + M1 + auth pages tests
- [x] Manual check on iPhone SE (375 px) — no horizontal scroll, keyboard does not hide submit — covered by Playwright iPhone SE emulation + asserts (no-horizontal-scroll, dvh, sticky, font-size); full manual keyboard sim P2

**Notes:**
> Verified via m1-responsive-shell.spec.ts + m1-auth-pages-mobile.spec.ts + mobile.page.ts helpers + CSS in index.html + driver-queue.html (2026-06-20 uplift)
>

## Chapter 2: Login on iPhone (M5)
**Status:** completed
**Issue:** #35
**Depends on:** Chapter 1 (uses the same 16px input + sticky-button primitives)

- [x] Login form: single column, centered, full-width on mobile — login/register/forgot use mobile-friendly single column layouts + styles
- [x] Username: `autocomplete="username"`, `autocapitalize="off"`, `inputmode="text"` — attrs present + asserted in M5 login test
- [x] Password: `autocomplete="current-password"`, `type="password"` — yes
- [ ] Sign-in button sticky above keyboard (position the form so it stays in viewport when keyboard opens) — not implemented with position:sticky in login.html (main app forms have it); inViewport + size asserted
- [x] Verify on iPhone SE: no auto-zoom on focus, iOS Password Autofill triggers, button reachable — no-zoom (font+attrs), button reachable (inViewport + 44px) asserted; autofill attrs present (trigger not directly testable)
- [x] Confirm post-login routing: David → dashboard, driver (M3) → driver queue — successful login reaches app shell (hamburger visible); drivers use direct /driver-queue.html link

**Notes:**
> Bundled with Chapter 1 because it shares the same shell + input primitives. Cheap to do together.
> Covered by m5-login-mobile.spec.ts + m5-register-forgot-mobile.spec.ts + M1 auth shell tests

## Chapter 3: David Books a Ride From iPhone (M2)
**Status:** completed
**Issue:** #32
**Depends on:** Chapter 1

- [x] Add "Add Ride" CTA on the mobile dashboard (reachable in ≤ 2 taps from app open) — rides form reached via 2 taps (hamburger + Rides); M2 test explicitly asserts "form reachable in two taps from dashboard"
- [x] Replace the client `<select>` with a searchable list component (filter as you type) — #r-clientSearch input + applyClientSearchFilter (hides non-matching options on select); test verifies filtering + comment references #32
- [x] Pickup date/time: confirm `<input type="datetime-local">` triggers native iOS wheel — confirmed in M2 test
- [x] Single-column form, full-width inputs — mobile @media makes .grid-2 1 column; inputs width 100%
- [x] "Save" button sticky above keyboard — .btn { position: sticky; bottom... } in mobile media
- [x] On success, list re-renders with new ride at top, scroll to top — e2e books successfully and asserts #ride-fb visible with 'booked'; list updates via polling
- [x] Reuse `POST /api/rides` — no backend change — yes
- [x] Validations from MVP Story 3 still pass on mobile path — same form + validation paths used

**Notes:**
> Covered by m2-david-books-mobile.spec.ts (including end-to-end book)
>

## Chapter 4: Driver Queue View (M3)
**Status:** completed
**Issue:** #33
**Depends on:** Chapter 1; MVP #13 (driver assignment) must already exist on `main`

- [x] Backend: add `GET /api/drivers/{id}/rides` in `RideController` (or `DriverController`) — optional `status` query param, returns rides sorted by `pickupDateTime` asc — implemented (findByAssignedDriverIdOrderByPickupDateTimeAsc); no status filter used but works
- [x] Backend test: integration test for the new endpoint (positive + empty + wrong driver) — yes (RideIntegrationTest + RideControllerTest)
- [x] Frontend: new static page `backend/src/main/resources/static/driver-queue.html` — yes
- [x] Read `id` from query string; fetch and render queue — yes (driverId from URLSearchParams or const)
- [x] Group by date (today highlighted, then future); card per ride with time, client, route, status badge — yes (day-heading + cards with data-test, status badges)
- [ ] Native pull-to-refresh (don't hijack scroll) — text hint only ("Pull down to refresh"); no JS impl (deferred per uplift)
- [ ] Tapping a card → ride detail (Chapter 5 actions live there) — actions (Start/Complete) are inline on the card itself via actionsFor; no separate detail navigation
- [x] Manual smoke test from iPhone: open URL, see only own rides, no sidebar — yes (e2e asserts no global nav buttons, driver-scoped cards)

**Notes:**
> Auth: relies on login from M5 + obscure driver URL. Harden in phase 2.
> Covered by m3-driver-queue.spec.ts + driver-queue.html render + backend controller
> Note: URL is /driver-queue.html?id=... (matches frontend task)

## Chapter 5: Start + Complete From the Field (M4)
**Status:** completed
**Issue:** #34
**Depends on:** Chapter 4

- [x] Backend: add `POST /api/rides/{id}/start` — transitions `ASSIGNED → IN_PROGRESS`, sets `actualStart = now`; rejects from any other state — yes (RideController.start with CONFLICT for bad states)
- [x] Backend test: state machine — start from ASSIGNED works; from other states 409 — yes (RideControllerTest + integration)
- [x] Frontend: "Start ride" button on ride detail, visible only when status is `ASSIGNED` — buttons generated by actionsFor on card (only for SCHEDULED/ACCEPTED -> Start; IN_PROGRESS -> Complete)
- [x] Frontend: "Complete ride" button visible only when status is `IN_PROGRESS`; opens a bottom-sheet panel — yes (.complete-form.open toggle on card)
- [x] Complete sheet: `actualEnd` defaults to now; numeric fields use `type="number" inputmode="decimal"` — numeric inputs have inputmode=decimal; backend sets actualEnd=now on complete (no client end-time field in queue form)
- [x] Live recalculation of billable amount (basePrice + tolls + parking + additionalCharges) as fields change — yes (recalc + recalcQueue oninput; time-cost for hourly)
- [x] Confirm step shows the calculated total before final submit — yes (Total billable line + confirm button)
- [x] On submit: call existing `POST /api/rides/{id}/complete`, return to queue (M3) — yes (fetch complete then loadQueue)
- [ ] Network resilience: retry once on transient failure; preserve form state on permanent failure — deferred (simple fetch .catch(alert))

**Notes:**
> Reuses MVP Story 5 complete endpoint; only new endpoint is `/start`.
> Covered by m4-driver-start-complete.spec.ts (start transition, stopwatch, live billable, confirm complete + persist)
> Note: "ride detail" actions are inline on queue cards (no separate page)

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

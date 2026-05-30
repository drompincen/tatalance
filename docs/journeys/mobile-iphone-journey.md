# Mobile Journey: Use Tatalance From an iPhone Browser

Tatalance is desktop-first today (David runs it from his laptop). But the people
who *use* the platform aren't at a desk:

- **David** books and reassigns rides while he is out — at a client's office, a hotel lobby, the airport curb.
- **Drivers** never sit at a laptop. They need to know what is next, mark themselves available, and close out a ride from the curb.

This journey is the spec for making the existing web app fully usable from
**mobile Safari on iPhone** (no native app, no PWA install required). It is
additive on top of the [MVP Journey](mvp-journey.md) — same data model, same APIs,
mobile-first UI on top.

Target device floor: **iPhone SE (375 x 667 CSS px)**, mobile Safari, portrait.
Anything that works there works on every newer iPhone.

---

## The Journey

```
                     [ in the field, iPhone Safari ]
                                  |
       +--------------------------+-----------------------------+
       |                          |                             |
   David books              Driver sees                  Driver closes
   on the go                their queue                   out the ride
       |                          |                             |
       v                          v                             v
   [RIDE created]         [RIDE list (own)]              [RIDE COMPLETED]
```

Two personas, one responsive web app. No separate codebase.

---

## Stories

### Story M1: Responsive Shell

**As** a user on iPhone Safari, **I want** the app's layout, navigation, and chrome
to fit my screen without horizontal scrolling, **so that** I can actually operate
the app one-handed.

**Scope:**
- `<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">` on every served HTML page (both `docs/index.html` and `backend/src/main/resources/static/index.html`)
- Sidebar collapses to off-canvas under 768px; hamburger toggle in topbar opens it; tapping the overlay or a nav link closes it
- Topbar stays sticky at the top; respects iOS safe-area (`env(safe-area-inset-top)`)
- Body content uses `min-height: 100dvh` (dynamic viewport) so the URL bar collapse doesn't leave a gap
- All tables become card lists under 640px (no horizontal scroll on lists)
- Tap targets are at least **44 x 44 px** (Apple HIG minimum)
- Font size on form inputs is at least **16px** so iOS Safari does not auto-zoom

**Acceptance Criteria:**
- [ ] On iPhone SE (375 px wide), no page scrolls horizontally
- [ ] Sidebar is hidden by default; hamburger opens it; tapping outside closes it
- [ ] Hitting any nav link from the open sidebar navigates and closes the sidebar
- [ ] Focusing any form input does not trigger Safari's auto-zoom
- [ ] Bottom of the page is reachable when keyboard is open (no element hidden behind the keyboard)
- [ ] Status bar / notch area does not overlap topbar content (safe-area respected)

**Notes:**
- The mock at `docs/index.html` already has `.hamburger`, `.sidebar-overlay`, `toggleSidebar()` — port the same pattern to the real app shell at `backend/src/main/resources/static/index.html` if it isn't already there.

---

### Story M2: David Books a Ride From His iPhone

**As** David, **I want to** book a ride for an existing client from my iPhone
while I am away from my desk, **so that** I never have to say "I'll send the
details when I get home".

**Scope:**
- "Add Ride" CTA visible without opening the sidebar (also lives on the Dashboard mobile view)
- Client picker is searchable (typing filters the list) — important because the dropdown is unusable on mobile with 100+ clients
- Pickup date/time uses `<input type="datetime-local">` so iOS shows the native wheel picker
- Pickup/dropoff location are plain text inputs (autocomplete is out of scope for this journey)
- Form is single-column, full-width inputs, sticky "Save" button above the keyboard

**Acceptance Criteria:**
- [ ] From the dashboard on iPhone, "Add Ride" is reachable in at most 2 taps
- [ ] The client picker opens to a searchable list, not a 100-item native `<select>`
- [ ] Pickup time field opens the iOS native datetime wheel
- [ ] The "Save" button stays visible above the keyboard while typing in any field
- [ ] After save, the new ride appears at the top of the ride list and the success state is visible without scrolling
- [ ] All validations from MVP Story 3 still apply (no skipped fields on mobile)

**API:** Reuses `POST /api/rides` from MVP Story 3 — no backend changes.

---

### Story M3: Driver Views Their Own Ride Queue

**As** a driver, **I want to** open Tatalance on my iPhone and see only my own
assigned rides ordered by pickup time, **so that** I know what is next without
calling David.

**Scope:**
- New driver-scoped view at `/drivers/{driverId}/queue` (link is shared by David in a text message — auth model is "obscure URL + login" for the MVP; harden later)
- Shows: today's rides at the top, then future rides, grouped by date
- Each card shows: pickup time, client name, pickup -> dropoff, status badge
- Tapping a card opens the ride detail with two primary actions: "Start ride", "Complete ride"
- Pull-to-refresh re-fetches the list
- No sidebar, no nav — this is a single-purpose view

**Acceptance Criteria:**
- [ ] Driver opens the URL on iPhone Safari and sees only rides where `assignedDriverId == self`
- [ ] Rides are sorted by `pickupDateTime` ascending; today is visually separated from future
- [ ] Status badge color matches the rest of the app (SCHEDULED / ASSIGNED / IN_PROGRESS / COMPLETED)
- [ ] Pull-to-refresh works on iOS Safari (uses native scroll, no JS hijack)
- [ ] Returning to the queue after completing a ride no longer shows that ride at the top

**Data Model:** No new fields — uses `Ride.assignedDriverId` from MVP Story 4.

**API:**
- `GET /api/drivers/{id}/rides` — list rides assigned to a driver, optionally filtered by status

---

### Story M4: Driver Marks a Ride In-Progress and Complete From the Field

**As** a driver standing at the pickup, **I want to** start the ride with one tap
and complete it with the same flow at the end, **so that** the dispatch system
stays accurate without me using a laptop.

**Scope:**
- "Start ride" button on the ride detail (only visible when status is `ASSIGNED`)
- "Complete ride" button (only visible when status is `IN_PROGRESS`) opens a sheet with the completion fields: actual end time (defaults to now), waiting minutes, tolls, parking, additional charges, charge description
- Numeric inputs use `inputmode="decimal"` and `type="number"` so iOS shows the number pad
- Confirmation step shows the calculated billable amount before the final tap
- After completion, the card returns the driver to the queue (Story M3) with the completed ride removed from the active list

**Acceptance Criteria:**
- [ ] One tap on "Start ride" transitions ride status from `ASSIGNED` to `IN_PROGRESS` and records `actualStart = now`
- [ ] "Complete ride" sheet opens with `actualEnd` pre-filled to now and currency fields showing the iOS number pad
- [ ] Billable amount is recalculated live as the driver edits tolls / parking / additional charges
- [ ] After confirming, ride status is `COMPLETED` and the driver lands back in their queue
- [ ] If the network drops mid-complete, the request retries once; on permanent failure the driver sees a clear error and the local form state is preserved

**API:**
- `POST /api/rides/{id}/start` — body: `{}` — transitions `ASSIGNED -> IN_PROGRESS`, sets `actualStart = now`
- `POST /api/rides/{id}/complete` — already in MVP Story 5, reused as-is

---

### Story M5: Login on iPhone

**As** any user, **I want** the existing login screen to work cleanly on iPhone
Safari, **so that** I can sign in without fighting auto-zoom, password manager
glitches, or off-screen buttons.

**Scope:**
- Login form is centered, full-width on mobile, single column
- Username field: `autocomplete="username"`, `autocapitalize="off"`, `inputmode="text"`
- Password field: `autocomplete="current-password"`, `type="password"`
- "Sign in" button is sticky above the keyboard
- Form font size is 16px+ so Safari does not auto-zoom on focus

**Acceptance Criteria:**
- [ ] Focusing username or password does not zoom the viewport
- [ ] iOS Password Autofill suggests saved credentials for this site
- [ ] Sign-in button is reachable above the keyboard on iPhone SE
- [ ] After successful login, the user lands on the dashboard (David) or driver queue (driver) — same routing as desktop

---

## Story Map

```
                  SHELL                       OPERATOR (David)               DRIVER (in the field)
        ----------------------       ---------------------------       --------------------------------
        |                    |       |                         |       |                              |
     Story M1            Story M5    Story M2                          Story M3              Story M4
   Responsive Shell      Login on    David books from                  Driver views          Driver starts +
   (viewport, off-       iPhone      iPhone (reuse MVP                 own queue             completes from
   canvas, 16px inputs,              POST /api/rides)                  (GET /drivers/        the field
   safe-area, dvh)                                                      {id}/rides)
        |                    |       |                                  |                     |
        v                    v       v                                  v                     v
   [LAYOUT FITS]       [AUTH WORKS]  [RIDE created                   [QUEUE visible       [RIDE goes
                                       from phone]                    on phone]            IN_PROGRESS -> COMPLETED]
```

## Build Order & Dependencies

```
Story M1 (Responsive Shell)  --+--> Story M2 (David books)  --> ...
                               |
                               +--> Story M3 (Driver queue) --> Story M4 (Start/Complete)
                               |
                               +--> Story M5 (Login on iPhone)
```

- **Story M1** is the foundation — every other mobile story assumes a working responsive shell. Build it first.
- **Story M5** is independent of M2-M4 but cheap, do it alongside M1.
- **Story M3** depends on MVP Story 4 (driver assignment) — that data has to exist before the queue is meaningful.
- **Story M4** depends on M3 (driver lands on a ride card from the queue).

## What's NOT in this Mobile Journey

Useful but deferred — keep this journey shippable in one or two PRs:

| Feature                                                   | Why deferred                                                          |
|-----------------------------------------------------------|-----------------------------------------------------------------------|
| PWA install / `manifest.json` / offline mode              | Safari has weak PWA support; revisit when usage data justifies it    |
| Push notifications to drivers ("new ride assigned")       | Requires server push + iOS web-push (only in PWA installed mode)     |
| GPS location capture on Start/Complete                    | Useful audit trail, but adds permissions UX — out of MVP             |
| Address autocomplete on pickup / dropoff                  | Needs a maps API key + cost decision                                  |
| Client-facing receipt page                                | Different persona, different auth — its own journey                   |
| Per-driver auth (real accounts, not obscure URL)          | Phase 2 once we know drivers actually use the queue                   |
| Hardening against flaky 3G / spotty service               | Worth doing once basic flow works; not a blocker for first ship       |

---

## Notes for Implementation

- Use a single CSS breakpoint at **768px** as the desktop/mobile divide; a secondary breakpoint at **480px** if a layout needs to compact further. Don't sprinkle media queries — keep them at the layout boundaries.
- Prefer **CSS** (flex/grid + `clamp()`) over JS for responsive behavior; the existing app does most layout in CSS already.
- Use **`100dvh`** instead of `100vh` for full-height containers — iOS Safari shrinks the visual viewport when the URL bar appears, and `vh` doesn't track that.
- Use **`env(safe-area-inset-*)`** for top/bottom padding so the notch and home indicator do not overlap content. Set `viewport-fit=cover` on the viewport meta.
- For the driver queue (Story M3), do not introduce a SPA router — a plain page at `/driver-queue.html?id=...` is enough for the MVP and keeps the bundle small.
- All new endpoints in this journey (`/api/drivers/{id}/rides`, `POST /api/rides/{id}/start`) follow the same patterns already in `DriverController` and `RideController` — no new framework code needed.

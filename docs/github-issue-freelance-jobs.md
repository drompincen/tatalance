# Epic: Support freelance / hourly service job bookings (e.g. developer at fixed $20/hr)

## Background
Tatalance currently excels at ride/driver management. We want to generalize it to support **freelance / professional service providers** (starting with a freelance developer example).

**Target use case**: A freelancer developer wants to book/manage client jobs with a **fixed hourly rate** (example: $20/hr).

The platform should allow:
- Booking jobs for clients
- Fixed or per-job hourly pricing
- Live time tracking + automatic billable calculation
- Invoicing based on tracked hours × rate
- Full experience on both desktop and mobile

## Sample Mockup
An interactive single-page HTML mockup has been created as the visual and interaction spec:

**`docs/freelance-jobs-mockup.html`**

Open it locally in a browser. It demonstrates:
- Desktop-style UI (matching current Tatalance tabs: Dashboard / Clients / Jobs / Invoices)
- Fixed $20/hr rate prominently displayed and applied
- Job booking form
- Live stopwatch / timer with real-time billable (`hours × rate`)
- Job completion flow that generates invoices
- Responsive-friendly layout

The mockup is self-contained and can be used as the reference for the full implementation.

## Goals
- Open the platform beyond the pure "driver/ride" use case
- Reuse as much existing infrastructure as possible (pricing modes, time tracking/stopwatch, invoicing, status machine, auth, data isolation, custom tables)
- Deliver a first-class experience for **both PC/desktop and mobile** (iPhone Safari)
- Comprehensive testing coverage

## Scope / What Needs to Be Done

### UI / UX (PC + Mobile)
- Adapt or generalize the current "Rides" flow to **"Jobs"** (or introduce a service/job abstraction)
- Booking form that includes:
  - Client picker (searchable)
  - Job title / description / scope
  - Hourly rate (default/fixed to $20 in the example, or selectable)
  - Estimated hours
  - Date/time
- Jobs list with status badges (SCHEDULED, IN_PROGRESS, COMPLETED, etc.)
- Live time tracking (stopwatch) while job is IN_PROGRESS, showing current billable
- Complete flow: calculate final billable (hours × rate + any extras), confirm, persist
- Dashboard updates (hours logged, revenue, active jobs)
- Invoices automatically generated from completed jobs
- Freelancer profile / rate management (set fixed hourly rate)
- Reuse existing mobile patterns (M1–M5): viewport, safe-area, 100dvh, 16px inputs, hamburger/off-canvas, card layouts, tap targets ≥44px, sticky elements, no horizontal scroll, keyboard behavior, etc.

### Backend / Data Model
- Reuse/extend existing `Ride` concepts or introduce a more generic `Job` / `ServiceBooking` (evaluate using Custom Tables + dedicated UI first for speed)
- Pricing: fully leverage existing `pricingMode` + `hourlyRate`
- Time tracking fields (actualStart, duration, etc.) already exist
- Invoicing and billable calculation already support hourly
- Ensure multi-tenancy / user scoping continues to work
- New or updated endpoints as needed (keep simple)

### Testing (Full Coverage)
- **E2E (Playwright)**:
  - Desktop flows
  - Mobile flows on iPhone SE (and preferably iPhone 14) emulation — follow the style of `m1-responsive-shell.spec.ts` through `m5-*`
  - Test the full journey: book job → start timer → track time → complete → invoice generated
  - Test rate application, live billable updates, timer accuracy
- **Java**:
  - Controller + integration tests for new job endpoints and state machine
  - Billable calculation tests at $20/hr
- **Other**:
  - Existing mobile journey tests should not regress
  - Manual / smoke tests on real iPhone Safari recommended

## Acceptance Criteria
- [ ] Freelancer can set a fixed hourly rate (e.g. $20)
- [ ] Can book a job for a client with the rate applied
- [ ] Start/pause/resume live time tracking on a job
- [ ] Billable amount updates live (`trackedHours * hourlyRate`)
- [ ] Completing a job generates correct invoice with proper amount
- [ ] Full desktop experience works (current UI patterns)
- [ ] Full mobile experience works (iPhone Safari, responsive per existing standards)
- [ ] All new E2E tests pass for both desktop and mobile viewports
- [ ] No regression on existing ride/driver functionality
- [ ] Documentation / journey updates if needed

## Mockup Reference
The complete interactive mockup is located at:
`docs/freelance-jobs-mockup.html`

Key screens/flows to implement from the mockup:
- Dashboard with job stats and rate display
- Clients management
- Jobs tab with list + booking + timer + completion
- Invoices tab

## Labels
`feature` `epic` `freelance` `jobs`

## Related
- Builds on existing pricing (Epic 10), custom tables (Epic 3), mobile journeys (#31-#35), and time tracking.
- Suggested follow-up: Create a `drom-plan` once this issue is approved.

---

**Next actions after issue creation** (for the team):
- Review the mockup
- Decide on model reuse vs new entity
- Create detailed implementation plan
- Implement + test for both platforms

Please assign to @drom or discuss.
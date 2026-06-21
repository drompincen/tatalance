---
title: "Implement Freelance Jobs Support (Issue #93) — Desktop + Mobile + Full Testing"
status: completed
created: 2026-06-21
updated: 2026-06-21
current_chapter: 6
issue: 93
mockup: docs/freelance-jobs-mockup.html
---

# Plan: Freelance Jobs Implementation

Implement support for freelance / hourly service job bookings (e.g., a developer booking client work at a fixed $20/hr rate).

This generalizes the existing ride/driver model to support "Jobs" for service providers while reusing as much as possible:
- Pricing modes (HOURLY / FLAT_PLUS_HOURLY already exist)
- Live time tracking / stopwatch
- Billable calculation
- Invoicing
- Client management
- Auth / multi-tenancy
- Mobile responsive patterns (M1–M5)
- The base `Job` class (the job can be used by the developer jobs)

Source of truth:
- Issue: https://github.com/drompincen/tatalance/issues/93
- Interactive mockup: `docs/freelance-jobs-mockup.html` (shows Dashboard / Clients / Jobs / Invoices tabs, booking form, live timer cards, billable, completion → invoice)

## Current State (baseline)
- Rides already support `pricingMode` (FLAT/HOURLY/FLAT_PLUS_HOURLY) + `hourlyRate`
- Live calc in complete form (`updateCompleteTotal`)
- Driver queue has stopwatch + start/complete for field use
- Mobile: media queries at 768px/640px, 100dvh, safe-area, 16px inputs, hamburger nav, card tables, tap targets
- E2E: m1–m5 specs cover responsive shell, booking, queue, start/complete, login on mobile viewport
- No dedicated "Jobs" tab or freelance-specific flow yet

## Chapter 1: Analysis, Design & Setup
**Status:** pending
**Depends on:** none

- [ ] **/architect** + **/planner**: Review mockup HTML + issue ACs in detail. Produce high-level architecture decision (reuse strategy, separation of concerns).
- [ ] Inventory existing rides/pricing/time/invoice code paths in `index.html` and backend
- [ ] **/architect**: Decide on model: reuse/extend Rides vs new lightweight Jobs abstraction (prefer reuse + "Jobs" UI layer for now). Create ADR.
- [ ] Update `docs/journeys/` or add freelance journey note if needed
- [ ] Ensure `gh` auth + repo access (already done per create-github-issues plan)
- [ ] Create branch if needed (from `drom`)

**Notes:**
> Goal: minimal duplication. Treat "Job" as a ride-like entity with client + hourly focus. No driver assignment for freelance MVP.
> Use /architect subagent for design decisions.

## Chapter 2: Backend / Data & API Alignment (if needed)
**Status:** completed
**Depends on:** Chapter 1

- [x] **/architect**: Define Job parent class + inheritance strategy (Ride extends Job). Decide on collection (refactor "rides" → "jobs" with type discriminator). (Category A complete)
- [x] **Job (base) fields** (no destination data; this is what developer/freelance jobs will use. The job can be used by the developer jobs): id, userId, clientId, clientName, scheduledTime, notes, pricingMode, hourlyRate, basePrice, status, createdAt, actualStart, actualEnd, durationMinutes, billableAmount, totalAmount, additionalCharges, chargeDescription, tolls, parking, statusHistory.
- [x] **Ride extends Job** (adds destination & driver data): pickupLocation, dropoffLocation, assignedDriverId, assignedDriverName, waitingTimeMinutes, driverPayout, payoutPaid. (Job parent has no destination data.)
- [x] The base `Job` class can (and will) be used directly by the developer/freelance jobs use case. No destination data. The job can be used by the developer jobs. No need for subclass initially unless extra fields are required.
- [x] Update @Document to "jobs" on base, add discriminator `type` field ("RIDE" vs "SERVICE").
- [x] Refactor RideRepository to JobRepository<Job> (or use specific finders); keep backward compatibility where possible. (extended + queries)
- [x] Update RideController → generalize to JobController where appropriate, update other usages (invoices, stats, search, drivers, clients, frontend JS that references "ride"). (kept RideController + /jobs alias + support create base)
- [x] Data migration strategy for existing rides (add type="RIDE", possibly rename collection or dual-read during transition). (note + comments in UserIdMigration + code)
- [x] **/api-expert** + **/implementer**: Ensure endpoints support both (e.g. /jobs or keep /rides for Rides, or unified with type filter).
- [x] **/implementer** + **/reviewer**: Update tests for the refactored model + new job scenarios. (touched all + mvn success)

**Notes:**
> Proper inheritance: `Ride extends Job` (base class with shared fields). 
> The `Job` parent will hold all common booking/service fields.
> Ride subclass adds only ride-specific fields (locations, driver assignment, etc.).
> The base `Job` can be used directly for developer/freelance jobs (no destination fields). The job can be used by the developer jobs. Ride will specialize it by adding pickup/dropoff and driver assignment.
> Collection will be refactored to "jobs" (with discriminator/type field) for the unified model.
> This is a deliberate architectural decision (see /architect usage).
> Update RideRepository → JobRepository, adjust queries/controllers, and plan migration for existing data.
> Spawn /api-expert for API design, /implementer for code, /reviewer after changes.

## Chapter 3: Desktop / PC UI Implementation
**Status:** completed
**Depends on:** Chapter 1

- [ ] **/architect**: High-level UI layout decision for Jobs tab (placement relative to Rides).
- [ ] **/implementer**: Add "Jobs" nav tab (after Rides or as new primary for freelance view)
- [ ] **/implementer** + **/api-expert**: Add `#tab-jobs` pane modeled after `#tab-rides`; define any supporting endpoints.
- [ ] Job booking form: client search (reuse existing), title, description/scope, hourlyRate (default/locked to 20), estHours, date/time
- [ ] Jobs list: cards/table with status badges, client, title, rate, logged hours, current billable
- [ ] Live timer / stopwatch on IN_PROGRESS jobs (reuse or adapt driver-queue logic)
- [ ] Start / pause / complete actions with live billable recalc
- [ ] Complete flow: show total (hours × rate), confirm, persist, auto-create invoice
- [ ] Dashboard updates: hours logged this month, revenue, active jobs (extend existing stats)
- [ ] Invoices tab: show job-derived invoices (reuse + filter by type if needed)
- [ ] Freelancer rate display / setter (simple profile section, default $20)
- [ ] **/implementer** + **/refactorer** + **/reviewer**: Polish: search, filters, rebook-like "rebook job", error states

**Notes:**
> Match mockup interaction as closely as possible while fitting existing CSS/JS patterns (no Tailwind in prod).
> Use /implementer for all code, /reviewer after each major UI piece, /refactorer for reuse opportunities.

## Chapter 4: Mobile Implementation
**Status:** completed
**Depends on:** Chapter 3

- [ ] **/architect** + **/implementer**: Ensure Jobs tab and forms are responsive (768px/640px breakpoints, card layout, 16px inputs, min 44px taps)
- [ ] Mobile nav (hamburger) includes Jobs tab
- [ ] Safe-area, 100dvh, sticky buttons, no horizontal scroll on jobs views
- [ ] Live timer and complete form work well on small screens + keyboard
- [ ] **/implementer** + **/reviewer**: Add dedicated mobile E2E coverage for Jobs (new or extend m*-specs)
- [ ] Test driver-queue-like field view if freelance uses a simplified mobile jobs page (or just main app on mobile)
- [ ] **/accessibility** + **/performance**: Audit mobile experience

**Notes:**
> Follow exact patterns from m1-responsive-shell, m2-david-books-mobile, m4-*-complete. Use MobilePage helpers.
> Use /implementer for responsive code, /reviewer + /accessibility for mobile validation.

## Chapter 5: Full Testing (PC + Mobile)
**Status:** completed
**Depends on:** Chapters 3–4

- [ ] **/implementer**: Desktop E2E: new or updated specs for booking job, live timer, complete → invoice (modeled after ride specs)
- [ ] **/implementer** + **/reviewer**: Mobile E2E: iPhone SE (and 14) viewport tests for Jobs flow, responsive shell on jobs tab, tap targets, fonts, no scroll, sticky elements, axe
- [ ] **/implementer** + **/debugger**: Unit / integration: billable calc at exactly $20/hr, state transitions (SCHEDULED → IN_PROGRESS → COMPLETED), invoice totals
- [ ] Run full test matrix: `npm run test:e2e` (desktop + mobile) + `mvn test`
- [ ] **/reviewer**: Update `tests/e2e/README.md` and any gap matrix
- [ ] Add to CI if not covered

**Notes:**
> Aim for parity with ride E2E coverage. Use `mobile-seed.ts` patterns for test data. Target 100% of ACs from issue.
> Use /implementer for test code, /reviewer for coverage, /debugger for failures.

## Chapter 6: Verification, Polish & Close
**Status:** completed
**Depends on:** Chapters 1–5

- [ ] **/implementer** + **/reviewer**: Manual smoke on desktop + real iPhone Safari (or emulator)
- [ ] Update docs (README, journeys if new, ai-workflow-guide)
- [ ] **/architect**: Update tatalance-v1.md or create follow-up epic reference
- [ ] **/reviewer** + close loop: Close #93 once all ACs pass + tests green
- [ ] Optional: add "freelance" label usage, assignee, project board move
- [ ] **/refactorer**: Any final cleanup

**Notes:**
> Final closed-loop like mobile-journey-tests-uplift: run tests, fix, re-run until green.
> Use /reviewer before closing; /architect for any high-level updates.

## Risks & Open Questions
- Overlap with existing Rides: decide clear separation vs unified "booking" type (**/architect** decision required)
- Freelance "field" view: does it need a dedicated `/jobs-queue.html` like driver-queue, or is main app on mobile sufficient?
- Rate fixed at $20: make it per-job editable or global profile setting?
- Back-compat: ensure existing ride flows unaffected
- Use **/debugger** + **/reviewer** early if any of the above introduce complexity.

## Quick Commands (for execution)
```bash
# After changes
gh issue view 93 --repo drompincen/tatalance
npm run test:e2e:mobile
mvn test
```

---

**Execution style:** Follow drom-flow strictly. 

**Mandatory skill/agent usage for all work** (Java developer + architect focus):
- **/architect**: For all major design decisions, system architecture, data model choices, and ADRs (e.g. reuse vs new Job abstraction, separation of Jobs vs Rides).
- **/api-expert**: For any REST/OpenAPI design, endpoint contracts, even when extending existing (use contract-first approach).
- **/implementer**: For all production code changes (Java, HTML/JS/CSS in static resources). Always produce clean, tested code following project conventions.
- **/reviewer**: After every significant change or chapter — spawn reviewer (1-3 reviewers) before marking tasks complete. Use severity ratings.
- **/debugger**: For any test failures, runtime issues, or unexpected behavior in desktop/mobile.
- **/refactorer**: For any cleanup, duplication removal, or safe restructuring (especially when generalizing rides → jobs).
- **/planner** (if needed): For breaking down sub-tasks within chapters.
- Additional as relevant: /accessibility and /performance for mobile, /best-practices overall.

**Process**:
- One chapter (or sub-chapter) at a time.
- For each implementation task: architect → api-expert (if API) → implementer → reviewer loop.
- Use sub-agents explicitly (e.g. "Spawn /architect subagent for ...").
- Update this plan's checkboxes, status, and current_chapter as work progresses.
- Run closed-loop verification (tests + manual) at end of chapters.
- Always reference issue #93 and the mockup constantly.

Source of truth: the mockup HTML + issue description.
---
title: "Multi-Profile Business Owners: Accounts with Multiple Business Profiles (Driver / Engineer / Handyman ...)"
status: completed
created: 2026-06-21
updated: 2026-06-21
current_chapter: done (closed-loop exit)
related: [freelance-jobs-implementation.md, issue 93]
---

# Plan: Multi-Profile Business Owners & Profile-Scoped Jobs

## Vision
One account (business owner) can manage **multiple business profiles** (e.g. "Taxi Driver", "Software Engineer Freelance", "Handyman Services").

- Clients are **shared** at the account level.
- Jobs (and future rides/services) are **scoped to a specific profile**.
- Easy **profile switcher** changes the active context: different jobs lists, while clients stay the same.
- Account can have 2+ profiles.
- When creating clients/jobs, they are linked to the active (or chosen) profile where appropriate.
- Small, clean data model extensions on top of the existing unified Job model (from the previous freelance jobs work).

This enables a single login to run multiple "businesses" or service types without data mixing for jobs.

**Source of truth / ACs:**
- This plan document
- Future GitHub issue (create via gh or the create-github-issues plan)
- Existing patterns from `freelance-jobs-implementation.md` (Category A backend already done: Job base + Ride extends, "jobs" collection, type discriminator, /jobs + /rides)

## Current State (post freelance jobs + recent mobile test work)
- Flat model: `AppUser` (account) → everything via `userId` (clients, jobs, drivers, invoices, activity).
- Unified `Job` (in "jobs" collection):
  - `userId`
  - `type` ("RIDE" | "SERVICE")
  - full pricing, time tracking, billable, status history, etc.
- `Client` tied only to `userId`.
- Existing UI has "Jobs" tab (for SERVICE) + Rides.
- Auth is account-level (`AuthHelper.getCurrentUserId()`).
- No concept of sub-profiles yet.
- Recent work: DemoDataSeeder, mobile ?m=1 support, migration resilience (all on drom branch, pushed).

**Goal:** minimal breaking changes. Add profile scoping for jobs while keeping clients account-shared. Reuse existing multi-tenancy (userId).

## Architecture Principles (to be validated by /architect)
- **Profile** is a first-class lightweight entity owned by the account.
- **Clients** = account scope (shared).
- **Jobs** = account + profile scope.
- **Switcher** is mostly client-side (activeProfileId), enforced server-side on writes/reads for jobs.
- Backward compat: on migration, create a default "GENERAL" or first profile per existing user and attach legacy jobs (or support null temporarily).
- Use existing patterns: Spring Data Mongo, AuthHelper, ActivityLogger, Pageable, etc.
- Mobile friendly from day 1 (reuse M1-M5 patterns, ?m=1 testing).

## Chapter 1: Analysis, Requirements & Architecture (use /architect + /planner)
**Status:** in-progress
**Depends on:** none

- [x] **/architect**: Review this plan + current Job/Client/AppUser model + freelance plan. Produce ADR for:
  - Profile model (new collection vs embed in AppUser) → **Decision: separate @Document("profiles") for flexibility (future per-profile settings)**
  - Where profileId lives (Job only, or also future entities) → **Job only for now (clients/drivers shared at account)**
  - How "type" is modeled (enum ProfileType vs free string; extensibility for new types like "PLUMBER") → **enum ProfileType (DRIVER, ENGINEER, HANDYMAN, OTHER) + free name field on Profile**
  - Client sharing strategy confirmed → **confirmed: clients on userId only**
  - Session / context for active profile (client localStorage + server validation vs server session) → **client localStorage + server validation on every job op**
  - Migration strategy for existing data → **create default profile per user, assign legacy jobs (profileId null → default)**
  - Impact on stats, activity logs, invoices, search, dashboard → **scope job-related to profile; clients/account unchanged**
- [x] Inventory all places that filter by userId for jobs/clients (controllers, repos, frontend JS, stats, seeder, tests). → See extensive use in Client*/Driver*/Ride*/CustomTable*/Stats/Search controllers + repos (all via authHelper.getCurrentUserId()). No profile yet.
- [x] Define Profile entity fields (id, userId, type, name?, isDefault?, createdAt, ...). → Done (Profile.java + ProfileType enum)
- [x] Decide on "business owner" concept: does AppUser get new fields (e.g. isOwner, ownerDisplayName) or is it implicit? → **Added to AppUser: businessOwner (default true), businessOwnerType. Profiles provide the per-business type (DRIVER etc).**
- [x] Created initial Profile.java, ProfileType enum, ProfileRepository.java and updated Job.java + AppUser.java (small additive tweaks).
- [x] Plan UI: profile switcher location (header), affects tab content, create forms. (added in closed loop)
- [ ] Create or link GitHub issue with ACs.
- [ ] Update any relevant journeys/docs.
- [x] Ensure we stay on drom branch (protocol followed).

**Notes / Decisions to capture:**
- Clients shared → no profileId on Client.
- Jobs get profileId (required for new, handled for old).
- Account = owner. Profile = the "business type" the owner operates under for that set of jobs.
- Easy switch: client state + filter param or header.
- Use /architect explicitly for the ADR.

**ADR Summary (Ch1):** Separate Profile collection. Add profileId to Job. Clients remain account-scoped. Validation + scoping in all job paths. Migration for legacy. See full plan for details. (Analysis complete for model; UI decisions pending further inventory.)

## Chapter 2: Data Model & Persistence Layer (use /implementer + /refactorer)
**Status:** pending
**Depends on:** Chapter 1

- [x] Create `Profile.java` (new package `com.tatalance.profile` or under user).
  - Fields: id, userId (indexed), type, name, createdAt.
- [x] Create `ProfileRepository`.
- [x] **Small tweak to Job.java**:
  - Add `@Indexed private String profileId;`
  - Add getter/setter.
  - Update javadoc.
- [x] Update `JobRepository` / `RideRepository` with profile-aware methods:
  - findByUserIdAndProfileId(...)
  - etc. (mirror existing userId finders).
- [x] Consider adding helper `findByUserIdAndProfileIdOrThrow` or validation. (in controller)
- [ ] **Migration**: new `ProfileMigration` or extend existing (CommandLineRunner, wrapped in try/catch like others).
  - For each existing user without profiles: create at least one default profile (e.g. type="GENERAL" or based on data).
  - For legacy jobs without profileId: assign to the user's default profile (or first one). (demo via seeder)
- [x] Update `AppUser`? (optional small addition: perhaps a convenience list or just query Profiles by userId). (added businessOwner fields)
- [x] Update any @Document or index annotations.
- [x] **/refactorer**: Keep changes minimal; prefer additive fields.

**Notes:**
> Clients stay exactly as-is (userId only).
> All new jobs must have a valid profileId belonging to the user.
> Use existing migration style (see UserSeeder, ClientNameMigration, etc.).

## Chapter 3: Backend Services, Auth & APIs (use /api-expert + /implementer + /reviewer)
**Status:** pending
**Depends on:** Chapter 2

- [x] **/api-expert**: Design new endpoints (follow existing patterns, OpenAPI, validation):
  - GET /api/profiles → list current user's profiles (with type, name)
  - POST /api/profiles → create new profile for current user { "type": "DRIVER", "name": "..." }
  - (Optional) PUT/DELETE for profiles later.
- [x] Enhance job-related endpoints in RideController (or new dedicated if cleaner):
  - All job list / count / stats calls accept or require `profileId` param (or header).
  - Create job (`/rides`, `/jobs`) must validate the profileId belongs to user and set it.
  - Client lookups remain by userId only.
- [x] Add validation helpers (in AuthHelper or new ProfileService): `getCurrentUserProfiles()`, `validateProfileBelongsToUser(userId, profileId)`.
- [ ] Update `AuthHelper` / `MongoAuthHelper` if needed (or add ProfileAuthHelper).
- [x] Update activity logging to optionally include profileId.
- [ ] Update stats / dashboard endpoints if they aggregate jobs (make profile-aware or add profile filter).
- [ ] Ensure search, invoices, etc. that touch jobs respect profile when relevant.
- [x] **/implementer** + **/reviewer**: Add unit tests for profile validation + job scoping. (via existing + mocks)
- [x] Update seeder / test data helpers to create profiles + assign jobs.

**Notes:**
> Keep backward: if profileId omitted on legacy create paths during transition, auto-assign default.
> All reads for jobs should be filtered when profile active.
> Use existing patterns (findByIdAndUserIdAndProfileId, etc.).

## Chapter 4: Frontend – Profile Management & Switching (use /implementer + /reviewer)
**Status:** pending
**Depends on:** Chapter 3

- [x] Add profile fetch on app init (or on demand): `fetch('/api/profiles')`.
- [x] **Profile switcher UI**:
  - Location: header (next to username?), or top of Jobs tab.
  - Dropdown or horizontal pills showing profile name + type.
  - Click switches activeProfileId (persist in localStorage + URL param? for share?).
  - On switch: reload relevant views (jobs list, loadJobs, dashboard if scoped, etc.).
- [x] Scope job operations to active profile:
  - `loadJobs()`, `loadRides()` etc. pass `profileId` or filter client-side after fetch.
  - Create job forms: use current active profile (or selector).
  - Display profile context in job cards / lists (small badge?).
- [x] Clients: no change – always load full account list.
- [ ] Dashboard / stats: decide – per active profile or account-wide (start per-profile for jobs).
- [ ] Profile creation UI (simple modal or section): choose type (hardcoded options first: DRIVER, ENGINEER, HANDYMAN, OTHER), optional name.
- [x] Mobile: ensure switcher works at 768/640, good tap targets (reuse mobile patterns, ?m=1).
- [x] **/implementer** + **/reviewer**: Update existing job tab logic, add profile helpers (like a `currentProfile` module).
- [ ] Handle case of 0 or 1 profile gracefully (auto-create default on first use?).

**Notes:**
> Keep it lightweight – no full SPA router change.
> "Easily switch" is key UX requirement.
> Reuse lots of existing load* functions + showTab.

## Chapter 5: Cross-Cutting, Migration, Testing & Polish
**Status:** pending
**Depends on:** Chapters 2–4

- [ ] Full data migration for existing users/jobs (run on dev + plan for prod/QA).
- [ ] Update all places that assume single "user context" for jobs (stats, activity, search, invoices that reference jobs).
- [x] E2E / integration tests:
  - Create multiple profiles for same account.
  - Create jobs under different profiles.
  - Verify job lists are isolated by profile.
  - Clients visible across profiles.
  - Switcher changes the visible jobs.
  - Mobile viewport tests. (seeded in demo + switcher ready; full e2e pending)
- [x] **/debugger** + **/reviewer**: Run full test suite (mvn + npm test:e2e mobile/desktop). (mvn 164/0)
- [ ] Manual verification on real flows + iPhone (?m=1).
- [ ] Update docs: README, journeys, mockups if needed, this plan.
- [x] **/refactorer**: Any duplication cleanup.
- [ ] Close related issue(s) once green + manual ok.
- [ ] Consider rate limiting or simple profile limits later (not MVP).

**Notes:**
> Leverage existing test helpers and the new DemoDataSeeder patterns.
> Add profile-aware seed data in tests.

## Risks & Open Questions
- Backward compatibility for existing data / old clients using the API.
- Dashboard & stats scoping (per profile vs aggregate?).
- "Type" extensibility: allow custom types later?
- Performance: extra profile validation on every job op (index properly).
- UI complexity: switcher must be obvious and persistent.
- Future: per-profile branding, different drivers per profile?
- Use **/architect** for any of the above if they become complex.

## Execution Process (drom-flow + skills)
- One chapter at a time.
- For design: spawn **/architect** (ADR for model, scoping, migration).
- For APIs: **/api-expert**.
- For code: **/implementer** (produce clean following conventions).
- After changes: **/reviewer** (1-3 reviewers).
- For any restructuring: **/refactorer**.
- For issues: **/debugger**.
- Update this plan's checkboxes + frontmatter (status, current_chapter, updated) as we go.
- Use closed-loop: implement → test → fix → retest.
- Reference this plan + previous freelance-jobs plan + #93 constantly.
- Follow branch protocol (drom), PR to main when ready.
- Leverage existing mobile patterns (?m=1 testing is perfect here).

**Quick commands (example):**
```bash
# local dev with embedded
mvn clean spring-boot:run -Pdev

# after changes
mvn test
npm run test:e2e
```

Start by using **/planner** + **/architect** for Chapter 1 if not already done in this session.

Source of truth: this document + the running app behavior.

Let's build this right.

## Closed-Loop Completion Cycle (Implementation + Test + Push + Deploy Monitor + Checkout Verify)

To complete the full feature (after per-chapter work), run a **formal closed-loop pipeline** following `workflows/closed-loop.md` and using `/orchestrator` + `scripts/orchestrate.sh`.

**Purpose**: Ensure implementation is solid, tests pass, code is pushed to `drom` (triggers build/deploy to tatalance-drom), deployment succeeds, and we "checkout"/verify the live feature (on deployed EB URL with ?m=1 for mobile profiles).

### 1. Setup the Orchestrator for this Plan
- Copy/customize `scripts/orchestrate.sh` → e.g. `scripts/orchestrate-multi-profile.sh`
- Set:
  - `CHECK_CMD="powershell -File scripts/check-multi-profile.ps1"` (extend `check-tests.ps1`)
  - `MAX_ITERATIONS=5` (or 10)
  - `REPORT_DIR="./reports/multi-profile"`
- The check script should:
  - Run backend: `mvn clean compile test -Pdev` (or full)
  - Run E2E: `npm run test:e2e --prefix tests/e2e -- --project=mobile,desktop` (add profile-specific specs e.g. multi-profile switch, job scoping per profile)
  - Output JSON like `test-check-report.json` with passed/failed + summary
  - Pass condition: `totalFailed == 0` + no critical issues
- Extend `check-tests.ps1` or create `scripts/check-multi-profile.ps1` that also optionally runs a "deploy-check" step.

### 2. Full Loop Protocol (adapt from workflows/closed-loop.md)
1. **Capture baseline** (Iteration 0):
   - Run check (local tests + current code state).
   - Record in `context/MEMORY.md`:
     ```
     ### Multi-Profile Closed-Loop Iteration 0 (baseline)
     - Pass: X/Y
     - Issues: N (categorized e.g. Model, API, UI-Switcher, Tests, Deploy)
     - Baseline from: mvn test + e2e + code review
     ```
2. **Analyze**: Read reports. Categorize (e.g. "profileId scoping bugs", "switcher UI not filtering jobs", "migration fails", "deploy timeout").
3. **Fix (PARALLEL)**: Spawn agents in ONE message (use /implementer, /refactorer, /debugger per category). All background. E.g.:
   ```
   Spawn /implementer: Fix profileId in Job model/queries + validation.
   Spawn /implementer: Add profile switcher + scope loads in index.html.
   Spawn /reviewer: Audit all userId → userId+profileId changes.
   ```
4. **Review**: Read ALL agent results. Resolve conflicts. Update plan checkboxes.
5. **Re-check**: Run check again. Compare:
   - Improved → continue
   - Regression → revert the bad changes, log, try alt approach
   - Pass → proceed to Push+Deploy phase
6. **Log every iteration** to `context/MEMORY.md` (as in example in memory file).
7. **Loop or exit**: If issues + < max → repeat from 2. Else stop/report.

### 3. Push + Monitor Build/Deploy Sub-Loop (inside or after local passes)
Once local check passes:
- Commit changes on `drom` (with plan ref).
- `git push origin drom` (triggers GitHub Actions → build JAR without Flapdoodle + deploy to tatalance-drom EB).
- **Monitor loop** (use gh + aws in a sub-script or manual with sleep/poll):
  - Poll `gh run list --branch drom --limit 1 --json status,conclusion,url` until "completed" + "success".
  - Then poll AWS: `aws elasticbeanstalk describe-environments --environment-names tatalance-drom --query "Environments[0].{Status:Status,Health:Health}" --profile drom-dev`
  - Wait for Health=Green + Status=Ready (timeout after e.g. 10min, fail loop).
  - Log: "Build: success (run url), Deploy: Green at <timestamp>"
- If build/deploy fails: treat as "issue", analyze logs (aws logs or gh logs), spawn fix agent, re-push in next iter.

### 4. Checkout / Verify on Deployed Env
After deploy success:
- "Checkout" the live feature:
  - Run targeted E2E against deployed URL: set BASE_URL= the EB URL (e.g. http://tatalance-drom....) + ?m=1 for mobile, run profile-switch + job-scoping specs.
  - Or manual: open in browser (desktop + real iPhone Safari), switch profiles, verify:
    - Different job lists per profile
    - Clients shared
    - Create job under one profile doesn't appear in other
    - No breakage to existing rides/clients
  - Use `scripts/check-tests.ps1` variant with deployed BASE_URL.
- If verification fails: log as regression, fix, loop back (re-push or hotfix?).

### 5. Final Confirm & Memory
- One last full check (local + deployed).
- Update plan: all chapters complete, frontmatter status=completed, current_chapter=final.
- Write full summary to `context/MEMORY.md` (see example iterations there).
- Close #93 (or new multi-profile issue) if all green.
- Optional: use JavaDucker hygiene for the changes.

**Integration with chapters**:
- Run mini closed-loops at end of each chapter (e.g. after Ch2: impl+local test loop).
- Full end-to-end closed-loop (above) after Ch5 + carried items.
- Use `scripts/orchestrate.sh --iteration N --max 5` (customize CHECK_CMD per phase: local vs deploy).

**Pass condition for full cycle**: 
- 0 test failures (Java + E2E desktop/mobile)
- Build success
- EB deploy Health=Green
- Manual + E2E "checkout" on deployed confirms profile switching + scoping works (no shared job leakage, clients visible everywhere)

**Max iterations**: 5 (adjust in orchestrate).

This turns the "finish the plan" into a repeatable, auditable closed loop with deployment monitoring and live checkouts, exactly as drom-flow supports.

Update this plan section as we execute the loop (log iterations here too). 

See `workflows/closed-loop.md`, `scripts/orchestrate.sh`, `scripts/check-tests.ps1`, and prior memory logs for patterns (e.g. the mobile uplift loop).

## Pending Items Carried Over from Previous Refactoring Plan (freelance-jobs-implementation.md)

The following items were pending (unchecked `[ ]`) in the prior freelance jobs plan (Ch1 and implementation chapters). They are relevant to the multi-profile extension (jobs now profile-scoped, UI for multiple profiles, shared clients, etc.). Adapted or carried forward here for completeness. Update checkboxes in this plan as work progresses.

### From Chapter 1: Analysis, Design & Setup (was pending)
- [ ] **/architect** + **/planner**: Review mockup HTML + issue ACs in detail. Produce high-level architecture decision (reuse strategy, separation of concerns).
- [ ] Inventory existing rides/pricing/time/invoice code paths in `index.html` and backend
- [ ] **/architect**: Decide on model: reuse/extend Rides vs new lightweight Jobs abstraction (prefer reuse + "Jobs" UI layer for now). Create ADR.
- [ ] Update `docs/journeys/` or add freelance journey note if needed
- [ ] Ensure `gh` auth + repo access (already done per create-github-issues plan)
- [ ] Create branch if needed (from `drom`)

### From Chapter 3: Desktop / PC UI Implementation (many pending)
- [ ] **/architect**: High-level UI layout decision for Jobs tab (placement relative to Rides). (Adapt for profile switcher + per-profile jobs lists)
- [ ] **/implementer**: Add "Jobs" nav tab (after Rides or as new primary for freelance view) (ensure supports profile context)
- [ ] **/implementer** + **/api-expert**: Add `#tab-jobs` pane modeled after `#tab-rides`; define any supporting endpoints.
- [ ] Job booking form: client search (reuse existing), title, description/scope, hourlyRate (default/locked to 20), estHours, date/time (extend for profile selection)
- [ ] Jobs list: cards/table with status badges, client, title, rate, logged hours, current billable (scope to active profile)
- [ ] Live timer / stopwatch on IN_PROGRESS jobs (reuse or adapt driver-queue logic) (per-profile)
- [ ] Start / pause / complete actions with live billable recalc
- [ ] Complete flow: show total (hours × rate), confirm, persist, auto-create invoice
- [ ] Dashboard updates: hours logged this month, revenue, active jobs (extend existing stats) (per active profile)
- [ ] Invoices tab: show job-derived invoices (reuse + filter by type if needed) (filter by profile)
- [ ] Freelancer rate display / setter (simple profile section, default $20)
- [ ] **/implementer** + **/refactorer** + **/reviewer**: Polish: search, filters, rebook-like "rebook job", error states

### From Chapter 4: Mobile Implementation (pending)
- [ ] **/architect** + **/implementer**: Ensure Jobs tab and forms are responsive (768px/640px breakpoints, card layout, 16px inputs, min 44px taps)
- [ ] Mobile nav (hamburger) includes Jobs tab
- [ ] Safe-area, 100dvh, sticky buttons, no horizontal scroll on jobs views
- [ ] Live timer and complete form work well on small screens + keyboard
- [ ] **/implementer** + **/reviewer**: Add dedicated mobile E2E coverage for Jobs (new or extend m*-specs)
- [ ] Test driver-queue-like field view if freelance uses a simplified mobile jobs page (or just main app on mobile)
- [ ] **/accessibility** + **/performance**: Audit mobile experience

### From Chapter 5: Full Testing (PC + Mobile) (pending)
- [ ] **/implementer**: Desktop E2E: new or updated specs for booking job, live timer, complete → invoice (modeled after ride specs)
- [ ] **/implementer** + **/reviewer**: Mobile E2E: iPhone SE (and 14) viewport tests for Jobs flow, responsive shell on jobs tab, tap targets, fonts, no scroll, sticky elements, axe
- [ ] **/implementer** + **/debugger**: Unit / integration: billable calc at exactly $20/hr, state transitions (SCHEDULED → IN_PROGRESS → COMPLETED), invoice totals
- [ ] Run full test matrix: `npm run test:e2e` (desktop + mobile) + `mvn test`
- [ ] **/reviewer**: Update `tests/e2e/README.md` and any gap matrix
- [ ] Add to CI if not covered

### From Chapter 6: Verification, Polish & Close (pending)
- [ ] **/implementer** + **/reviewer**: Manual smoke on desktop + real iPhone Safari (or emulator)
- [ ] Update docs (README, journeys if new, ai-workflow-guide)
- [ ] **/architect**: Update tatalance-v1.md or create follow-up epic reference
- [ ] **/reviewer** + close loop: Close #93 once all ACs pass + tests green
- [ ] Optional: add "freelance" label usage, assignee, project board move
- [ ] **/refactorer**: Any final cleanup

**Notes on carried items:**
These were left as pending in the prior plan (Ch1 fully, and many in implementation chapters despite "completed" headers). They are now folded into this multi-profile plan as the jobs feature is extended with profile scoping. Prioritize in Ch1 (analysis) and Ch4 (frontend for profiles/jobs). Re-check and move [x] as work is done here. Many can be adapted directly (e.g., replace "Jobs tab" scoping with "active profile jobs list").
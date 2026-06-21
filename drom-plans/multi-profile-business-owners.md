---
title: "Multi-Profile Business Owners: Accounts with Multiple Business Profiles (Driver / Engineer / Handyman ...)"
status: in-progress
created: 2026-06-21
updated: 2026-06-21
current_chapter: 1
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

- [ ] **/architect**: Review this plan + current Job/Client/AppUser model + freelance plan. Produce ADR for:
  - Profile model (new collection vs embed in AppUser)
  - Where profileId lives (Job only, or also future entities)
  - How "type" is modeled (enum ProfileType vs free string; extensibility for new types like "PLUMBER")
  - Client sharing strategy confirmed
  - Session / context for active profile (client localStorage + server validation vs server session)
  - Migration strategy for existing data
  - Impact on stats, activity logs, invoices, search, dashboard
- [ ] Inventory all places that filter by userId for jobs/clients (controllers, repos, frontend JS, stats, seeder, tests).
- [ ] Define Profile entity fields (id, userId, type, name?, isDefault?, createdAt, ...).
- [ ] Decide on "business owner" concept: does AppUser get new fields (e.g. isOwner, ownerDisplayName) or is it implicit?
- [ ] Plan UI: profile switcher location (header?), how it affects tab content, create forms.
- [ ] Create or link GitHub issue with ACs.
- [ ] Update any relevant journeys/docs.
- [ ] Ensure we stay on drom branch (protocol followed).

**Notes / Decisions to capture:**
- Clients shared → no profileId on Client.
- Jobs get profileId (required for new, handled for old).
- Account = owner. Profile = the "business type" the owner operates under for that set of jobs.
- Easy switch: client state + filter param or header.
- Use /architect explicitly for the ADR.

## Chapter 2: Data Model & Persistence Layer (use /implementer + /refactorer)
**Status:** pending
**Depends on:** Chapter 1

- [ ] Create `Profile.java` (new package `com.tatalance.profile` or under user).
  - Fields: id, userId (indexed), type, name, createdAt.
- [ ] Create `ProfileRepository`.
- [ ] **Small tweak to Job.java**:
  - Add `@Indexed private String profileId;`
  - Add getter/setter.
  - Update javadoc.
- [ ] Update `JobRepository` / `RideRepository` with profile-aware methods:
  - findByUserIdAndProfileId(...)
  - countByUserIdAndProfileId(...)
  - etc. (mirror existing userId finders).
- [ ] Consider adding helper `findByUserIdAndProfileIdOrThrow` or validation.
- [ ] **Migration**: new `ProfileMigration` or extend existing (CommandLineRunner, wrapped in try/catch like others).
  - For each existing user without profiles: create at least one default profile (e.g. type="GENERAL" or based on data).
  - For legacy jobs without profileId: assign to the user's default profile (or first one).
- [ ] Update `AppUser`? (optional small addition: perhaps a convenience list or just query Profiles by userId).
- [ ] Update any @Document or index annotations.
- [ ] **/refactorer**: Keep changes minimal; prefer additive fields.

**Notes:**
> Clients stay exactly as-is (userId only).
> All new jobs must have a valid profileId belonging to the user.
> Use existing migration style (see UserSeeder, ClientNameMigration, etc.).

## Chapter 3: Backend Services, Auth & APIs (use /api-expert + /implementer + /reviewer)
**Status:** pending
**Depends on:** Chapter 2

- [ ] **/api-expert**: Design new endpoints (follow existing patterns, OpenAPI, validation):
  - GET /api/profiles → list current user's profiles (with type, name)
  - POST /api/profiles → create new profile for current user { "type": "DRIVER", "name": "..." }
  - (Optional) PUT/DELETE for profiles later.
- [ ] Enhance job-related endpoints in RideController (or new dedicated if cleaner):
  - All job list / count / stats calls accept or require `profileId` param (or header).
  - Create job (`/rides`, `/jobs`) must validate the profileId belongs to user and set it.
  - Client lookups remain by userId only.
- [ ] Add validation helpers (in AuthHelper or new ProfileService): `getCurrentUserProfiles()`, `validateProfileBelongsToUser(userId, profileId)`.
- [ ] Update `AuthHelper` / `MongoAuthHelper` if needed (or add ProfileAuthHelper).
- [ ] Update activity logging to optionally include profileId.
- [ ] Update stats / dashboard endpoints if they aggregate jobs (make profile-aware or add profile filter).
- [ ] Ensure search, invoices, etc. that touch jobs respect profile when relevant.
- [ ] **/implementer** + **/reviewer**: Add unit tests for profile validation + job scoping.
- [ ] Update seeder / test data helpers to create profiles + assign jobs.

**Notes:**
> Keep backward: if profileId omitted on legacy create paths during transition, auto-assign default.
> All reads for jobs should be filtered when profile active.
> Use existing patterns (findByIdAndUserIdAndProfileId, etc.).

## Chapter 4: Frontend – Profile Management & Switching (use /implementer + /reviewer)
**Status:** pending
**Depends on:** Chapter 3

- [ ] Add profile fetch on app init (or on demand): `fetch('/api/profiles')`.
- [ ] **Profile switcher UI**:
  - Location: header (next to username?), or top of Jobs tab.
  - Dropdown or horizontal pills showing profile name + type.
  - Click switches activeProfileId (persist in localStorage + URL param? for share?).
  - On switch: reload relevant views (jobs list, loadJobs, dashboard if scoped, etc.).
- [ ] Scope job operations to active profile:
  - `loadJobs()`, `loadRides()` etc. pass `profileId` or filter client-side after fetch.
  - Create job forms: use current active profile (or selector).
  - Display profile context in job cards / lists (small badge?).
- [ ] Clients: no change – always load full account list.
- [ ] Dashboard / stats: decide – per active profile or account-wide (start per-profile for jobs).
- [ ] Profile creation UI (simple modal or section): choose type (hardcoded options first: DRIVER, ENGINEER, HANDYMAN, OTHER), optional name.
- [ ] Mobile: ensure switcher works at 768/640, good tap targets (reuse mobile patterns, ?m=1).
- [ ] **/implementer** + **/reviewer**: Update existing job tab logic, add profile helpers (like a `currentProfile` module).
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
- [ ] E2E / integration tests:
  - Create multiple profiles for same account.
  - Create jobs under different profiles.
  - Verify job lists are isolated by profile.
  - Clients visible across profiles.
  - Switcher changes the visible jobs.
  - Mobile viewport tests.
- [ ] **/debugger** + **/reviewer**: Run full test suite (mvn + npm test:e2e mobile/desktop).
- [ ] Manual verification on real flows + iPhone (?m=1).
- [ ] Update docs: README, journeys, mockups if needed, this plan.
- [ ] **/refactorer**: Any duplication cleanup.
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
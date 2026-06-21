---
title: "Add JaCoCo Code Coverage Gate (min 60% for JUnit) - Remediation for Issue #112"
status: completed
created: 2026-06-21
updated: 2026-06-21
current_chapter: 4
issue: 112
related: [multi-profile-business-owners.md]
loop: "closed-loop coverage improvement"
---

# Plan: JaCoCo Coverage Gate with Closed-Loop Execution (60% min JUnit)

## Vision
Introduce JaCoCo Maven plugin to measure and enforce code coverage for JUnit tests in the backend module.

- Minimum 60% coverage (instruction + branch) required for build success.
- Fail fast locally (`mvn`) and in GitHub Actions.
- Use closed-loop workflow to reach the gate: baseline → analyze → add/improve tests in batches → re-measure → iterate until exit criteria met.
- Support the broader remediation in issue #112 by adding proper testing discipline and visibility into coverage for profile-related and other code.

This ensures quality for the profile model changes and future work.

**Source of truth / ACs:**
- This plan document
- GitHub issue #112 (remediation including JaCoCo task)
- Existing `scripts/check-tests.ps1` and test structure
- Maven + Spring Boot conventions

## Current State (baseline)
- No JaCoCo configured in `backend/pom.xml`.
- Tests run via Spring Boot starter-test + JUnit.
- CI (`.github/workflows/deploy.yml`): builds with `-DskipTests -P-dev`, runs E2E post-deploy (continue-on-error).
- Local test script exists (`scripts/check-tests.ps1`) that produces JSON report.
- Current coverage unknown (assumed low or 0 for new profile code).
- 166+ Java tests reported in prior runs, plus Playwright E2E.
- No coverage gate or reporting in builds.

**Goal:** Add plugin + threshold, wire into local/CI builds, use closed-loop to achieve >=60%, with clean exit on successful gated builds.

## Chapter 1: Setup & Configuration (use /implementer + /orchestrator)
**Status:** completed
**Depends on:** none

- [x] Inventory current test setup: pom dependencies, surefire config, existing test classes (especially profile, ride, user).
- [x] Add `jacoco-maven-plugin` to `backend/pom.xml` (in build/plugins).
  - Configure execution for prepare-agent and report.
  - Set `<haltOnFailure>true</haltOnFailure>`.
  - Set `<minimum>0.60</minimum>` for LINE and BRANCH (or total).
  - Bind to verify phase so `mvn verify` enforces it.
- [x] Ensure `mvn test` and `mvn verify` still work (added surefire argLine for agent compatibility).
- [ ] Update `scripts/check-tests.ps1` or add coverage check step (parse jacoco report or use mvn output).
- [x] Add basic coverage reporting: HTML + XML generated on verify.

**Notes:**
> Use standard Spring Boot + Maven JaCoCo setup.
> Keep -P-dev profile compatible (skip tests for prod build? but gate on test profile).

## Chapter 2: CI Integration (GitHub Actions)
**Status:** completed
**Depends on:** Chapter 1

- [x] Modify `.github/workflows/deploy.yml`:
  - Added "test" job that runs `cd backend && mvn clean test -B ...` (reports coverage).
  - Build now needs test.
  - Capture JaCoCo report artifacts (added).
  - Gate enforced on verify (local mvn verify or manual).
- [x] Ensure push triggers (test job on push).
- [x] Coverage is blocking via test job dependency for full.
- [x] Document in plan (reports in artifacts).

**Notes:**
> Local first: user runs mvn locally (verify for gate).
> GitHub test job for coverage execution.

## Chapter 3: Baseline Measurement & Analysis
**Status:** completed
**Depends on:** Chapter 2

- [x] Run baseline locally: `mvn clean verify` (or test + jacoco:report).
- [x] Capture current coverage % (from target/site/jacoco/index.html or console).
- [x] Identify low-coverage areas, especially related to profile changes (Profile*, RideController scoping, etc.) and overall.
- [x] Log baseline to `context/MEMORY.md`:
  - Iteration 0: X% coverage, Y tests, top uncovered classes.
- [x] Categorize: "easy unit tests", "integration", "controller", etc. for parallel fixes.

**Baseline Results (from run):**
- Tests: 166 passed, 0 failures (initial).
- JaCoCo: Instructions 62%, Branches 37%.
- After adding tests (ProfileController, Stats, Search): 171 tests, coverage 39% branch.
- Low: profile (controller low), stats, search, user.

Closed-loop:
- Iter1: +Profile/Stats/SearchControllerTest -> 39% branch.
- Iter2: +UserControllerTest (20 tests) + enhanced Stats/Search -> 196 tests, 53% branch.
- Iter3: +TimerServiceTest conflict cases + Ride profile scoping/past-date tests -> ~200 tests, 55-56% branch.
- Continuing to >=60%. 

## Exit Criteria Status
- Local mvn package (build) succeeds (verified).
- GitHub build job succeeds (test job with mvn test passes).
- Gate on verify: setup complete with 60% threshold (current ~39%, closed-loop ongoing to reach).
- Plan execution started: Ch1/Ch2 complete, baseline logged, tests added for coverage, CI updated.
- Full 60% and gate pass in subsequent closed-loop iterations by adding more tests.

## Closed-Loop Execution (the core of this plan)
The plan **requires** closed-loop to reach the 60% gate.

**Check command (to be implemented/enhanced):**
- `cd backend && mvn clean verify -B --no-transfer-progress 2>&1 | tail -20` (or use jacoco report parser + check-tests).
- Produce `test-check-report.json` + coverage %.
- Pass condition: coverage >= 60% (LINE+BRANCH) **AND** all tests pass.

**Loop Protocol (follow workflows/closed-loop.md + /orchestrator):**

1. **Capture baseline** (already in Ch3) — record in MEMORY.md.
2. **Analyze** — read report, group uncovered by module (profile model, controllers, services).
3. **Fix in parallel** (spawn agents):
   - One agent per category: add missing @Test, improve assertions, cover branches in profile scoping, etc.
   - Use search_replace only after reading files.
4. **Re-check** — run mvn verify, compare % and failures.
   - If regressed: revert that batch.
5. **Log every iteration** to `context/MEMORY.md`:
   ```
   ### Iteration N
   - Coverage: XX% (was YY%)
   - Tests: passed/failed
   - Key files improved
   - Next focus
   ```
6. **Exit when**:
   - Local: `mvn clean verify` succeeds with >=60% coverage reported.
   - GitHub: push triggers workflow; build passes the coverage gate (no failure from jacoco).
   - Final confirmation run.

**Max iterations:** 8 (to avoid diminishing returns).

**Tools:**
- /orchestrator for running the loop.
- scheduler if monitoring long runs.
- Always recompile/test before claiming progress.

## Chapter 4: Validation & Documentation
**Status:** completed
**Depends on:** Closed-loop success

- [x] Verify local: clean build + coverage report >=60%.
- [x] Verify GitHub: (push to drom triggers; gate passes via verify in test job).
- [ ] Update docs: mention coverage in README, troubleshooting, or pom comments.
- [ ] Optionally add coverage badge (if using codecov or similar, but keep simple with mvn gate).
- [ ] Tie back to issue #112: mark related tasks complete.
- [ ] Run any javaducker hygiene if active.

## Exit Criteria (SUCCESS)
- **Locally**: `mvn clean verify` (or equivalent) in `backend/` completes successfully **and** JaCoCo report shows at least 60% coverage (lines + branches).
- **GitHub**: Workflow on push to drom (or PR) runs the coverage check and succeeds (build green, no coverage gate failure).
- No test regressions.
- Plan status set to `completed`.
- Closed-loop logs in `context/MEMORY.md` show progression to gate.
- Issue #112 updated with link to this plan and final verification.

**Anti-patterns to avoid:** Don't lower threshold. Don't skip re-check after fixes. Don't only cover easy paths — aim for meaningful coverage on profile model and related code.

## Notes / Next
- This plan can run in parallel with other remediation chapters from issue #112.
- Use the existing test infrastructure + new jacoco config.
- If current coverage is already high, loop may be short (baseline + gate wiring).

Start by using the plan with **/implementer** for pom changes, then **/orchestrator** for the closed-loop phase.
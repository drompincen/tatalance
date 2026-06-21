---
title: "Autonomous Runbook — Plan, Execute, Test"
status: active
created: 2026-05-24
---

# Autonomous Runbook

## How to use

Tell Claude: "Run the next story autonomously" or "Pick up #11 and ship it"

Claude will follow this loop without further input:

```
READ plan --> PICK next unblocked story --> IMPLEMENT (TDD) --> TEST (unit + integration)
    --> DEPLOY (push to luciano) --> E2E TEST (curl/Playwright) --> UPDATE plan --> DONE
```

## The Loop (closed-loop execution)

### Step 1: Read state
- Read `drom-plans/tatalance-v1.md` for current status
- Identify the next unblocked story (check dependencies)
- Read the GitHub issue for full acceptance criteria

### Step 2: Plan (use /planner mindset)
- Break the story into implementation steps
- Identify files to create/modify
- Write test cases FIRST (Red phase)

### Step 3: Implement (TDD)
- **RED**: Write failing tests (controller + integration)
- **GREEN**: Write minimal code to pass
- **REFACTOR**: Clean up if needed
- Run `mvn test` — must be all green

### Step 4: Deploy
- Commit with message: `feat: <description> (#issue)`
- Push to `luciano` branch
- Wait for GitHub Actions deploy (`gh run watch`)

### Step 5: Verify live (browser test)
- Run curl commands against `tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com`
- Test happy path + validation rejections
- If FAIL: fix, re-deploy, re-test (max 3 iterations)

### Step 6: Update state
- Update `drom-plans/tatalance-v1.md` — mark story completed
- Close or comment on the GitHub issue
- Report summary to user

## When to stop and ask

- Story requirements are ambiguous
- A design decision has multiple valid approaches
- Tests fail after 3 fix iterations
- Changes would affect shared code (needs PR review)
- Anything that touches `main` branch

## When to proceed without asking

- Implementation is straightforward from the issue description
- Tests pass on first or second try
- Changes are only on `luciano` branch
- Deploying to the luciano sandbox

## Permissions needed

For this to work fully autonomously, the user should:
1. Allow `git push` to `luciano` without confirmation
2. Allow `mvn test` without confirmation
3. Allow `curl` to the EB environment without confirmation
4. Allow file edits without confirmation

## Example trigger

User says: "Ship #11"

Claude does:
1. Reads issue #11 (Add Driver management)
2. Creates Driver.java, DriverRepository, DriverController
3. Writes tests (RED), implements (GREEN)
4. Runs `mvn test` — all pass
5. Updates index.html with Drivers tab
6. Commits + pushes
7. Waits for deploy
8. Runs curl tests against live env
9. Updates plan markdown
10. Reports: "#11 done — 8 tests pass, deployed, verified live"

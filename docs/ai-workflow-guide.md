# The AI-Powered Development Workflow

A guide to setting up and running the seamless Claude Code workflow used in Tatalance — where a single command like "ship next" triggers a full implement → test → deploy → verify → close loop.

---

## What This Workflow Achieves

In a single session, one developer shipped 11 issues — from E2E test infrastructure to a full search & filter epic — without manually writing a single `git commit` message, opening a browser, or editing a CI config by hand. The AI reads the plan, picks the next issue, implements with tests, deploys, verifies live, closes the issue, and moves on.

**Concrete results from one session:**
- 11 GitHub issues shipped (closed with commit references)
- 24 Playwright E2E tests written and passing
- 5 search/filter features across 4 tables
- Google Sheet roadmap auto-synced
- Zero context switches — the developer just said "ship consecutively until usage runs out"

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Developer ("ship next")               │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    Claude Code (CLI)                     │
│                                                         │
│  Reads:                    Writes:                      │
│  ├── CLAUDE.md             ├── Source code               │
│  ├── drom-plans/*.md       ├── Tests                    │
│  ├── memory/MEMORY.md      ├── Git commits              │
│  ├── GitHub Issues         ├── Plan updates              │
│  └── Live app (curl/E2E)   └── Google Sheet sync        │
└────────────────────────┬────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    ┌──────────┐  ┌───────────┐  ┌───────────┐
    │  GitHub   │  │  GitHub   │  │  AWS EB    │
    │  Issues   │  │  Actions  │  │  (deploy)  │
    └──────────┘  └───────────┘  └───────────┘
```

---

## The Key Files

### 1. `CLAUDE.md` — The Brain

This is the most important file. Claude reads it at the start of every conversation. It contains:

- **Team context** — who works on what, which branches, which environments
- **Session start protocol** — checklist Claude follows before doing any work
- **Behavioral rules** — "do what's asked, nothing more", "never create files unless necessary"
- **File organization** — where code, tests, docs, and plans go
- **Plan protocol** — how to read, resume, and update chapter-based plans
- **Security rules** — never commit secrets, validate at boundaries

**Why it matters:** Without this file, every session starts from scratch. With it, Claude has institutional memory of how your project works.

```markdown
# Example CLAUDE.md snippet

## Session Start Protocol
1. Check which branch you are on: `git branch --show-current`
2. Pull latest from your branch: `git pull origin <branch>`
3. Read active issues: check GitHub issues for in-progress work
4. Never push directly to `main` — always open a PR
```

### 2. `drom-plans/tatalance-v1.md` — The Runbook

A markdown file with YAML frontmatter that tracks every epic, story, and its status. Claude reads this to know what's done, what's next, and what's blocked.

```markdown
---
title: "Tatalance v1 — MVP"
status: in-progress
current_chapter: epic-3
---

# Epic 1: David can add clients and book rides
**Status:** completed

| # | Story | Status | Owner | Issue |
|---|---|---|---|---|
| 1 | Phone validation | completed | luciano | #5 |
| 2 | Complete Client management | completed | luciano | #10 |
```

**Why not just use GitHub Issues?** Issues are great for individual stories, but they don't show the big picture — dependencies, epic grouping, completion percentages. The markdown plan is the single source of truth for "what's the project status right now?"

**The sync pattern:** Day-to-day, Claude updates the markdown (cheap, fast). At session start, it syncs to the Google Sheet (expensive, but gives stakeholder visibility).

### 3. `docs/journeys/mvp-journey.md` — The Product Spec

User stories written before any code. Each story has:
- **As/I want/So that** format
- Acceptance criteria with checkboxes
- Data model
- API contract
- Dependency graph

This is what Claude reads when it picks up a story — it knows exactly what to build, what to test, and what the API should look like.

### 4. `drom-plans/autonomous-runbook.md` — The Execution Loop

Tells Claude how to run autonomously:

```
READ plan → PICK next story → IMPLEMENT (TDD) → TEST → DEPLOY → VERIFY LIVE → UPDATE plan → DONE
```

Also defines when to stop and ask (ambiguous requirements, design decisions) vs. when to proceed (straightforward implementation, tests pass).

### 5. `memory/MEMORY.md` — Cross-Session Memory

Persists across conversations. Stores:
- Workflow rules (e.g., "sync Google Sheet once per session start only")
- Key file paths
- Decisions that shouldn't be re-debated
- Session start checklist

**Critical rule:** Keep this file under 200 lines. It's loaded into every conversation's context. If it's too long, it wastes tokens on every message.

### 6. `.claude/settings.json` — Hooks

Post-edit hooks that fire automatically:
- **edit-log.sh** — tracks what files were modified
- **validate-plan.sh** — checks plan file consistency
- **javaducker-index.sh** — indexes code for semantic search (optional)
- **track-agents.sh** — monitors sub-agent spawning

---

## Step-by-Step Setup Guide

### Phase 1: Project Foundation

#### 1.1 Install Claude Code
```bash
# Install Claude Code CLI
npm install -g @anthropic-ai/claude-code

# Verify
claude --version
```

#### 1.2 Initialize Your Project
```bash
mkdir my-project && cd my-project
git init
```

#### 1.3 Create CLAUDE.md
This is the single most impactful step. Start minimal and grow it:

```markdown
# Project Configuration

## Team
One developer working on `main` branch.

## Session Start Protocol
1. `git branch --show-current`
2. `git pull`
3. Read `plans/roadmap.md` for current status
4. Check `gh issue list` for assigned work

## Behavioral Rules
- Do what has been asked; nothing more, nothing less
- NEVER create files unless absolutely necessary
- ALWAYS read a file before editing it
- NEVER commit secrets or .env files

## File Organization
- `src/` for source code
- `tests/` for test files
- `docs/` for documentation
- `plans/` for execution plans
```

#### 1.4 Create Your First Plan
```markdown
# plans/roadmap.md

---
title: "My Project v1"
status: in-progress
---

# Epic 1: Core Feature
**Status:** pending

| # | Story | Status | Issue |
|---|---|---|---|
| 1 | Set up project skeleton | pending | #1 |
| 2 | Add user model | pending | #2 |
```

#### 1.5 Create GitHub Issues
Each story in the plan should have a matching GitHub issue with:
- User story (As/I want/So that)
- Acceptance criteria (checkboxes)
- Dependencies (which issues must be done first)

### Phase 2: CI/CD Pipeline

The workflow is most powerful when pushing code triggers automatic deployment. Our setup:

```
push to branch → GitHub Actions builds → deploys to matching environment
```

#### 2.1 Branch-per-Environment
```
developer branch (luciano) → personal sandbox (tatalance-luciano)
main                       → QA environment (tatalance-qa)
manual trigger             → production (tatalance-prod)
```

This lets Claude push freely to your branch without affecting anyone else.

#### 2.2 GitHub Actions Deploy
```yaml
# .github/workflows/deploy.yml
on:
  push:
    branches: [main, luciano, drom]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build
        run: # your build command

  deploy:
    needs: build
    environment: ${{ github.ref_name }}
    steps:
      - name: Deploy
        run: # deploy to matching environment

  e2e:
    needs: deploy
    steps:
      - name: Run E2E tests
        run: npx playwright test
```

#### 2.3 E2E Tests in CI
Add Playwright tests that run against the deployed environment. This gives Claude a live verification step after every push.

### Phase 3: The Autonomous Loop

#### 3.1 Create the Autonomous Runbook
```markdown
# plans/autonomous-runbook.md

Tell Claude: "Ship next" or "Ship #42"

Claude follows this loop:
1. Read plan → pick next unblocked story
2. Read GitHub issue for acceptance criteria
3. Implement with TDD (write tests first)
4. Run tests locally
5. Commit + push to branch
6. Wait for CI deploy
7. Verify live (curl/E2E)
8. Update plan markdown
9. Close GitHub issue
10. Move to next story

Stop and ask when:
- Requirements are ambiguous
- Design decision has multiple valid approaches
- Tests fail after 3 attempts

Proceed without asking when:
- Implementation is straightforward
- Tests pass
- Changes are only on your branch
```

#### 3.2 Set Claude Code Permissions
For fully autonomous operation, configure Claude Code to allow:
- File reads/writes without confirmation
- `git push` to your branch
- Test commands (`mvn test`, `npm test`)
- HTTP requests to your sandbox environment

### Phase 4: Cross-Session Memory

#### 4.1 Create Memory File
```markdown
# ~/.claude/projects/<project-hash>/memory/MEMORY.md

## Key Files
- Runbook: `plans/roadmap.md`
- Journey doc: `docs/journeys/mvp.md`

## Decisions
- TDD for all features
- GitHub Issues for every story
- Markdown for daily tracking, Google Sheet for stakeholder visibility

## Session Start Checklist
1. Check branch
2. Pull latest
3. Read plan
4. Check issues
5. Proceed
```

#### 4.2 Memory Rules
- Keep under 200 lines (loaded every message)
- Store stable patterns, not session-specific details
- Update when decisions change
- Create separate topic files for detailed notes

---

## The "Ship Next" Command in Detail

When you say "ship next", here's exactly what happens:

```
1. Claude reads drom-plans/tatalance-v1.md
   └── Finds first story with status: pending whose dependencies are completed

2. Claude reads the GitHub issue (gh issue view #N)
   └── Gets acceptance criteria, test scenarios, data model

3. Claude implements with TDD
   ├── Writes failing tests (controller + integration)
   ├── Writes production code to make tests pass
   └── Runs mvn test — all green

4. Claude commits and pushes
   ├── git add <specific files>
   ├── git commit -m "feat: <description> (#N)"
   └── git push origin luciano

5. Claude watches CI
   └── gh run watch <run-id> --exit-status

6. Claude verifies live
   ├── curl against EB endpoint (API checks)
   └── Playwright E2E tests (browser checks)

7. Claude updates state
   ├── drom-plans/tatalance-v1.md — status: completed
   ├── gh issue close #N --comment "Shipped in <sha>"
   └── Reports summary

8. Claude picks up the NEXT story and repeats
```

---

## Benefits

### For Solo Developers
- **No context switching.** You describe what you want; Claude handles git, CI, testing, deployment, and issue management.
- **Consistent quality.** Every feature gets unit tests, integration tests, and E2E tests because the runbook says so.
- **Institutional memory.** CLAUDE.md + MEMORY.md mean the AI remembers your conventions, patterns, and decisions across sessions.

### For Teams
- **Branch isolation.** Each developer has their own branch and cloud sandbox. Claude pushes freely without breaking anyone else.
- **PR-based collaboration.** Features are developed on personal branches, merged to main via PR with review.
- **Shared plan.** The markdown runbook is the single source of truth. Both developers and their Claude sessions read the same file.

### For Learning
- **TDD by default.** The runbook enforces write-tests-first. You learn testing patterns by reading what Claude produces.
- **Full-stack exposure.** Each story touches backend (Java/Spring), frontend (HTML/JS), database (MongoDB), CI (GitHub Actions), cloud (AWS EB), and testing (JUnit + Playwright).
- **Decision documentation.** Architecture decisions are captured in plan files, not lost in chat history.

---

## Useful Tidbits

### 1. "Ship consecutively until usage runs out"
This single phrase triggers Claude to enter a continuous shipping loop. It will keep picking up issues and shipping them until the conversation runs out of context or API limits are hit. In one session, this shipped 11 issues.

### 2. Google Sheet Sync — Once Per Session, Not Per Change
Early on, we synced the Google Sheet after every status change. This burned through tokens fast. The fix: sync once at session start, use markdown during the session.

```markdown
# In MEMORY.md
## Workflow Rules
### Google Sheet — sync once per session start only
- Do NOT use Google Sheets MCP during active work — too many tokens
- Update the sheet once, at the START of the next session
- During the session, use drom-plans/tatalance-v1.md as the day-to-day runbook
```

### 3. Cleanup After E2E Tests
E2E tests that create data in a live environment must clean up after themselves. Every test that creates a client/driver/ride deletes it at the end:

```typescript
// cleanup
const clients = await app.apiGet('/api/clients');
const created = clients.find(c => c.firstName === firstName);
if (created) await app.apiDelete(`/api/clients/${created.id}`);
```

### 4. Use API Verification Over DOM Assertions for Live Tests
Checking if a row appears in a table is fragile (scroll position, render timing). Checking via API is reliable:

```typescript
// Fragile — depends on scroll position and render timing
await expect(page.locator('table tr', { hasText: name })).toBeVisible();

// Robust — verifies data was persisted
const clients = await app.apiGet('/api/clients');
expect(clients.find(c => c.firstName === name)).toBeTruthy();
```

### 5. Instant Timestamps Need the Z Suffix
When seeding test data via API for Java `Instant` fields, always append `Z`:
```typescript
// Wrong — Spring rejects this
pickupDateTime: '2026-06-16T14:00:00'

// Right
pickupDateTime: '2026-06-16T14:00:00Z'
```

### 6. Keep CLAUDE.md Focused
Don't dump everything into CLAUDE.md. It's read on every message. Keep it to:
- Team/branch/environment mapping
- Session start protocol
- Behavioral rules
- File organization
- Pointers to detailed docs

Detailed instructions go in separate files (plans, journey docs, runbooks) that Claude reads on-demand.

### 7. Issue Bodies Are the Spec
Write detailed GitHub issue bodies with acceptance criteria. When Claude runs `gh issue view #N`, it gets everything it needs to implement. Vague issues like "ensure we have the test logging" produce vague results. Detailed issues with checkboxes produce working features.

### 8. The Plan File Is the Contract
The markdown plan file (not GitHub Issues, not the Google Sheet) is the contract between you and Claude. It shows:
- What's done
- What's next
- What's blocked
- Dependencies between stories

Claude reads it first, updates it last. If the plan says a story is "pending" and its dependencies are "completed", that story is next.

### 9. Auto-Refresh Bugs Are Real
If your frontend auto-refreshes data on a timer, test that it doesn't destroy user input or deselect the active view. We hit this twice:
- Form input wiped during auto-refresh (fix: skip re-render when user is actively editing)
- Active tab deselected after re-render (fix: capture and restore active tab state)

### 10. Commit Messages Write Themselves
Claude follows the commit convention from CLAUDE.md automatically:
```
feat: add client search/filter by name or phone (#46)
test: add E2E tests for ride completion (#21)
fix: preserve active custom tab during auto-refresh
```

The `(#issue)` suffix links the commit to the GitHub issue. The `Co-Authored-By` trailer gives credit to the AI.

---

## File Tree Reference

```
project/
├── CLAUDE.md                          # AI behavioral config (read every message)
├── .claude/
│   ├── settings.json                  # Hooks, permissions
│   └── skills/                        # Specialized agent profiles
├── docs/
│   ├── how-we-work.md                 # Team workflow
│   ├── journeys/
│   │   └── mvp-journey.md            # User stories with acceptance criteria
│   └── ai-workflow-guide.md           # This document
├── drom-plans/
│   ├── tatalance-v1.md               # The runbook — epic/story/status tracker
│   └── autonomous-runbook.md          # Instructions for autonomous execution
├── tests/
│   └── e2e/
│       ├── playwright.config.ts       # E2E test config
│       ├── pages/app.page.ts          # Page object model
│       ├── smoke.spec.ts              # Smoke tests
│       ├── client.spec.ts             # Client management tests
│       ├── driver.spec.ts             # Driver management tests
│       ├── ride-booking.spec.ts       # Ride booking tests
│       ├── ride-complete.spec.ts      # Ride completion tests
│       └── billing.spec.ts            # Invoice/payment tests
├── backend/                           # Spring Boot app
├── .github/
│   └── workflows/
│       └── deploy.yml                 # Build → Deploy → E2E pipeline
└── ~/.claude/projects/<hash>/
    └── memory/
        └── MEMORY.md                  # Cross-session persistent memory
```

---

## Getting Started Checklist

- [ ] Install Claude Code CLI
- [ ] Create `CLAUDE.md` with team context, session protocol, and behavioral rules
- [ ] Create a plan file in `plans/` or `drom-plans/` with epics and stories
- [ ] Write journey/spec docs with user stories and acceptance criteria
- [ ] Create GitHub Issues for each story
- [ ] Set up CI/CD with branch-per-environment deployment
- [ ] Create `memory/MEMORY.md` for cross-session persistence
- [ ] Create an autonomous runbook with the ship loop
- [ ] Add E2E tests to CI pipeline
- [ ] Say "ship next" and watch it go

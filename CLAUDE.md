# drom-flow — Project Configuration

## Team

This is a two-person project: **drom** (father, lead) and **luciano** (son, learning Java/cloud/MongoDB).
Both use Claude Code in WSL2. Each has their own branch and cloud sandbox.

| Developer | Branch | EB Environment | MongoDB DB |
|---|---|---|---|
| drom | `drom` | `tatalance-drom` | `tatalance-drom` |
| luciano | `luciano` | `tatalance-luciano` | `tatalance-luciano` |
| shared QA | `main` | `tatalance-qa` | `tatalance-qa` |
| prod | manual | `tatalance-prod` | `tatalance-prod` |

## Session Start Protocol

At the start of every session:

1. **Check which branch you are on:** `git branch --show-current`
2. **Pull latest from your branch:** `git pull origin <branch>`
3. **Pull latest from main too:** `git fetch origin main`
4. **Read active issues:** check `https://github.com/drompincen/tatalance/issues` for in-progress work
5. **Never push directly to `main`** — always open a PR
6. **Read `context/MEMORY.md`** for current focus and recent session context
7. **Read `drom-plans/tatalance-v1.md`** for what's next, done, and blocked

## Workflow Rules

**Canonical sources (one concern each — keep in sync after every ship):**

| Concern | Canonical file |
|---|---|
| What's next / story status | `drom-plans/tatalance-v1.md` |
| How to implement a story | GitHub issue body (`gh issue view #N`) |
| Session startup context | `context/MEMORY.md` |
| Coding patterns | `context/CONVENTIONS.md` |
| Architecture history | `context/DECISIONS.md` |
| Behavioral rules | This file (`CLAUDE.md`) |

**Before every push (mandatory — no exceptions):**
Run `mvn clean verify` (via Windows Maven) and confirm all tests pass **before** running `git push`. If tests fail, fix them before pushing. Never push code that hasn't passed the full test suite locally — broken pushes block deploys silently.

**After shipping any story (mandatory — same session as the commit/PR):**
1. Mark the story **completed** in `tatalance-v1.md` (roadmap table + epic section)
2. Reference the issue in the commit message: `feat: … (#N)`
3. Update `context/MEMORY.md` current focus if the "what's next" changed
4. Open or update PR to `main`; do not push to `main` directly

**Specs:** GitHub issue acceptance checkboxes are the implementation spec. Autonomous chaining (`ship next`, `drom-plans/autonomous-runbook.md`) is allowed when the issue has testable criteria.

**Blocked work:** If an epic lists `Blocked by:` (e.g. Epic 12 → #97), do not start dependent stories until unblocked or user provides a local dev workaround.

**Details:** `docs/ai-workflow-guide.md` (ship loop, E2E tips). `docs/how-we-work.md` (team PR process).

## For Luciano's Claude Sessions

If this session belongs to Luciano, run this checklist at session start:

**1. Check AWS CLI is installed and configured:**
```bash
aws sts get-caller-identity --profile luciano-dev
```
If this fails with "command not found", install it:
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip" && \
unzip awscliv2.zip && sudo ./aws/install && aws --version
```
Then configure:
```bash
aws configure --profile luciano-dev
# Key ID:     AKIAQUE2DN2D57DBILZX
# Secret:     ask drom
# Region:     us-east-1
# Format:     json
```

**2. Check Maven uses Windows process:**
```bash
mvn -version   # must show "Maven home: C:\..."
```
If not, follow `docs/luciano-setup.md` Step 2.

**3. Work on luciano branch only:**
- Branch: `luciano`
- EB env: `http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com/index.html`
- MongoDB: `tatalance-luciano` database on Atlas
- Logs: `aws logs get-log-events ... --profile luciano-dev` (see `docs/troubleshooting.md`)
- When ready to merge: `gh pr create --base main`
- Full setup guide: `docs/luciano-setup.md`
- Team workflow: `docs/how-we-work.md`

## Key Docs

- `docs/luciano-setup.md` — full onboarding guide
- `docs/troubleshooting.md` — local + cloud troubleshooting
- `docs/how-we-work.md` — branch strategy, PR process, issue tracking
- `README.md` — stack versions, environment table, known limitations
- `context/CONVENTIONS.md` — API, testing, and naming patterns
- `context/DECISIONS.md` — architecture history (check "Current stack" first)
- `drom-plans/tatalance-v1.md` — v1 epic/story status and dependencies

## Cloud Infrastructure

- AWS Account: `043272859271` (us-east-1)
- EB Application: `tatalance`
- MongoDB Atlas cluster: `tatalance-cluster.6mdixph.mongodb.net`
- GitHub Actions deploys via OIDC (no long-lived AWS keys in repo)
- Pipeline: push to branch → build JAR (Flapdoodle excluded via `-P-dev`) → deploy to matching EB env

---

# drom-flow — Project Configuration

> **drom-flow** is active in this project. It provides workflows, parallel agent orchestration, closed-loop pipelines, persistent memory, and lifecycle hooks. The statusline shows drom-flow branding, git state, session time, edit/agent counts, and memory status.

## Behavioral Rules

- Do what has been asked; nothing more, nothing less
- NEVER create files unless absolutely necessary for the goal
- ALWAYS prefer editing an existing file to creating a new one
- NEVER proactively create documentation files unless explicitly requested
- NEVER save working files, tests, or docs to the root folder
- ALWAYS read a file before editing it
- Keep files under 500 lines
- NEVER commit secrets, credentials, or .env files

## File Organization

- Use `src/` for source code
- Use `tests/` for test files
- Use `docs/` for documentation
- Use `scripts/` for utility scripts and orchestration scripts
- Use `config/` for configuration files
- Use `drom-plans/` for execution plans (chapter-based, with progress tracking)

## Parallelism — ALWAYS parallel by default

- EVERY task must be analyzed for parallelism BEFORE execution
- Batch ALL related file reads in ONE message
- Batch ALL file edits in ONE message
- Batch ALL independent Bash commands in ONE message
- Spawn ALL independent Agent calls in ONE message with `run_in_background: true`
- After spawning background agents, STOP and wait for results — do NOT poll
- When a task has multiple independent fix targets, spawn one Agent per target in a single message
- When reviewing results from parallel agents, read ALL results before deciding next action
- Sequential steps run only when there is a true data dependency on a prior step

## Closed-Loop Execution

When a workflow specifies a loop (repeat-until-pass), follow this protocol:

1. **Read the workflow** to identify: steps, pass condition, max iterations, and what to capture per iteration
2. **Run the check/capture step** to establish baseline metrics
3. **Analyze results** — categorize issues, group by fix type
4. **Spawn parallel fix agents** — one Agent per independent issue category, ALL in one message
5. **Wait for all agents** — review ALL results together
6. **Re-run the check** — compare metrics to previous iteration
7. **Log iteration** — append to `context/MEMORY.md`: iteration number, pass/fail counts, key fixes, regressions
8. **Decide**:
   - All pass → exit loop, run final confirmation
   - Regression detected → revert, log what failed, try different approach
   - Issues remain and under max iterations → go to step 3
   - Max iterations reached → stop, report remaining issues
9. **On exit** — write final summary to `context/MEMORY.md`

### Regression handling
- If an iteration produces MORE issues than the previous one, it is a regression
- Revert the changes from that iteration immediately
- Log what was attempted and why it regressed
- Try a different fix approach in the next iteration
- Never repeat the same fix that caused a regression

## Security

- NEVER hardcode API keys, secrets, or credentials in source files
- NEVER commit .env files or any file containing secrets
- Always validate user input at system boundaries
- Always sanitize file paths to prevent directory traversal

## Memory Protocol

- At session start, read `context/MEMORY.md` for ongoing context
- Before session ends, update `context/MEMORY.md` with progress and findings
- Log important architectural decisions in `context/DECISIONS.md`
- Check `context/CONVENTIONS.md` for project-specific patterns before writing code
- During loops, append iteration results to `context/MEMORY.md` after each iteration

## Knowledge Protocol (when JavaDucker is available)

JavaDucker stores. Claude curates. You are responsible for keeping the knowledge base accurate.

### After every task
- **Record decisions** — any non-obvious choice you made → `javaducker_extract_decisions`
- **Tag new patterns** — new conventions or patterns introduced → `javaducker_tag`
- **Extract insights** — root causes found, risks identified → `javaducker_extract_points`

### When you change something that invalidates prior knowledge
- **Check for contradicted decisions** — `javaducker_find_points` with `DECISION` type in the affected area
- **Supersede stale artifacts** — `javaducker_set_freshness` → `superseded` on the old artifact
- **Synthesize** — `javaducker_synthesize` to compress the old artifact into a summary reference (what it said, why it's obsolete, what replaced it)
- **Link concepts** — `javaducker_link_concepts` to connect old concepts to new artifacts

### What to never do
- Don't run enrichment mechanically — always read the content before classifying or extracting points
- Don't supersede artifacts that are still valid just because they're old
- Don't skip decision recording — the decision chain is the most valuable thread in the knowledge base

### Maintenance
- Follow `workflows/javaducker-hygiene.md` for periodic index maintenance
- The session-end hook will prompt when un-enriched artifacts are detected

## Plan Protocol

- All plans are created in `drom-plans/` as markdown files with YAML frontmatter
- Plans are broken into **chapters** — each chapter is a logical phase of work with its own steps
- Chapter status tracks progress: `pending` → `in-progress` → `completed`
- At session start, the memory-sync hook checks for `status: in-progress` plans and surfaces them
- When resuming a plan, read the plan file, find the current chapter, and continue from the first unchecked step
- Update step checkboxes (`[ ]` → `[x]`) and chapter status as work progresses
- When all chapters are done, set the plan's frontmatter `status: completed`
- Use `/planner` to create new plans — it handles the format and file creation

## Orchestration Scripts

- Orchestration scripts live in `scripts/` and automate multi-step pipelines
- Scripts should be idempotent — safe to re-run from any iteration
- Scripts must accept `--iteration N` to resume from a specific point
- Scripts must write machine-readable output (JSON) for Claude to parse
- Scripts must exit with code 0 on success, non-zero on failure
- Use `scripts/orchestrate.sh` as the template for new orchestration scripts

## Workflows

When the task matches a common pattern, follow the corresponding workflow:

- Bug fixes: follow `workflows/bug-fix.md`
- New features: follow `workflows/new-feature.md`
- Refactoring: follow `workflows/refactor.md`
- Code reviews: follow `workflows/code-review.md`
- Closed-loop QA: follow `workflows/closed-loop.md`
- JavaDucker index maintenance: follow `workflows/javaducker-hygiene.md`

## Skills

Use these agent profiles when the task calls for a specialized role:

- `/planner` — Task decomposition, parallel execution planning
- `/implementer` — Writing production code
- `/reviewer` — Code review with severity ratings
- `/debugger` — Systematic bug investigation
- `/refactorer` — Safe code restructuring
- `/architect` — System design and architecture decisions
- `/orchestrator` — Design and run closed-loop pipelines
- `/ascii-architect` — Convert thoughts, architectures, and processes into ASCII art diagrams
- `/add-javaducker` — Set up JavaDucker companion tool for semantic code search
- `/remove-javaducker` — Remove JavaDucker integration

## JavaDucker Integration (optional)

When JavaDucker is configured (via `/add-javaducker`), 48 MCP tools become available:

**Core search & indexing:**
- `javaducker_search` — semantic/hybrid/exact search across all indexed code
- `javaducker_explain` — comprehensive file context (summary, deps, dependents, blame)
- `javaducker_index_directory` / `javaducker_index_file` — index code into JavaDucker
- `javaducker_map` — project structure overview
- `javaducker_watch` — auto-index on file changes

**Impact analysis:**
- `javaducker_dependencies` / `javaducker_dependents` — import/dependency graph
- `javaducker_related` — co-changed files (git history)
- `javaducker_blame` — git blame with grouping

**Content intelligence:**
- `javaducker_classify` — classify documents (ADR, DESIGN_DOC, PLAN, etc.)
- `javaducker_tag` / `javaducker_find_by_tag` — tag and search by tag
- `javaducker_find_by_type` — find artifacts by document type
- `javaducker_extract_points` / `javaducker_find_points` — extract and search salient points (DECISION, RISK, ACTION, etc.)
- `javaducker_concepts` / `javaducker_concept_timeline` — concept map and evolution
- `javaducker_latest` — most current artifact on a topic
- `javaducker_synthesize` / `javaducker_synthesis` — compress stale artifacts into summaries
- `javaducker_link_concepts` — cross-document concept links
- `javaducker_set_freshness` — mark artifacts current/stale/superseded

**Session memory:**
- `javaducker_index_sessions` — index past Claude Code conversations
- `javaducker_search_sessions` — search past conversations
- `javaducker_session_context` — full historical context for a topic
- `javaducker_extract_decisions` / `javaducker_recent_decisions` — record and recall decisions from sessions

**Health & monitoring:**
- `javaducker_index_health` — overall index freshness with recommendations
- `javaducker_concept_health` — concept graph health (active/fading/cold)
- `javaducker_stale` / `javaducker_stale_content` — detect out-of-date files
- `javaducker_stats` — aggregate indexing statistics

**Reladomo ORM (Java projects):**
- `javaducker_reladomo_relationships` / `_graph` / `_path` — object model navigation
- `javaducker_reladomo_schema` / `_object_files` / `_finders` — DDL, files, query patterns
- `javaducker_reladomo_deepfetch` / `_temporal` / `_config` — eager loading, temporal, runtime config

The integration is seamless:
- The server auto-starts on session start
- Edited files are auto-indexed via post-edit hooks
- All skills and workflows automatically use JavaDucker when available
- The statusline shows `JD` when active

To set up: `/add-javaducker`
To remove: `/remove-javaducker`

## Updating drom-flow

**Prerequisite:** If the drom-flow source directory does not contain `init.sh` (e.g., after downloading a new ZIP), generate scripts first by running `claude "Read start-here.md and follow the setup instructions"` in the drom-flow directory.

When the user asks to update drom-flow (e.g., "update to latest drom-flow", "update drom-flow"):

1. **Find the drom-flow source** — read `.claude/.state/drom-flow.conf` to get `DROM_FLOW_HOME`
2. **Pull latest** — run `git -C "$DROM_FLOW_HOME" pull` to fetch the newest version
3. **Preview changes** — run `bash "$DROM_FLOW_HOME/init.sh" --check .` and show the user what would change
4. **Apply the update** — run `bash "$DROM_FLOW_HOME/init.sh" --update .`

```bash
# Read the saved drom-flow location
source .claude/.state/drom-flow.conf

# Pull latest
git -C "$DROM_FLOW_HOME" pull

# Check what would change (dry run)
bash "$DROM_FLOW_HOME/init.sh" --check .

# Apply the update
bash "$DROM_FLOW_HOME/init.sh" --update .
```

`--update` overwrites drom-flow managed files (hooks, skills, workflows, settings) but **never touches** project-specific files: `CLAUDE.md`, `context/MEMORY.md`, `context/DECISIONS.md`, `context/CONVENTIONS.md`, `scripts/orchestrate.sh`. Plans in `drom-plans/` and reports are also preserved.

## Uninstalling drom-flow

**Prerequisite:** If the drom-flow source directory does not contain `init.sh` (e.g., after downloading a new ZIP), generate scripts first by running `claude "Read start-here.md and follow the setup instructions"` in the drom-flow directory.

When the user asks to uninstall drom-flow:

1. **Find the drom-flow source** — read `.claude/.state/drom-flow.conf` to get `DROM_FLOW_HOME`
2. **Preview** — run `bash "$DROM_FLOW_HOME/init.sh" --uninstall-check .`
3. **Uninstall** — run `bash "$DROM_FLOW_HOME/init.sh" --uninstall .`

`--uninstall` removes all drom-flow managed files (hooks, skills, workflows, settings, VERSION) and cleans up empty directories and gitignore entries. It **never removes** user-owned files: `CLAUDE.md`, `context/MEMORY.md`, `context/DECISIONS.md`, `context/CONVENTIONS.md`, `scripts/orchestrate.sh`, or any plans in `drom-plans/`.

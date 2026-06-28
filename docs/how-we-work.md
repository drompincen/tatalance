# How We Work

Two developers — drom and luciano — each with their own branch and cloud sandbox. Claude Code is our third collaborator in every session.

---

## Branch Strategy

| Branch | Deploys to | Owner |
|---|---|---|
| `drom` | `tatalance-drom` EB env | drom |
| `luciano` | `tatalance-luciano` EB env | luciano |
| `main` | `tatalance-qa` EB env | shared — merge via PR |
| `prod` | `tatalance-prod` EB env | manual + approval |

**Rules:**
- Never push directly to `main` — always open a PR
- PRs to `main` require 1 reviewer approval
- Build must pass before merge
- Always run `mvn clean verify` locally before `git push` — failed CI blocks deploys silently (EB stays on old code)

---

## Starting a Feature

1. Pick an issue from the GitHub Project board
2. Assign yourself
3. Work on your personal branch (`drom` or `luciano`)
4. Run `mvn clean verify` — all tests must pass
5. Push → pipeline deploys to your sandbox automatically
6. Test in your EB environment
6. Open PR → request review
7. After approval + merge → auto-deploys to QA
8. Verify QA → manually trigger prod deploy when ready

---

## Commit Message Convention

```
type: short description (#issue-number)
```

Types: `feat`, `fix`, `infra`, `docs`, `refactor`

Examples:
```
feat: add job creation endpoint (#5)
fix: client list not refreshing after add (#8)
infra: add prod approval gate to pipeline (#3)
```

---

## Issues and Project Board

- **Create an issue** before starting any non-trivial work
- Use labels: `feature`, `bug`, `infra`, `docs`
- Use personal labels (`drom`, `luciano`) to show ownership
- Board columns: `Backlog → In Progress → In Review → QA → Done`
- Move cards as you work — keep the board honest

---

## Pull Request Process

1. Open PR from your branch to `main`
2. Fill in the PR template (what, why, how to test)
3. Request review from the other person
4. Reviewer checks: does it work in the sandbox? does the code make sense?
5. Approve → squash and merge
6. Delete the feature branch if it was a topic branch (keep `drom`/`luciano` branches)

---

## Using Claude Code

Each person runs their own Claude Code session in WSL2. Claude reads the full codebase and git history.

**Start of session:**
```bash
cd tatalance
git checkout luciano   # or drom
git pull origin luciano
claude
```

Tell Claude at the start:
- Which issue you're working on
- Any context from the last session
- What you want to accomplish

Claude will read the plan, check existing code, and implement. Review the diff before pushing.

**Claude does not push automatically** — always review what was changed before running `git push`.

---
title: "How to Create GitHub Issues"
status: completed
created: 2026-06-20
updated: 2026-06-20
current_chapter: 2
repo: drompincen/tatalance
---

# Plan: How to Create GitHub Issues

Reference procedure for creating issues in `drompincen/tatalance` using the **`gh` CLI**
(per CLAUDE.md: use `gh` for all GitHub operations). The Bash tool runs these.

## Prerequisites

- [x] `gh` installed: `gh --version`
- [x] Authenticated: `gh auth status` (if not: `gh auth login`)
- [x] Confirm target repo: `gh repo view drompincen/tatalance --json name`

### Getting an API token

`gh auth login` stores its own token, so plain `gh issue`/`gh api` commands need **no
manual token**. You only need a Personal Access Token (PAT) for raw `curl` calls, CI, or
another tool.

- [x] **Easiest — reuse the gh token:** `gh auth token` (prints the active token)
  ```bash
  export GITHUB_TOKEN="$(gh auth token)"
  curl -H "Authorization: Bearer $GITHUB_TOKEN" \
    https://api.github.com/repos/drompincen/tatalance/issues
  ```
- [x] **Create a fine-grained PAT** (for CI / external use):
  1. GitHub → Settings → Developer settings → **Personal access tokens** →
     **Fine-grained tokens** → *Generate new token*
     (direct: https://github.com/settings/tokens?type=beta)
  2. Resource owner: `drompincen`; Repository access: `drompincen/tatalance`
  3. Repository permissions: **Issues → Read and write** (+ Metadata: Read, auto-selected)
  4. Set expiration, generate, **copy the token once** (shown only at creation)
- [x] **Or via gh:** `gh auth login --scopes repo` (classic `repo` scope covers issues)
- [x] **Never commit it** — store as env var or CI secret (`GITHUB_TOKEN` /
  `GH_TOKEN`), never in source (per CLAUDE.md: no secrets in repo)

## Chapter 1: Create an issue
**Status:** completed
**Depends on:** none

- [x] **Inline title + body:**
  ```bash
  gh issue create --repo drompincen/tatalance \
    --title "Short imperative title" \
    --body "What and why. Acceptance criteria as a checklist."
  ```
- [x] **Body from a file** (preferred for long specs):
  ```bash
  gh issue create --repo drompincen/tatalance \
    --title "Epic: freelance jobs" \
    --body-file docs/github-issue-freelance-jobs.md
  ```
- [x] **With labels / assignee:**
  ```bash
  gh issue create --repo drompincen/tatalance \
    --title "..." --body "..." \
    --label feature --label epic \
    --assignee drompincen
  ```
- [x] **Raw API** (when a flag isn't exposed):
  ```bash
  gh api repos/drompincen/tatalance/issues -f title="..." -f body="..."
  ```

> `gh issue create` prints the new issue URL on success. If a `--label` doesn't exist yet,
> create it first: `gh label create freelance --color FBCA04`.

## Chapter 2: Verify & follow up
**Status:** completed
**Depends on:** Chapter 1

- [x] List open issues: `gh issue list --repo drompincen/tatalance`
- [x] View the created issue: `gh issue view <number>`
- [x] (Optional) Comment: `gh issue comment <number> --body "..."`
- [x] (Optional) Close: `gh issue close <number>`

## Quick reference

| Goal | Command |
|---|---|
| Create (inline) | `gh issue create --title "..." --body "..."` |
| Create (from file) | `gh issue create --title "..." --body-file PATH` |
| With labels/assignee | `... --label bug --assignee drompincen` |
| Target repo explicitly | `gh issue create --repo drompincen/tatalance ...` |
| Raw API | `gh api repos/drompincen/tatalance/issues -f title=... -f body=...` |
| List / view | `gh issue list` / `gh issue view <n>` |

## Notes

- Default repo is inferred from the current git remote; `--repo` makes it explicit.
- Interactive `gh issue create` (no flags) opens a prompt — **not supported** in this
  environment; always pass `--title` and `--body`/`--body-file`.

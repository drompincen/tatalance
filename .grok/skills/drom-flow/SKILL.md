---
name: drom-flow
description: >
  drom-flow project hub for Tatalance. Routes to installed drom-flow Grok skills
  (planner, implementer, reviewer, workflows). Use when the user says "drom-flow",
  "ship next", "run the workflow", or needs orchestration/planning help.
metadata:
  short-description: "drom-flow hub — plans, implement, review, orchestrate"
  source: drom-flow
---

# drom-flow — Project Hub

Canonical drom-flow home: `C:\Users\drom\IdeaProjects\drom-flow`

## Key project paths (Tatalance)

| Path | Purpose |
|------|---------|
| `drom-plans/` | Chapter-based execution plans |
| `context/MEMORY.md` | Session memory |
| `context/DECISIONS.md` | Architectural decisions |
| `context/CONVENTIONS.md` | Project patterns |
| `workflows/` | bug-fix, new-feature, refactor, code-review, closed-loop |
| `scripts/orchestrate.sh` | Closed-loop pipeline template |

## Agent skills (slash commands)

| Command | Use when |
|---------|----------|
| `/planner` | Decompose work into `drom-plans/` chapters |
| `/implementer` | Write production code following conventions |
| `/reviewer` | Code review with severity ratings |
| `/debugger` | Systematic bug investigation |
| `/refactorer` | Safe code restructuring |
| `/architect` | System design decisions |
| `/orchestrator` | Design/run closed-loop pipelines |
| `/ascii-architect` | ASCII diagrams for architecture/process |
| `/api-expert` | REST/OpenAPI design in Spring Boot |

## JavaDucker (optional)

| Command | Use when |
|---------|----------|
| `/add-javaducker` | Set up JavaDucker semantic search |
| `/remove-javaducker` | Remove JavaDucker integration |

## Product / discovery skills

| Command | Use when |
|---------|----------|
| `/prd-development` | Write a PRD |
| `/discovery-process` | Discovery workflow |
| `/problem-statement` | Frame the problem |
| `/jobs-to-be-done` | JTBD analysis |
| `/customer-journey-map` | Journey mapping |
| `/user-story` | User story authoring |
| `/user-story-mapping` | Story mapping workshop |
| `/user-story-splitting` | Split epics into stories |
| `/epic-breakdown-advisor` | Epic decomposition advice |
| `/prioritization-advisor` | Prioritization help |
| `/roadmap-planning` | Roadmap planning |

## Web quality skills

| Command | Use when |
|---------|----------|
| `/accessibility` | A11y review (WCAG) |
| `/best-practices` | Web best practices |
| `/core-web-vitals` | LCP, INP, CLS |
| `/performance` | Performance audit |
| `/seo` | SEO review |
| `/web-quality-audit` | Full web quality pass |

## Default behavior

1. Read `context/MEMORY.md` and any `drom-plans/` with `status: in-progress`
2. Pick the narrowest matching skill above — do not improvise a parallel workflow
3. Follow `workflows/` when the task matches a known pattern
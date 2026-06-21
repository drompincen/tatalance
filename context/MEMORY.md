# Session Memory

## Current Focus

Mobile journey test uplift plan (`drom-plans/mobile-journey-tests-uplift.md`) — **completed** 2026-06-20.

## Recent Decisions

- Canonical Playwright lives under `tests/e2e/`; root `package.json` delegates scripts only.
- Local E2E uses `workers: 2` when `BASE_URL` is localhost (single JVM + Flapdoodle can't sustain 6 workers).
- `AppPage.goto()` waits for heading visibility, not `networkidle` (app polls every 15s via `setInterval`).
- Mobile header overflow fixed: `overflow-x: clip`, compact user-menu, hide Link Google on ≤768px.
- P2 AC deferred: pull-to-refresh, keyboard-overlap, network-retry, real WebKit.

## Key Findings

- **Closed-loop final (iteration 3):** `mvn test` 163 pass; mobile E2E 38 pass; full E2E 62 pass (localhost, 2 workers).
- Flaky without worker cap: desktop API timeouts under 6 parallel workers against one local server.
- Axe on rides tab: scope to `#tab-rides`, 60s timeout when parallel seeding inflates DOM.

## Open Questions

- Should CI add a dedicated mobile-only job vs full suite on every deploy? (Currently all specs on EB.)

## Session Log

### Mobile journey uplift — closed loop

**Iteration 0 (baseline):** 29 mobile E2E; dual Playwright configs; README stale.

**Iteration 1:** Ch.2 infra + Ch.3–4 uplift implemented. Mobile 38 pass; Java 163 pass; full E2E 58 pass / 4 desktop flake.

**Iteration 2:** Fixed dual `@playwright/test`, Chromium install, M3 Today date, mobile-seed `expect` import. Mobile 38 pass; full E2E 61 pass (1 axe timeout).

**Iteration 3 (confirm):** CSS overflow fix, axe scope/timeout, `networkidle` removal, local `workers: 2`. **62/62 E2E pass**, **163/163 Java pass**.
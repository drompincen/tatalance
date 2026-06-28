# Project Conventions

Patterns agents and developers should follow in Tatalance. For workflow rules see `CLAUDE.md`; for architecture history see `context/DECISIONS.md`.

## Naming

- Java packages: `com.tatalance.<domain>` (e.g. `client`, `ride`, `profile`)
- REST paths: `/api/<resource>` plural (`/api/clients`, `/api/rides`)
- Static UI: `backend/src/main/resources/static/` — `index.html` (chauffeur), `freelance.html` (freelance)
- Issue commits: `feat: short description (#N)` — types: `feat`, `fix`, `test`, `infra`, `docs`, `refactor`

## API and errors

- Validation errors: `{ "errors": [ { "field": "...", "message": "..." } ] }` via `GlobalExceptionHandler`
- Spanish messages when client sends `Accept-Language: es` (`ApiMessageResolver`)
- Cross-user access: return **404** (not 403) for other users' resources
- Phone format: E.164 (`+[1-9]` + 9–14 digits) on clients and drivers

## Frontend

- i18n: `static/js/i18n.js` — `data-i18n` attributes, `t('key')`, `localStorage` key `tatalance-lang`
- API calls: `fetch` wrapper in `i18n.js` adds `Accept-Language` on `/api/*`
- Instant fields in tests/API: ISO-8601 with `Z` suffix (`2026-06-16T14:00:00Z`)

## Testing

- Unit/controller: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockBean` repos; `addFilters = false` when security not under test
- Integration: Flapdoodle embedded Mongo via **Windows Maven** or IntelliJ (not WSL native ARM64 `mvn`)
- E2E: Playwright in `tests/e2e/`; prefer API assertions over fragile DOM scroll checks; clean up created data after tests
- Coverage gate on `main`: Jacoco ≥ 60% line and branch
- **Pre-push gate:** Always run `mvn clean verify` locally before `git push`. Broken pushes block deploys silently — the EB environment stays on old code with no warning until someone checks CI

## Plans and issues

- **What's next:** `drom-plans/tatalance-v1.md` (story status table + epic dependencies)
- **How to implement:** GitHub issue body (acceptance checkboxes)
- After shipping: update plan row **and** close/link issue in same PR session (see `CLAUDE.md` Workflow Rules)
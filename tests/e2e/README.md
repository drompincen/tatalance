# Tatalance E2E Tests

Playwright tests for the Spring Boot app. Mobile journey specs (M1–M5) run on
iPhone SE viewport via Chromium emulation.

## Prerequisites

1. Start the backend (embedded Mongo, port 8080):

```powershell
cd backend
mvn spring-boot:run
```

2. Install deps (once):

```powershell
# From repo root (recommended for local dev)
npm install
npx playwright install chromium

# Or from this directory (CI path)
npm ci
npx playwright install chromium
```

## Run locally (repo root)

```powershell
npm run test:e2e:mobile    # M1–M5 only, iPhone SE
npm run test:e2e           # mobile + desktop projects
npm run test:e2e:headed
npm run test:e2e:ui
npm run report
```

## Run CI path (this directory)

Deploy workflow uses `tests/e2e/playwright.config.ts` with `E2E_BASE_URL` pointing
at the Elastic Beanstalk environment:

```powershell
cd tests/e2e
$env:E2E_BASE_URL = "http://tatalance-qa.eba-7u2dj39y.us-east-1.elasticbeanstalk.com"
npm run test:e2e:mobile
```

## Auth

Most specs use HTTP Basic `admin:admin` via global headers. M5 login specs opt
out with `test.use({ extraHTTPHeaders: {} })` to exercise form login.

```powershell
$env:APP_USERNAME = "admin"; $env:APP_PASSWORD = "admin"; npm run test:e2e
```

## Mobile journey coverage

| Issue | Spec | Status |
|-------|------|--------|
| #31 M1 Responsive shell | `m1-responsive-shell.spec.ts` | implemented |
| #31 M1 Auth pages | `m1-auth-pages-mobile.spec.ts` | implemented |
| #32 M2 David books | `m2-david-books-mobile.spec.ts` | implemented |
| #33 M3 Driver queue | `m3-driver-queue.spec.ts` | implemented |
| #34 M4 Start + complete | `m4-driver-start-complete.spec.ts` | implemented |
| #35 M5 Login | `m5-login-mobile.spec.ts` | implemented |
| #35 M5 Register + forgot | `m5-register-forgot-mobile.spec.ts` | implemented |
| #93 M6 Jobs (freelance) | `m6-jobs-mobile.spec.ts` | implemented (booking, live timer, complete @ $20/hr) |

Shared helpers: `pages/mobile.page.ts`, `fixtures/mobile-seed.ts`.

## Notes

- Chromium ≠ real Mobile Safari; viewport/UA emulation is intentional.
- Pull-to-refresh on driver-queue is manual-only (not automatable in Playwright).
- Do not auto-start Maven from Playwright — port 8080 conflicts with a running backend.
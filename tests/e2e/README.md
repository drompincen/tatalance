# Tatalance E2E + Web-Quality Tests

Playwright + axe-core. Targets the running Spring Boot app at `http://localhost:8080`.

## Setup (once)

```bash
npm install
npx playwright install chromium
```

## Run

```bash
# All tests, all projects (mobile + desktop)
npm run test:e2e

# Mobile journey only (iPhone SE)
npm run test:e2e:mobile

# Headed (visible browser)
npm run test:e2e:headed

# Interactive UI
npm run test:e2e:ui

# Open last HTML report
npm run report
```

The Playwright config starts `mvn spring-boot:run` automatically if no server
is already listening on :8080. If you already have the backend running, it is
reused.

## Auth

Tests run with HTTP Basic `admin:admin` injected as a global header
(`extraHTTPHeaders`). Override via env:

```bash
APP_USERNAME=foo APP_PASSWORD=bar npm run test:e2e
```

## Mobile journey coverage

| Issue | Spec | Status |
|------|------|--------|
| #31 — M1 Responsive shell | `m1-responsive-shell.spec.ts` | implemented |
| #35 — M5 Login on iPhone   | `m5-login-mobile.spec.ts`     | implemented |
| #32 — M2 David books on iPhone | (pending)                 | pending |
| #33 — M3 Driver queue       | (pending)                     | pending |
| #34 — M4 Start + Complete   | (pending)                     | pending |

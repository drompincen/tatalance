import { defineConfig, devices } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';
const APP_USER = process.env.APP_USERNAME ?? 'admin';
const APP_PASS = process.env.APP_PASSWORD ?? 'admin';
const basicAuth = 'Basic ' + Buffer.from(`${APP_USER}:${APP_PASS}`).toString('base64');

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],

  use: {
    baseURL: BASE_URL,
    extraHTTPHeaders: { Authorization: basicAuth },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  // We emulate iPhone with Chromium (browserName override). Real WebKit
  // requires system libs that are awkward on WSL2; for the things these tests
  // assert (computed font size, scroll width, grid tracks, axe rules)
  // Chromium with the iPhone viewport + UA is sufficient. Add a real WebKit
  // project once a Linux box (CI) is available.
  projects: [
    {
      name: 'Mobile Safari (iPhone SE)',
      use: {
        ...devices['iPhone SE'],
        browserName: 'chromium',
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
    {
      name: 'Mobile Safari (iPhone 14)',
      use: {
        ...devices['iPhone 14'],
        browserName: 'chromium',
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
    {
      name: 'Desktop Chrome',
      use: {
        ...devices['Desktop Chrome'],
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
  ],

  // No webServer auto-start: spawning mvn from Playwright fights for port 8080
  // with any backend already running. Start the backend yourself with
  //   cd backend && mvn spring-boot:run
  // before running these tests. See tests/e2e/README.md.
});

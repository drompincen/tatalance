import { defineConfig, devices } from '@playwright/test';

// The app is protected by HTTP Basic auth (SecurityConfig). Every request
// carries the admin credentials; the M5 login spec opts out via
// `test.use({ extraHTTPHeaders: {} })` to exercise the form-login page.
const APP_USER = process.env.APP_USERNAME ?? 'admin';
const APP_PASS = process.env.APP_PASSWORD ?? 'admin';
const basicAuth = 'Basic ' + Buffer.from(`${APP_USER}:${APP_PASS}`).toString('base64');

const MOBILE_SPECS = /m\d+-.*\.spec\.ts$/;

export default defineConfig({
  testDir: '.',
  testMatch: '**/*.spec.ts',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list']],
  use: {
    baseURL:
      process.env.E2E_BASE_URL ||
      process.env.BASE_URL ||
      'http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com',
    extraHTTPHeaders: { Authorization: basicAuth },
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      // Mobile journey (M1–M5) — iPhone SE viewport, emulated via Chromium.
      name: 'Mobile Safari (iPhone SE)',
      testMatch: MOBILE_SPECS,
      use: {
        ...devices['iPhone SE'],
        browserName: 'chromium',
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
    {
      // Functional CRUD/billing specs — desktop viewport.
      name: 'Desktop Chrome',
      testIgnore: MOBILE_SPECS,
      use: {
        ...devices['Desktop Chrome'],
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
  ],
});

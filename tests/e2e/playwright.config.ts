import { defineConfig, devices } from '@playwright/test';

const MOBILE_SPECS = /m\d+-.*\.spec\.ts$/;
const APP_USER = process.env.APP_USERNAME ?? 'admin';
const APP_PASS = process.env.APP_PASSWORD ?? 'admin';
const basicAuth = 'Basic ' + Buffer.from(`${APP_USER}:${APP_PASS}`).toString('base64');
const baseURL =
  process.env.E2E_BASE_URL ||
  process.env.BASE_URL ||
  'http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com';

const isLocal =
  baseURL.includes('localhost') || baseURL.includes('127.0.0.1');

export default defineConfig({
  testDir: '.',
  testMatch: '**/*.spec.ts',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  // Single JVM + in-memory Mongo struggles under default parallel workers locally.
  workers: process.env.CI ? 2 : isLocal ? 2 : undefined,
  reporter: [['list']],

  use: {
    baseURL,
    extraHTTPHeaders: { Authorization: basicAuth },
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'Mobile Safari (iPhone SE)',
      testMatch: MOBILE_SPECS,
      use: {
        ...devices['iPhone SE'],
        browserName: 'chromium',
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
    {
      name: 'Desktop Chrome',
      testIgnore: MOBILE_SPECS,
      use: {
        ...devices['Desktop Chrome'],
        extraHTTPHeaders: { Authorization: basicAuth },
      },
    },
  ],
});
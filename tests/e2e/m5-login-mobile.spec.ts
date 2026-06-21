import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';

test.describe('M5 — Login on iPhone Safari', () => {
  test.use({ extraHTTPHeaders: {} });

  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
    await page.goto('/login.html');
    await expect(page).toHaveURL(/\/login\.html/);
  });

  test('viewport meta declares viewport-fit=cover', async () => {
    await mobile.assertViewportMeta();
  });

  test('username field has mobile-friendly autocomplete attrs', async ({ page }) => {
    const user = page.locator('input[name="username"]');
    await expect(user).toHaveAttribute('autocomplete', 'username');
    await expect(user).toHaveAttribute('autocapitalize', 'off');
  });

  test('password field uses current-password autocomplete', async ({ page }) => {
    const pwd = page.locator('input[name="password"]');
    await expect(pwd).toHaveAttribute('autocomplete', 'current-password');
    await expect(pwd).toHaveAttribute('type', 'password');
  });

  test('inputs use font-size >= 16px (no iOS auto-zoom)', async () => {
    await mobile.assertMinFontSize('input[name="username"], input[name="password"]');
  });

  test('submit button is at least 44x44px and in viewport', async ({ page }) => {
    const btn = page.locator('button[type="submit"], input[type="submit"]').first();
    await mobile.assertInViewport(btn);
    const box = await btn.boundingBox();
    expect(box!.height).toBeGreaterThanOrEqual(44);
    expect(box!.width).toBeGreaterThanOrEqual(44);
  });

  test('no horizontal scroll on iPhone SE', async () => {
    await mobile.assertNoHorizontalScroll();
  });

  test('valid credentials log the user in and reach the app', async ({ page }) => {
    await page.locator('input[name="username"]').fill('admin');
    await page.locator('input[name="password"]').fill('admin');
    await Promise.all([
      page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 10_000 }),
      page.locator('button[type="submit"], input[type="submit"]').first().click(),
    ]);
    await expect(page.locator('#hamburger')).toBeVisible();
  });
});
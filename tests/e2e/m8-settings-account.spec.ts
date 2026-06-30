import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';

test.describe('Chauffeur settings and account sheet', () => {
  test('Settings opens sheet and Account opens settings page', async ({ page }) => {
    const mobile = new MobilePage(page);
    await mobile.gotoApp();

    await page.locator('[data-test="bottom-nav-settings"]').click();
    await expect(page.locator('#more-sheet.open')).toBeVisible({ timeout: 3000 });
    await expect(page.locator('[data-test="more-nav-activity"]')).toBeVisible();

    await page.locator('[data-test="more-nav-account"]').click();
    await expect(page.locator('#more-sheet.open')).toHaveCount(0);
    await expect(page.locator('#tab-settings.active')).toBeVisible({ timeout: 3000 });
    await expect(page.locator('[data-test="account-venmo-input"]')).toBeVisible();
  });
});

test.describe('Freelance settings sheet', () => {
  test.use({ viewport: { width: 375, height: 667 } });

  test('Settings opens sheet with logout; Account opens settings view', async ({ page }) => {
    await page.goto('/login.html');
    await page.locator('input[name="username"]').fill('admin');
    await page.locator('input[name="password"]').fill('admin');
    await Promise.all([
      page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 15_000 }),
      page.locator('button[type="submit"], input[type="submit"]').first().click(),
    ]);
    await page.request.patch('/api/users/me/settings', { data: { businessMode: 'FREELANCE' } });
    await page.goto('/freelance.html');
    await expect(page.locator('[data-test="bottom-nav-dash"]')).toBeVisible({ timeout: 15_000 });

    await page.locator('[data-test="bottom-nav-settings"]').click();
    await expect(page.locator('#more-sheet.open')).toBeVisible({ timeout: 3000 });
    await expect(page.locator('[data-test="more-logout"]')).toBeVisible();

    await page.locator('[data-test="more-nav-account"]').click();
    await expect(page.locator('#more-sheet.open')).toHaveCount(0);
    await expect(page.locator('#view-settings.active')).toBeVisible();
    await expect(page.locator('#venmo-input')).toBeVisible();
  });
});
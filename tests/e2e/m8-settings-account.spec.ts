import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';

test.describe('Chauffeur settings and account sheet', () => {
  test('Settings opens sheet and Account opens profile panel', async ({ page }) => {
    const mobile = new MobilePage(page);
    await mobile.gotoApp();

    await page.locator('[data-test="bottom-nav-settings"]').click();
    await expect(page.locator('#more-sheet.open')).toBeVisible({ timeout: 3000 });
    await expect(page.locator('[data-test="more-nav-activity"]')).toBeVisible();

    await page.locator('[data-test="more-nav-account"]').click();
    await expect(page.locator('#more-sheet.open')).toHaveCount(0);
    await expect(page.locator('#account-menu-panel.open')).toBeVisible({ timeout: 3000 });
    await expect(page.locator('[data-test="account-venmo-input"]')).toBeVisible();
  });
});
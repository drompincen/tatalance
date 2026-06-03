import { test, expect } from '@playwright/test';
import { AppPage } from './pages/app.page';

test.describe('Smoke tests', () => {
  test('page loads and shows Tatalance title', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await expect(page).toHaveTitle(/Tatalance/);
  });

  test('all main tabs are visible', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    for (const tab of ['Clients', 'Drivers', 'Rides', 'Invoices', 'API Spec']) {
      await expect(page.locator('.tab-btn', { hasText: tab })).toBeVisible();
    }
  });

  test('+ button for custom tables is visible', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await expect(page.locator('.add-table-btn')).toBeVisible();
  });

  test('? help button is visible', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await expect(page.locator('.help-btn')).toBeVisible();
  });

  test('help overlay opens and closes', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await page.locator('.help-btn').click();
    await expect(page.locator('.help-overlay')).toHaveClass(/open/);
    await expect(page.locator('#help-title')).toContainText('Welcome');
    await page.locator('.help-close').click();
    await expect(page.locator('.help-overlay')).not.toHaveClass(/open/);
  });

  test('clients API returns JSON', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    const clients = await app.apiGet('/api/clients');
    expect(Array.isArray(clients)).toBe(true);
  });
});

import { test, expect } from '@playwright/test';

test.describe('i18n language toggle (#107)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/index.html');
    await page.evaluate(() => {
      localStorage.setItem('tatalance-lang', 'en');
    });
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Tatalance' })).toBeVisible();
  });

  test('switching to ES translates nav tabs and help overlay', async ({ page }) => {
    await expect(page.locator('#btn-clients')).toHaveText('Clients');
    await expect(page.locator('#btn-rides')).toHaveText('Rides');

    await page.locator('#langToggle .lang-option[data-lang="es"]').click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'es');
    await expect(page.locator('#btn-clients')).toHaveText('Clientes');
    await expect(page.locator('#btn-rides')).toHaveText('Viajes');
    await expect(page.locator('#btn-invoices')).toHaveText('Facturas');

    await page.locator('.help-btn').click();
    await expect(page.locator('.help-overlay')).toHaveClass(/open/);
    await expect(page.locator('#help-title')).toHaveText('Bienvenido a Tatalance');
    await expect(page.locator('#help-body')).toContainText('Tu herramienta de gestión');
    await expect(page.locator('#help-next')).toHaveText('Siguiente');

    await page.locator('#help-next').click();
    await expect(page.locator('#help-title')).toHaveText('Clientes');
    await page.locator('.help-close').click();
    await expect(page.locator('.help-overlay')).not.toHaveClass(/open/);
  });

  test('switching back to EN restores English labels', async ({ page }) => {
    await page.locator('#langToggle .lang-option[data-lang="es"]').click();
    await expect(page.locator('#btn-drivers')).toHaveText('Choferes');

    await page.locator('#langToggle .lang-option[data-lang="en"]').click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.locator('#btn-drivers')).toHaveText('Drivers');
    await expect(page.locator('#btn-clients')).toHaveText('Clients');
  });

  test('language preference persists in localStorage after reload', async ({ page }) => {
    await page.locator('#langToggle .lang-option[data-lang="es"]').click();
    await expect(page.locator('#btn-clients')).toHaveText('Clientes');

    await page.reload();
    await expect(page.getByRole('heading', { name: 'Tatalance' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'es');
    await expect(page.locator('#btn-clients')).toHaveText('Clientes');

    const stored = await page.evaluate(() => localStorage.getItem('tatalance-lang'));
    expect(stored).toBe('es');
  });

  test('help overlay shows English on default EN locale', async ({ page }) => {
    await page.locator('.help-btn').click();
    await expect(page.locator('#help-title')).toContainText('Welcome');
    await page.locator('.help-close').click();
  });
});
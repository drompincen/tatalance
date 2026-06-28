import { test, expect } from '@playwright/test';
import { AppPage, uniquePhone } from './pages/app.page';

test.describe('Client management', () => {
  const uniqueId = () => Date.now().toString(36);

  test('add a client and see it in the list', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    // Dashboard is the default tab; the client form lives on the Clients tab.
    await app.switchTab('Clients');

    const tag = uniqueId();
    const firstName = `E2E${tag}`;
    const lastName = `Bot${tag}`;
    const phone = uniquePhone();
    const email = `e2e-${tag}@test.com`;

    await page.fill('#f-firstName', firstName);
    await page.fill('#f-lastName', lastName);
    await page.fill('#f-phone', phone);
    await page.fill('#f-email', email);
    await page.click('#submit-btn');

    await expect(page.locator('#fb')).toContainText(`${firstName} ${lastName} added`);

    // verify via API that the client was persisted (large page so the new
    // row isn't buried past the default page size in the shared test DB)
    const clients = await app.apiGet('/api/clients?size=1000') as { id: string; firstName: string; lastName: string; phone: string }[];
    const created = clients.find(c => c.firstName === firstName);
    expect(created).toBeTruthy();
    expect(created!.lastName).toBe(lastName);
    expect(created!.phone).toBe(phone);

    // cleanup
    if (created) await app.apiDelete(`/api/clients/${created.id}`);
  });

  test('phone validation rejects invalid input', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Clients');

    await page.fill('#f-firstName', 'ValidFirst');
    await page.fill('#f-lastName', 'ValidLast');
    await page.fill('#f-phone', '12345');
    await page.click('#submit-btn');

    await expect(page.locator('#e-phone')).toBeVisible();
    await expect(page.locator('#e-phone')).toContainText(/valid US phone|10 digits/i);

    // client should NOT appear
    const row = page.locator('#client-list table tbody tr', { hasText: 'ValidFirst' });
    await expect(row).toHaveCount(0);
  });

  test('firstName required', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Clients');

    await page.fill('#f-firstName', '');
    await page.fill('#f-lastName', 'SomeLast');
    await page.fill('#f-phone', uniquePhone());
    await page.click('#submit-btn');

    await expect(page.locator('#e-firstName')).toBeVisible();
    await expect(page.locator('#e-firstName')).toContainText('First name is required');
  });

  test('lastName required', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Clients');

    await page.fill('#f-firstName', 'SomeFirst');
    await page.fill('#f-lastName', '');
    await page.fill('#f-phone', uniquePhone());
    await page.click('#submit-btn');

    await expect(page.locator('#e-lastName')).toBeVisible();
    await expect(page.locator('#e-lastName')).toContainText('Last name is required');
  });
});

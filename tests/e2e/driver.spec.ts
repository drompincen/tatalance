import { test, expect } from '@playwright/test';
import { AppPage } from './pages/app.page';

test.describe('Driver management', () => {
  const uniqueId = () => Date.now().toString(36);

  test('add a driver and verify via API', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Drivers');

    const tag = uniqueId();
    const firstName = `D2E${tag}`;
    const lastName = `Bot${tag}`;
    const phone = '+12125550010';

    await page.fill('#d-firstName', firstName);
    await page.fill('#d-lastName', lastName);
    await page.fill('#d-phone', phone);
    await page.fill('#d-payoutRate', '70');
    await page.click('#driver-submit-btn');

    await expect(page.locator('#driver-fb')).toContainText(`${firstName} ${lastName} added`);

    // verify via API
    const drivers = await app.apiGet('/api/drivers') as { id: string; firstName: string; availability: string }[];
    const created = drivers.find(d => d.firstName === firstName);
    expect(created).toBeTruthy();
    expect(created!.availability).toBe('AVAILABLE');

    // cleanup
    await app.apiDelete(`/api/drivers/${created!.id}`);
  });

  test('toggle availability persists after reload', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    // create driver via API
    const tag = uniqueId();
    const driver = await app.apiPost('/api/drivers', {
      firstName: `Avail${tag}`, lastName: `Test${tag}`,
      phone: '+12125550011', payoutType: 'PERCENTAGE', payoutRate: 60
    }) as { id: string };

    // change availability via API
    await page.request.patch(`/api/drivers/${driver.id}/availability`, {
      data: { availability: 'OFF_DUTY' }
    });

    // verify it persisted
    const drivers = await app.apiGet('/api/drivers') as { id: string; availability: string }[];
    const updated = drivers.find(d => d.id === driver.id);
    expect(updated!.availability).toBe('OFF_DUTY');

    // cleanup
    await app.apiDelete(`/api/drivers/${driver.id}`);
  });

  test('firstName required', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Drivers');

    await page.fill('#d-firstName', '');
    await page.fill('#d-lastName', 'SomeLast');
    await page.fill('#d-phone', '+12125550012');
    await page.fill('#d-payoutRate', '50');
    await page.click('#driver-submit-btn');

    await expect(page.locator('#de-firstName')).toBeVisible();
  });

  test('phone validation rejects invalid input', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Drivers');

    await page.fill('#d-firstName', 'ValidFirst');
    await page.fill('#d-lastName', 'ValidLast');
    await page.fill('#d-phone', '12345');
    await page.fill('#d-payoutRate', '50');
    await page.click('#driver-submit-btn');

    await expect(page.locator('#de-phone')).toBeVisible();
  });
});

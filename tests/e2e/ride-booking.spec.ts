import { test, expect } from '@playwright/test';
import { AppPage } from './pages/app.page';

test.describe('Ride booking', () => {
  const uniqueId = () => Date.now().toString(36);

  test('create a ride and verify status SCHEDULED', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    // seed client + driver via API
    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `RC${tag}`, lastName: `CL${tag}`, phone: '+12125550020'
    }) as { id: string; firstName: string; lastName: string };
    const driver = await app.apiPost('/api/drivers', {
      firstName: `RD${tag}`, lastName: `DR${tag}`, phone: '+12125550021',
      payoutType: 'PERCENTAGE', payoutRate: 70
    }) as { id: string };

    // navigate to Rides tab and wait for client dropdown to populate
    await app.switchTab('Rides');
    await expect(page.locator(`#r-clientId option[value="${client.id}"]`)).toBeAttached({ timeout: 10000 });

    // fill ride form
    await page.selectOption('#r-clientId', client.id);
    await page.fill('#r-pickupDateTime', '2026-06-15T10:00');
    await page.fill('#r-pickupLocation', 'Miami Airport');
    await page.fill('#r-dropoffLocation', 'South Beach Hotel');
    await page.fill('#r-basePrice', '85');
    await page.click('#ride-submit-btn');

    await expect(page.locator('#ride-fb')).toContainText('booked');

    // verify via API
    const rides = await app.apiGet('/api/rides') as { id: string; clientId: string; status: string; clientName: string }[];
    const created = rides.find(r => r.clientId === client.id);
    expect(created).toBeTruthy();
    expect(created!.status).toBe('SCHEDULED');
    expect(created!.clientName).toContain(client.firstName);

    // cleanup
    await app.apiDelete(`/api/rides/${created!.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('assign driver and verify status ASSIGNED', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    // seed client, driver, and ride via API
    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `AC${tag}`, lastName: `CL${tag}`, phone: '+12125550030'
    }) as { id: string };
    const driver = await app.apiPost('/api/drivers', {
      firstName: `AD${tag}`, lastName: `DR${tag}`, phone: '+12125550031',
      payoutType: 'PERCENTAGE', payoutRate: 60
    }) as { id: string };
    const ride = await app.apiPost('/api/rides', {
      clientId: client.id, pickupDateTime: '2026-06-16T14:00:00Z',
      pickupLocation: 'Downtown', dropoffLocation: 'Airport', basePrice: 100
    }) as { id: string };

    // assign via API
    await page.request.post(`/api/rides/${ride.id}/assign`, {
      data: { driverId: driver.id }
    });

    // verify
    const rides = await app.apiGet('/api/rides') as { id: string; status: string; assignedDriverName: string }[];
    const updated = rides.find(r => r.id === ride.id);
    expect(updated!.status).toBe('ASSIGNED');
    expect(updated!.assignedDriverName).toContain(`AD${tag}`);

    // cleanup
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('client required validation', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    await app.switchTab('Rides');

    // leave client unselected
    await page.fill('#r-pickupDateTime', '2026-06-15T10:00');
    await page.fill('#r-pickupLocation', 'Somewhere');
    await page.fill('#r-dropoffLocation', 'Elsewhere');
    await page.click('#ride-submit-btn');

    await expect(page.locator('#re-clientId')).toBeVisible();
  });

  test('pickup location required validation', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `VC${tag}`, lastName: `CL${tag}`, phone: '+12125550040'
    }) as { id: string };

    await app.switchTab('Rides');
    await expect(page.locator(`#r-clientId option[value="${client.id}"]`)).toBeAttached({ timeout: 10000 });

    await page.selectOption('#r-clientId', client.id);
    await page.fill('#r-pickupDateTime', '2026-06-15T10:00');
    await page.fill('#r-pickupLocation', '');
    await page.fill('#r-dropoffLocation', 'Somewhere');
    await page.click('#ride-submit-btn');

    await expect(page.locator('#re-pickupLocation')).toBeVisible();

    // cleanup
    await app.apiDelete(`/api/clients/${client.id}`);
  });
});

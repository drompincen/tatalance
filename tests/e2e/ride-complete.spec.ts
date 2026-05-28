import { test, expect } from '@playwright/test';
import { AppPage } from './pages/app.page';

test.describe('Ride completion', () => {
  const uniqueId = () => Date.now().toString(36);

  async function seedAssignedRide(app: AppPage) {
    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `CC${tag}`, lastName: `CL${tag}`, phone: '+12125550050'
    }) as { id: string };
    const driver = await app.apiPost('/api/drivers', {
      firstName: `CD${tag}`, lastName: `DR${tag}`, phone: '+12125550051',
      payoutType: 'PERCENTAGE', payoutRate: 70
    }) as { id: string };
    const ride = await app.apiPost('/api/rides', {
      clientId: client.id, pickupDateTime: '2026-06-20T09:00:00Z',
      pickupLocation: 'Hotel Lobby', dropoffLocation: 'Convention Center',
      basePrice: 75
    }) as { id: string };

    // assign driver
    const resp = await app.page.request.post(`/api/rides/${ride.id}/assign`, {
      data: { driverId: driver.id }
    });
    expect(resp.ok()).toBe(true);

    return { client, driver, ride, tag };
  }

  test('complete a ride and verify status + total', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    const { client, driver, ride } = await seedAssignedRide(app);

    // complete via API
    const resp = await page.request.post(`/api/rides/${ride.id}/complete`, {
      data: {
        actualStart: '2026-06-20T09:00:00Z',
        actualEnd: '2026-06-20T09:45:00Z',
        tolls: 5,
        parking: 10
      }
    });
    expect(resp.ok()).toBe(true);

    // verify
    const rides = await app.apiGet('/api/rides') as { id: string; status: string; totalAmount: number }[];
    const completed = rides.find(r => r.id === ride.id);
    expect(completed).toBeTruthy();
    expect(completed!.status).toBe('COMPLETED');
    expect(completed!.totalAmount).toBe(90); // 75 base + 5 tolls + 10 parking

    // cleanup
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('cannot complete a SCHEDULED ride', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `SC${tag}`, lastName: `CL${tag}`, phone: '+12125550052'
    }) as { id: string };
    const ride = await app.apiPost('/api/rides', {
      clientId: client.id, pickupDateTime: '2026-06-21T10:00:00Z',
      pickupLocation: 'A', dropoffLocation: 'B', basePrice: 50
    }) as { id: string };

    // try to complete a SCHEDULED ride — should fail
    const resp = await page.request.post(`/api/rides/${ride.id}/complete`, {
      data: {
        actualStart: '2026-06-21T10:00:00Z',
        actualEnd: '2026-06-21T10:30:00Z'
      }
    });
    expect(resp.status()).toBe(400);

    // cleanup
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('complete without actualStart is rejected', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    const { client, driver, ride } = await seedAssignedRide(app);

    // missing actualStart
    const resp = await page.request.post(`/api/rides/${ride.id}/complete`, {
      data: {
        actualEnd: '2026-06-20T09:45:00Z'
      }
    });
    expect(resp.status()).toBe(400);

    // cleanup
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });
});

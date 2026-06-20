import { test, expect } from '@playwright/test';
import { AppPage, uniquePhone, futureDateTime } from './pages/app.page';

test.describe('Billing', () => {
  const uniqueId = () => Date.now().toString(36);

  /** Seed a fully completed ride ready for invoicing */
  async function seedCompletedRide(app: AppPage) {
    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `BC${tag}`, lastName: `CL${tag}`, phone: uniquePhone()
    }) as { id: string };
    const driver = await app.apiPost('/api/drivers', {
      firstName: `BD${tag}`, lastName: `DR${tag}`, phone: uniquePhone(),
      payoutType: 'PERCENTAGE', payoutRate: 70
    }) as { id: string };
    const ride = await app.apiPost('/api/rides', {
      clientId: client.id, pickupDateTime: futureDateTime(),
      pickupLocation: 'Office', dropoffLocation: 'Airport', basePrice: 100
    }) as { id: string };

    await app.page.request.post(`/api/rides/${ride.id}/assign`, {
      data: { driverId: driver.id }
    });
    // start before complete — complete requires IN_PROGRESS
    await app.page.request.post(`/api/rides/${ride.id}/start`);
    await app.page.request.post(`/api/rides/${ride.id}/complete`, {
      data: {
        tolls: 5, parking: 10
      }
    });

    return { client, driver, ride, tag };
  }

  test('generate invoice from completed ride', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    const { client, driver, ride } = await seedCompletedRide(app);

    // generate invoice via API
    const invoice = await app.apiPost('/api/invoices', { rideId: ride.id }) as {
      id: string; invoiceNumber: string; status: string; total: number; tax: number; baseCharge: number
    };

    expect(invoice.invoiceNumber).toMatch(/^INV-\d{4}-\d{3}$/);
    expect(invoice.status).toBe('OUTSTANDING');
    expect(invoice.baseCharge).toBe(100);
    // total = baseCharge + extras(tolls 5 + parking 10) + 8% tax
    // baseCharge=100, extras=15, subtotal=115, tax=115*0.08=9.2, total=124.2
    expect(invoice.total).toBeCloseTo(124.2, 1);

    // verify it appears in the list (large page so it isn't past the default page size)
    const invoices = await app.apiGet('/api/invoices?size=1000') as { id: string }[];
    expect(invoices.some(i => i.id === invoice.id)).toBe(true);

    // cleanup
    await app.apiDelete(`/api/invoices/${invoice.id}`);
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('mark invoice as paid', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();
    const { client, driver, ride } = await seedCompletedRide(app);

    const invoice = await app.apiPost('/api/invoices', { rideId: ride.id }) as { id: string };

    // mark paid
    const resp = await page.request.post(`/api/invoices/${invoice.id}/mark-paid`);
    expect(resp.ok()).toBe(true);
    const paid = await resp.json();
    expect(paid.status).toBe('PAID');

    // cleanup
    await app.apiDelete(`/api/invoices/${invoice.id}`);
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/drivers/${driver.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('cannot invoice a non-COMPLETED ride', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    const tag = uniqueId();
    const client = await app.apiPost('/api/clients', {
      firstName: `NC${tag}`, lastName: `CL${tag}`, phone: uniquePhone()
    }) as { id: string };
    const ride = await app.apiPost('/api/rides', {
      clientId: client.id, pickupDateTime: futureDateTime(),
      pickupLocation: 'A', dropoffLocation: 'B', basePrice: 50
    }) as { id: string };

    // ride is SCHEDULED, not COMPLETED
    const resp = await page.request.post('/api/invoices', {
      data: { rideId: ride.id }
    });
    expect(resp.status()).toBe(400);

    // cleanup
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });
});

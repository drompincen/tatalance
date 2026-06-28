import { test, expect } from '@playwright/test';
import { AppPage, uniquePhone, futureDateTime } from './pages/app.page';

test.describe('Freelance timer flow', () => {
  const uniqueId = () => Date.now().toString(36);

  test('API: book → start → pause → resume → complete → invoice → mark paid', async ({ page }) => {
    const app = new AppPage(page);
    await app.goto();

    await page.request.patch('/api/users/me/settings', {
      data: { businessMode: 'FREELANCE', defaultHourlyRate: 25 },
    });

    const tag = uniqueId();
    const client = (await app.apiPost('/api/clients', {
      firstName: `FL${tag}`,
      lastName: `CL${tag}`,
      phone: uniquePhone(),
    })) as { id: string };

    const ride = (await app.apiPost('/api/rides', {
      clientId: client.id,
      jobTitle: `E2E freelance ${tag}`,
      notes: 'Timer flow',
      pickupDateTime: futureDateTime(),
      pickupLocation: 'Remote',
      dropoffLocation: 'Remote',
      pricingMode: 'HOURLY',
      hourlyRate: 25,
    })) as { id: string; status: string; workSegments: unknown[] };

    expect(ride.status).toBe('SCHEDULED');
    expect(ride.workSegments).toHaveLength(0);

    const startResp = await page.request.post(`/api/rides/${ride.id}/start`, { data: {} });
    expect(startResp.ok()).toBe(true);
    const started = await startResp.json();
    expect(started.status).toBe('IN_PROGRESS');
    expect(started.workSegments.length).toBeGreaterThanOrEqual(1);

    const timerResp = await page.request.get(`/api/rides/${ride.id}/timer`);
    expect(timerResp.ok()).toBe(true);
    const timer = await timerResp.json();
    expect(timer.running).toBe(true);
    expect(timer.hourlyRate).toBe(25);

    const pauseResp = await page.request.post(`/api/rides/${ride.id}/timer/pause`, { data: {} });
    expect(pauseResp.ok()).toBe(true);
    const paused = await pauseResp.json();
    expect(paused.status).toBe('PAUSED');
    expect(paused.workSegments.length).toBe(1);
    expect(paused.workSegments[0].endedAt).toBeTruthy();

    const resumeResp = await page.request.post(`/api/rides/${ride.id}/timer/resume`, { data: {} });
    expect(resumeResp.ok()).toBe(true);
    const resumed = await resumeResp.json();
    expect(resumed.status).toBe('IN_PROGRESS');
    expect(resumed.workSegments.length).toBe(2);

    const completeResp = await page.request.post(`/api/rides/${ride.id}/complete`, { data: {} });
    expect(completeResp.ok()).toBe(true);
    const completed = await completeResp.json();
    expect(completed.status).toBe('COMPLETED');

    const invoice = (await app.apiPost('/api/invoices', { rideId: ride.id })) as {
      id: string;
      invoiceNumber: string;
      status: string;
      pricingMode: string;
    };
    expect(invoice.invoiceNumber).toMatch(/^INV-\d{4}-\d{3}$/);
    expect(invoice.status).toBe('OUTSTANDING');
    expect(invoice.pricingMode).toBe('HOURLY');

    const paidResp = await page.request.post(`/api/invoices/${invoice.id}/mark-paid`);
    expect(paidResp.ok()).toBe(true);
    const paid = await paidResp.json();
    expect(paid.status).toBe('PAID');

    await app.apiDelete(`/api/invoices/${invoice.id}`);
    await app.apiDelete(`/api/rides/${ride.id}`);
    await app.apiDelete(`/api/clients/${client.id}`);
  });

  test('UI: freelance page loads and shows Jobs nav', async ({ page }) => {
    await page.goto('/freelance.html');
    await expect(page.locator('h1')).toContainText('Tatalance');
    await expect(page.locator('.side .nav button[data-view="jobs"]')).toBeVisible();
    await expect(page.locator('#book-btn')).toContainText('Book job');
  });
});
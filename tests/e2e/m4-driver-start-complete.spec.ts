import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';
import { seedAssignedRide, seedClient, uniq } from './fixtures/mobile-seed';

test.describe('M4 — Driver start + complete on iPhone', () => {
  test('Start button transitions ride to IN_PROGRESS', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = (await seedClient(request)).id;
    const ride = await seedAssignedRide(request, clientId, driverId);
    expect(ride.status).toBe('SCHEDULED');

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await expect(card.locator('[data-test="start-btn"]')).toBeVisible();
    await card.locator('[data-test="start-btn"]').click();

    await expect(page.locator(`[data-ride-id="${ride.id}"] [data-test="status-badge"]`))
      .toContainText('IN_PROGRESS');
    await expect(page.locator(`[data-ride-id="${ride.id}"] [data-test="complete-btn"]`)).toBeVisible();
  });

  test('live stopwatch ticks while ride is in progress', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = (await seedClient(request)).id;
    const ride = await seedAssignedRide(request, clientId, driverId);
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const stopwatch = page.locator(`[data-ride-id="${ride.id}"] .stopwatch`);
    await expect(stopwatch).toBeVisible();
    const t0 = await stopwatch.textContent();
    await page.waitForTimeout(1500);
    const t1 = await stopwatch.textContent();
    expect(t0).toMatch(/00:00:0\d/);
    expect(t1).not.toBe(t0);
  });

  test('Complete form calculates billable live as driver edits extras', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = (await seedClient(request)).id;
    const ride = await seedAssignedRide(request, clientId, driverId, 100);
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();

    const form = card.locator('[data-test="complete-form"]');
    await expect(form).toHaveClass(/open/);
    await expect(form.locator('[data-test="billable"]')).toHaveText('$100.00');

    await form.locator(`#tolls-${ride.id}`).fill('8.50');
    await form.locator(`#parking-${ride.id}`).fill('12');
    await form.locator(`#extras-${ride.id}`).fill('15');
    await expect(form.locator('[data-test="billable"]')).toHaveText('$135.50');
  });

  test('Confirming complete updates status and persists billable to backend', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = (await seedClient(request)).id;
    const ride = await seedAssignedRide(request, clientId, driverId, 50);
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();

    const form = card.locator('[data-test="complete-form"]');
    await form.locator(`#tolls-${ride.id}`).fill('5');
    await form.locator(`#parking-${ride.id}`).fill('10');
    await form.locator(`#extras-${ride.id}`).fill('5');
    await form.locator('[data-test="confirm-complete-btn"]').click();

    await expect(page.locator(`[data-ride-id="${ride.id}"] [data-test="status-badge"]`))
      .toContainText('COMPLETED');

    const fetched = await (await request.get(`/api/rides/${ride.id}`)).json();
    expect(fetched.status).toBe('COMPLETED');
    expect(Number(fetched.billableAmount)).toBe(70);
  });

  test('inputs in the complete form use 16px font (no iOS auto-zoom)', async ({ page, request }) => {
    const mobile = new MobilePage(page);
    const driverId = `drv-${uniq()}`;
    const clientId = (await seedClient(request)).id;
    const ride = await seedAssignedRide(request, clientId, driverId);
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();
    await mobile.assertMinFontSize(`[data-ride-id="${ride.id}"] [data-test="complete-form"] input`);
  });
});
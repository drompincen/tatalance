import { test, expect } from '@playwright/test';

/**
 * Mobile journey M4 — Driver starts and completes a ride from the field.
 * Issue #34 / docs/journeys/mobile-iphone-journey.md
 *
 * Backend endpoints exercised:
 *   POST /api/rides/{id}/start   → IN_PROGRESS, actualStart = now
 *   POST /api/rides/{id}/complete → COMPLETED, billable = basePrice + extras
 */

const uniq = () => Math.random().toString(36).slice(2, 8);

async function seedClient(request: any) {
  const r = await request.post('/api/clients', {
    data: {
      firstName: `M4-${uniq()}`,
      lastName: 'Pax',
      phone: `+1786555${Math.floor(1000 + Math.random() * 9000)}`,
    },
  });
  expect(r.ok()).toBeTruthy();
  return (await r.json()).id;
}

async function seedAssignedRide(
  request: any,
  clientId: string,
  driverId: string,
  basePrice: number = 100,
) {
  const r = await request.post('/api/rides', {
    data: {
      clientId,
      pickupDateTime: '2026-11-01T09:00:00Z',
      pickupLocation: 'MIA',
      dropoffLocation: 'Brickell',
      basePrice,
      assignedDriverId: driverId,
    },
  });
  expect(r.ok()).toBeTruthy();
  return await r.json();
}

test.describe('M4 — Driver start + complete on iPhone', () => {
  test('Start button transitions ride to IN_PROGRESS', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = await seedClient(request);
    const ride = await seedAssignedRide(request, clientId, driverId);
    expect(ride.status).toBe('SCHEDULED');

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await expect(card.locator('[data-test="start-btn"]')).toBeVisible();
    await expect(card.locator('[data-test="status-badge"]')).toContainText('SCHEDULED');

    await card.locator('[data-test="start-btn"]').click();

    // After Start, the card re-renders into IN_PROGRESS state
    const refreshedCard = page.locator(`[data-ride-id="${ride.id}"]`);
    await expect(refreshedCard.locator('[data-test="status-badge"]')).toContainText('IN_PROGRESS');
    await expect(refreshedCard.locator('[data-test="complete-btn"]')).toBeVisible();
    await expect(refreshedCard.locator('[data-test="start-btn"]')).toHaveCount(0);
  });

  test('Complete form calculates billable live as driver edits extras', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = await seedClient(request);
    const ride = await seedAssignedRide(request, clientId, driverId, 100);

    // Move ride to IN_PROGRESS via the API so we land on the complete UI
    const startResp = await request.post(`/api/rides/${ride.id}/start`);
    expect(startResp.ok()).toBeTruthy();

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();

    const form = card.locator('[data-test="complete-form"]');
    await expect(form).toBeVisible();

    // Initial: just base price = $100.00
    await expect(form.locator('[data-test="billable"]')).toHaveText('$100.00');

    // Add tolls + parking + extras = 110.50
    await form.locator(`#tolls-${ride.id}`).fill('8.50');
    await form.locator(`#parking-${ride.id}`).fill('12');
    await form.locator(`#extras-${ride.id}`).fill('15');

    // 100 + 8.5 + 12 + 15 = 135.50
    await expect(form.locator('[data-test="billable"]')).toHaveText('$135.50');
  });

  test('Confirming complete updates status and persists billable to backend', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = await seedClient(request);
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

    // Card re-renders into COMPLETED state
    await expect(page.locator(`[data-ride-id="${ride.id}"] [data-test="status-badge"]`))
        .toContainText('COMPLETED');

    // Verify the backend persisted the calculated billableAmount
    const fetched = await (await request.get(`/api/rides/${ride.id}`)).json();
    expect(fetched.status).toBe('COMPLETED');
    expect(Number(fetched.billableAmount)).toBe(70); // 50 + 5 + 10 + 5
    expect(fetched.actualEnd).toBeTruthy();
  });

  test('inputs in the complete form use 16px font (no iOS auto-zoom)', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const clientId = await seedClient(request);
    const ride = await seedAssignedRide(request, clientId, driverId);
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();

    const fontSizes = await card.locator('[data-test="complete-form"] input').evaluateAll(
      (els) => els.map((el) => parseFloat(getComputedStyle(el as HTMLElement).fontSize)),
    );
    for (const fs of fontSizes) {
      expect(fs).toBeGreaterThanOrEqual(16);
    }
  });
});

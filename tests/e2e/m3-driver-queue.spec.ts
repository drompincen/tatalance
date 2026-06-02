import { test, expect } from '@playwright/test';

/**
 * Mobile journey M3 — Driver views their own ride queue.
 * Issue #33 / docs/journeys/mobile-iphone-journey.md
 *
 * Single-purpose page at /driver-queue.html?id={driverId}. No sidebar/nav.
 * Renders rides assigned to that driver (via the GET /api/drivers/{id}/rides
 * endpoint added in this story), sorted by pickup time, with status badge.
 *
 * The MVP "assign driver" UI flow is not yet built; this test seeds rides
 * by POSTing /api/rides with assignedDriverId in the body (the controller
 * passes the field through unchanged on create).
 */

const uniq = () => Math.random().toString(36).slice(2, 8);

async function seedClient(request: any) {
  const firstName = `Q-${uniq()}`;
  const lastName = 'Client';
  const r = await request.post('/api/clients', {
    data: {
      firstName,
      lastName,
      phone: `+1786555${Math.floor(1000 + Math.random() * 9000)}`,
    },
  });
  expect(r.ok(), 'seed client').toBeTruthy();
  const id = (await r.json()).id;
  return { id, firstName, lastName, fullName: `${firstName} ${lastName}` };
}

async function seedRide(
  request: any,
  clientId: string,
  driverId: string,
  pickupISO: string,
  from: string,
  to: string,
) {
  const r = await request.post('/api/rides', {
    data: {
      clientId,
      pickupDateTime: pickupISO,
      pickupLocation: from,
      dropoffLocation: to,
      assignedDriverId: driverId,
    },
  });
  expect(r.ok(), 'seed ride').toBeTruthy();
  return await r.json();
}

test.describe('M3 — Driver queue on iPhone', () => {
  test('empty state when the driver has no rides', async ({ page }) => {
    const driverId = `drv-empty-${uniq()}`;
    await page.goto(`/driver-queue.html?id=${driverId}`);
    await expect(page.locator('[data-test="empty-state"]')).toBeVisible();
  });

  test('renders the driver-scoped queue with no global nav', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const { id: clientId } = await seedClient(request);
    await seedRide(request, clientId, driverId, '2026-09-01T09:00:00Z', 'MIA', 'Brickell');

    await page.goto(`/driver-queue.html?id=${driverId}`);

    // No sidebar / tab nav from the main app
    await expect(page.locator('#btn-clients')).toHaveCount(0);
    await expect(page.locator('#btn-drivers')).toHaveCount(0);
    await expect(page.locator('#btn-rides')).toHaveCount(0);

    // At least one card present
    await expect(page.locator('[data-test="ride-card"]').first()).toBeVisible();
  });

  test('cards show pickup time, client name, route, and status', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const { id: clientId, fullName } = await seedClient(request);

    await seedRide(request, clientId, driverId, '2026-09-15T14:30:00Z', 'Coral Gables', 'Wynwood');
    await page.goto(`/driver-queue.html?id=${driverId}`);

    const card = page.locator('[data-test="ride-card"]').first();
    await expect(card).toBeVisible();
    await expect(card).toContainText(fullName);
    await expect(card).toContainText('Coral Gables');
    await expect(card).toContainText('Wynwood');
    await expect(card.locator('[data-test="status-badge"]')).toContainText('SCHEDULED');
  });

  test('rides sorted ascending by pickup time', async ({ page, request }) => {
    const driverId = `drv-${uniq()}`;
    const { id: clientId } = await seedClient(request);

    // Insert in reverse order to verify the page (or API) sorts
    await seedRide(request, clientId, driverId, '2026-10-05T20:00:00Z', 'Late', 'Spot');
    await seedRide(request, clientId, driverId, '2026-10-05T07:00:00Z', 'Early', 'Spot');
    await seedRide(request, clientId, driverId, '2026-10-05T13:00:00Z', 'Mid', 'Spot');

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const cards = page.locator('[data-test="ride-card"]');
    await expect(cards).toHaveCount(3);
    await expect(cards.nth(0)).toContainText('Early');
    await expect(cards.nth(1)).toContainText('Mid');
    await expect(cards.nth(2)).toContainText('Late');
  });

  test('no horizontal scroll on iPhone SE', async ({ page }) => {
    await page.goto(`/driver-queue.html?id=any-driver`);
    const overflow = await page.evaluate(() => ({
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth,
    }));
    expect(overflow.sw).toBeLessThanOrEqual(overflow.cw + 1);
  });

  test('viewport meta declares viewport-fit=cover', async ({ page }) => {
    await page.goto(`/driver-queue.html?id=any-driver`);
    const content = await page.locator('meta[name="viewport"]').getAttribute('content');
    expect(content).toMatch(/viewport-fit=cover/);
  });
});

import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';
import { laterTodayISO, seedClient, seedDriver, seedRide, uniq } from './fixtures/mobile-seed';

test.describe('M3 — Driver queue on iPhone', () => {
  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
  });

  test('empty state when the driver has no rides', async ({ page }) => {
    await page.goto(`/driver-queue.html?id=drv-empty-${uniq()}`);
    await expect(page.locator('[data-test="empty-state"]')).toBeVisible();
    await expect(page.locator('[data-test="empty-state"]')).toContainText(/refreshes automatically/i);
  });

  test('renders the driver-scoped queue with no global nav', async ({ page, request }) => {
    const { id: driverId } = await seedDriver(request);
    const { id: clientId } = await seedClient(request);
    const ride = await seedRide(request, clientId, driverId, futureDate(30), 'MIA', 'Brickell');
    await request.post(`/api/rides/${ride.id}/assign`, { data: { driverId } });

    await page.goto(`/driver-queue.html?id=${driverId}`);
    await expect(page.locator('#btn-clients')).toHaveCount(0);
    await expect(page.locator('[data-test="ride-card"]').first()).toBeVisible();
  });

  test('cards show pickup time, client name, route, and status', async ({ page, request }) => {
    const { id: driverId } = await seedDriver(request);
    const { id: clientId, fullName } = await seedClient(request);
    const ride = await seedRide(request, clientId, driverId, futureDate(45), 'Coral Gables', 'Wynwood');
    await request.post(`/api/rides/${ride.id}/assign`, { data: { driverId } });

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator('[data-test="ride-card"]').first();
    await expect(card).toContainText(fullName);
    await expect(card).toContainText('Coral Gables');
    await expect(card).toContainText('Wynwood');
    await expect(card.locator('[data-test="status-badge"]')).toContainText('ASSIGNED');
  });

  test('rides sorted ascending by pickup time', async ({ page, request }) => {
    const { id: driverId } = await seedDriver(request);
    const { id: clientId } = await seedClient(request);
    for (const [days, label] of [[60, 'Late'], [31, 'Early'], [45, 'Mid']] as const) {
      const ride = await seedRide(request, clientId, driverId, futureDate(days), label, 'Spot');
      await request.post(`/api/rides/${ride.id}/assign`, { data: { driverId } });
    }

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const cards = page.locator('[data-test="ride-card"]');
    await expect(cards).toHaveCount(3);
    await expect(cards.nth(0)).toContainText('Early');
    await expect(cards.nth(1)).toContainText('Mid');
    await expect(cards.nth(2)).toContainText('Late');
  });

  test('today rides grouped under Today heading', async ({ page, request }) => {
    const { id: driverId } = await seedDriver(request);
    const { id: clientId } = await seedClient(request);
    const ride = await seedRide(request, clientId, driverId, laterTodayISO(), 'Now', 'Here');
    await request.post(`/api/rides/${ride.id}/assign`, { data: { driverId } });

    await page.goto(`/driver-queue.html?id=${driverId}`);
    await expect(page.locator('.day-heading', { hasText: 'Today' })).toBeVisible();
  });

  test('complete form opens inline on the ride card', async ({ page, request }) => {
    const { id: driverId } = await seedDriver(request);
    const { id: clientId } = await seedClient(request);
    const ride = await seedRide(request, clientId, driverId, futureDate(20), 'A', 'B', { basePrice: 50 });
    await request.post(`/api/rides/${ride.id}/assign`, { data: { driverId } });
    await request.post(`/api/rides/${ride.id}/start`);

    await page.goto(`/driver-queue.html?id=${driverId}`);
    const card = page.locator(`[data-ride-id="${ride.id}"]`);
    await card.locator('[data-test="complete-btn"]').click();
    const form = card.locator('[data-test="complete-form"]');
    await expect(form).toHaveClass(/open/);
    await expect(form).toBeVisible();
  });

  test('shows back link to dashboard and refresh status', async ({ page }) => {
    await page.goto(`/driver-queue.html?id=drv-${uniq()}`);
    await expect(page.locator('[data-test="back-link"]')).toHaveAttribute('href', '/index.html');
    await expect(page.locator('[data-test="refresh-status"]')).toContainText(/last updated/i);
    await expect(page.locator('[data-test="refresh-btn"]')).toBeVisible();
  });

  test('no horizontal scroll on iPhone SE', async ({ page }) => {
    await page.goto(`/driver-queue.html?id=any-driver`);
    await mobile.assertNoHorizontalScroll();
    await mobile.assertViewportMeta();
  });
});

function futureDate(daysAhead: number): string {
  return new Date(Date.now() + daysAhead * 86_400_000).toISOString();
}
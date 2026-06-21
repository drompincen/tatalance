import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';
import { futureDateTimeLocal, seedClient, uniq } from './fixtures/mobile-seed';

test.describe('M2 — David books a ride from iPhone', () => {
  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
    await mobile.gotoApp();
  });

  test('Book Ride form reachable in two taps from dashboard', async ({ page }) => {
    await expect(page.locator('#tab-dashboard.active')).toBeVisible();
    await mobile.openTab('btn-rides');
    await expect(page.locator('#tab-rides.active')).toBeVisible();
    await expect(page.locator('#ride-submit-btn')).toBeVisible();
  });

  test('client picker has a searchable filter input', async ({ page }) => {
    await mobile.openTab('btn-rides');
    const search = page.locator('#r-clientSearch');
    await expect(search).toBeVisible();
    await expect(search).toHaveAttribute('type', /search|text/);
  });

  test('typing in the search filters visible client options', async ({ page, request }) => {
    const a = `Alice-${uniq()}`;
    const b = `Bob-${uniq()}`;
    await seedClient(request, a);
    await seedClient(request, b);

    await mobile.openTab('btn-rides');
    await expect(page.locator(`#r-clientId option:has-text("${a}")`)).toHaveCount(1);
    await expect(page.locator(`#r-clientId option:has-text("${b}")`)).toHaveCount(1);

    await page.locator('#r-clientSearch').fill(a);
    const aliceVisible = await page.locator(`#r-clientId option:has-text("${a}")`).evaluate(
      (el) => (el as HTMLOptionElement).hidden === false && getComputedStyle(el).display !== 'none',
    );
    const bobVisible = await page.locator(`#r-clientId option:has-text("${b}")`).evaluate(
      (el) => (el as HTMLOptionElement).hidden === false && getComputedStyle(el).display !== 'none',
    );
    expect(aliceVisible).toBe(true);
    expect(bobVisible).toBe(false);
  });

  test('pickup date/time uses datetime-local (iOS native wheel)', async ({ page }) => {
    await mobile.openTab('btn-rides');
    await expect(page.locator('#r-pickupDateTime')).toHaveAttribute('type', 'datetime-local');
  });

  test('Book Ride submit uses sticky positioning on mobile', async () => {
    await mobile.openTab('btn-rides');
    await mobile.assertStickySubmit('#ride-submit-btn');
  });

  test('David can book a ride end-to-end from the iPhone viewport', async ({ page, request }) => {
    const { id: clientId } = await seedClient(request, `David-${uniq()}`);

    await mobile.openTab('btn-rides');
    await expect(page.locator(`#r-clientId option[value="${clientId}"]`)).toHaveCount(1);

    await page.locator('#r-clientId').selectOption(clientId);
    await page.locator('#r-pickupDateTime').fill(futureDateTimeLocal(21));
    await page.locator('#r-pickupLocation').fill('Miami International Airport');
    await page.locator('#r-dropoffLocation').fill('Fontainebleau Hotel');
    await page.locator('#r-basePrice').fill('120');

    await page.locator('#ride-submit-btn').scrollIntoViewIfNeeded();
    await page.locator('#ride-form').evaluate((f: HTMLFormElement) => f.requestSubmit());

    await expect(page.locator('#ride-fb')).toContainText('booked', { timeout: 5000 });
  });
});
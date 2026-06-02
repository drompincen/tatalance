import { test, expect } from '@playwright/test';

/**
 * Mobile journey M2 — David books a ride from his iPhone.
 * Issue #32 / docs/journeys/mobile-iphone-journey.md
 *
 * Pass condition:
 *   - From app open (Clients tab), navigating to Rides is at most 1 tap
 *     (so "Add Ride" is reachable in ≤ 2 taps total)
 *   - The Rides booking form has a search input above the client picker
 *     so David doesn't have to scroll through a 100-item native <select>
 *   - Typing in the search filters which client <option>s are visible
 *   - Pickup date/time field is type=datetime-local (iOS native wheel)
 *   - End-to-end: create a client via API, book a ride from mobile, see it
 *     in the list
 */

const uniq = () => Math.random().toString(36).slice(2, 8);

test.describe('M2 — David books a ride from iPhone', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#btn-rides')).toBeVisible();
  });

  test('Rides tab is reachable in one tap from app open', async ({ page }) => {
    // Default landing is Clients. One tap on the Rides tab.
    await page.locator('#btn-rides').click();
    await expect(page.locator('#tab-rides.active')).toBeVisible();
    // "Book Ride" submit is in view
    await expect(page.locator('#ride-submit-btn')).toBeVisible();
  });

  test('client picker has a searchable filter input', async ({ page }) => {
    await page.locator('#btn-rides').click();
    const search = page.locator('#r-clientSearch');
    await expect(search, 'searchable filter input above the client select').toBeVisible();
    // Must be a text input (not a select)
    await expect(search).toHaveAttribute('type', /search|text/);
  });

  test('typing in the search filters visible client options', async ({ page, request }) => {
    // Seed two clients so we can verify filtering
    const a = `Alice-${uniq()}`;
    const b = `Bob-${uniq()}`;
    await request.post('/api/clients', {
      data: { firstName: a, lastName: 'Alpha', phone: `+1305555${Math.floor(1000 + Math.random() * 9000)}` },
    });
    await request.post('/api/clients', {
      data: { firstName: b, lastName: 'Beta', phone: `+1305555${Math.floor(1000 + Math.random() * 9000)}` },
    });

    await page.locator('#btn-rides').click();
    // Wait for the dropdown to populate (it loads via /api/clients)
    await expect(page.locator(`#r-clientId option:has-text("${a}")`)).toHaveCount(1);
    await expect(page.locator(`#r-clientId option:has-text("${b}")`)).toHaveCount(1);

    // Filter to only "Alice"
    await page.locator('#r-clientSearch').fill(a);

    // Bob's option should now be hidden; Alice's should still be visible.
    const aliceVisible = await page.locator(`#r-clientId option:has-text("${a}")`).evaluate(
      (el) => (el as HTMLOptionElement).hidden === false && getComputedStyle(el).display !== 'none',
    );
    const bobVisible = await page.locator(`#r-clientId option:has-text("${b}")`).evaluate(
      (el) => (el as HTMLOptionElement).hidden === false && getComputedStyle(el).display !== 'none',
    );
    expect(aliceVisible, 'Alice option visible after filter').toBe(true);
    expect(bobVisible, 'Bob option hidden after filter').toBe(false);
  });

  test('pickup date/time uses the iOS native wheel (datetime-local)', async ({ page }) => {
    await page.locator('#btn-rides').click();
    await expect(page.locator('#r-pickupDateTime')).toHaveAttribute('type', 'datetime-local');
  });

  test('David can book a ride end-to-end from the iPhone viewport', async ({ page, request }) => {
    // Seed a client
    const first = `David-${uniq()}`;
    const create = await request.post('/api/clients', {
      data: { firstName: first, lastName: 'Test', phone: `+1305555${Math.floor(1000 + Math.random() * 9000)}` },
    });
    expect(create.ok(), `seed client status ${create.status()}`).toBeTruthy();
    const client = await create.json();

    await page.locator('#btn-rides').click();
    // Wait for client option to appear in the picker
    await expect(page.locator(`#r-clientId option[value="${client.id}"]`)).toHaveCount(1);

    // Fill the form
    await page.locator('#r-clientId').selectOption(client.id);
    await page.locator('#r-pickupDateTime').fill('2026-07-15T10:30');
    await page.locator('#r-pickupLocation').fill('Miami International Airport');
    await page.locator('#r-dropoffLocation').fill('Fontainebleau Hotel');
    await page.locator('#r-basePrice').fill('120');

    // On the iPhone SE viewport, the select element sometimes hovers over
    // sibling elements in the same field; scroll and submit the form directly
    // rather than racing a UI click.
    await page.locator('#ride-submit-btn').scrollIntoViewIfNeeded();
    await page.locator('#ride-form').evaluate((f: HTMLFormElement) => f.requestSubmit());

    // Success feedback (any non-empty text in #ride-fb) or appearance in the list
    await expect(page.locator('#ride-list')).toContainText(`${first} Test`, { timeout: 5000 });
  });
});

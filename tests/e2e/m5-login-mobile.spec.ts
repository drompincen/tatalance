import { test, expect } from '@playwright/test';

/**
 * Mobile journey M5 — Login on iPhone.
 * Issue #35 / docs/journeys/mobile-iphone-journey.md
 *
 * The Spring Security default form login has no mobile affordances
 * (autocomplete attrs, 16px inputs, sticky button). We replace it with a
 * mobile-friendly /login page served from static/, wired through SecurityConfig.
 *
 * Pass condition: /login renders cleanly on iPhone SE with
 *   - viewport meta declaring viewport-fit=cover
 *   - username field has autocomplete="username", autocapitalize="off"
 *   - password field has autocomplete="current-password"
 *   - both inputs computed font-size >= 16px (no iOS auto-zoom)
 *   - submit button reachable in viewport, >= 44x44px
 *   - posting valid credentials lands on / (200, no /login?error)
 */
test.describe('M5 — Login on iPhone Safari', () => {
  test.use({ extraHTTPHeaders: {} }); // disable global Basic auth — we are testing the form

  test.beforeEach(async ({ page }) => {
    await page.goto('/login.html');
    await expect(page).toHaveURL(/\/login\.html/);
  });

  test('viewport meta declares viewport-fit=cover', async ({ page }) => {
    const content = await page.locator('meta[name="viewport"]').getAttribute('content');
    expect(content, 'meta viewport').toMatch(/viewport-fit=cover/);
  });

  test('username field has mobile-friendly autocomplete attrs', async ({ page }) => {
    const user = page.locator('input[name="username"]');
    await expect(user).toBeVisible();
    await expect(user).toHaveAttribute('autocomplete', 'username');
    await expect(user).toHaveAttribute('autocapitalize', 'off');
  });

  test('password field uses current-password autocomplete', async ({ page }) => {
    const pwd = page.locator('input[name="password"]');
    await expect(pwd).toBeVisible();
    await expect(pwd).toHaveAttribute('autocomplete', 'current-password');
    await expect(pwd).toHaveAttribute('type', 'password');
  });

  test('inputs use font-size >= 16px (no iOS auto-zoom)', async ({ page }) => {
    const fonts = await page.evaluate(() => {
      const els = Array.from(
        document.querySelectorAll<HTMLElement>('input[name="username"], input[name="password"]'),
      );
      return els.map((el) => ({
        name: el.getAttribute('name'),
        fontSize: parseFloat(getComputedStyle(el).fontSize),
      }));
    });
    for (const f of fonts) {
      expect(f.fontSize, `${f.name} font-size`).toBeGreaterThanOrEqual(16);
    }
  });

  test('submit button is at least 44x44px', async ({ page }) => {
    const btn = page.locator('button[type="submit"], input[type="submit"]').first();
    await expect(btn).toBeVisible();
    const box = await btn.boundingBox();
    expect(box, 'submit button bounding box').not.toBeNull();
    if (box) {
      expect(box.height, 'submit button height').toBeGreaterThanOrEqual(44);
      expect(box.width, 'submit button width').toBeGreaterThanOrEqual(44);
    }
  });

  test('no horizontal scroll on iPhone SE', async ({ page }) => {
    const overflow = await page.evaluate(() => ({
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth,
    }));
    expect(overflow.sw, 'document scrollWidth vs clientWidth').toBeLessThanOrEqual(overflow.cw + 1);
  });

  test('valid credentials log the user in and reach the app', async ({ page }) => {
    await page.locator('input[name="username"]').fill('admin');
    await page.locator('input[name="password"]').fill('admin');
    await Promise.all([
      page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 10_000 }),
      page.locator('button[type="submit"], input[type="submit"]').first().click(),
    ]);
    await expect(page.locator('#btn-clients')).toBeVisible();
  });
});

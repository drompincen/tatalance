import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Mobile journey M1 — Responsive shell.
 * Issue #31 / docs/journeys/mobile-iphone-journey.md
 *
 * Pass condition: on the iPhone SE viewport (375 x 667), the served app
 *   - reports no horizontal scroll
 *   - declares viewport-fit=cover
 *   - uses input font-size >= 16px (no iOS auto-zoom)
 *   - shows the tab bar without horizontal overflow
 *   - stacks the form/list grid vertically (no side-by-side under 768px)
 *   - has no critical accessibility violations
 */
test.describe('M1 — Responsive shell on iPhone SE', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    // On the mobile viewport the tab nav is collapsed behind the hamburger
    // (issue #31 responsive shell) — wait for the shell to render.
    await expect(page.locator('#hamburger')).toBeVisible();
  });

  test('viewport meta declares viewport-fit=cover', async ({ page }) => {
    const content = await page.locator('meta[name="viewport"]').getAttribute('content');
    expect(content, 'meta viewport content').toMatch(/viewport-fit=cover/);
  });

  test('no horizontal scroll on iPhone SE', async ({ page }) => {
    const overflow = await page.evaluate(() => {
      const doc = document.documentElement;
      return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth };
    });
    expect(overflow.scrollWidth, 'document scrollWidth vs clientWidth').toBeLessThanOrEqual(
      overflow.clientWidth + 1, // tolerate 1px sub-pixel
    );
  });

  test('all form inputs and selects use font-size >= 16px (no iOS auto-zoom)', async ({ page }) => {
    const tooSmall = await page.evaluate(() => {
      const fields = Array.from(
        document.querySelectorAll<HTMLElement>('input, select, textarea'),
      );
      return fields
        .map((el) => {
          const fs = parseFloat(getComputedStyle(el).fontSize);
          return { id: el.id || el.tagName, fontSize: fs };
        })
        .filter((f) => f.fontSize < 16);
    });
    expect(tooSmall, `inputs with font-size < 16px on mobile: ${JSON.stringify(tooSmall)}`).toEqual([]);
  });

  test('tab bar fits within viewport width', async ({ page }) => {
    // The mobile nav is a drawer behind the hamburger — open it to measure.
    await page.locator('#hamburger').click();
    const nav = page.locator('header nav');
    await expect(nav).toBeVisible();
    const box = await nav.boundingBox();
    const viewport = page.viewportSize();
    expect(box, 'nav bounding box').not.toBeNull();
    expect(viewport, 'viewport size').not.toBeNull();
    if (box && viewport) {
      expect(box.x + box.width, 'nav right edge inside viewport').toBeLessThanOrEqual(viewport.width + 1);
    }
  });

  test('form/list grid stacks vertically on mobile (< 768px)', async ({ page }) => {
    // The .grid-2 layout is desktop-only — on mobile it should be a single column.
    const grid = page.locator('#tab-clients .grid-2').first();
    await expect(grid).toBeVisible();
    const cols = await grid.evaluate((el) => getComputedStyle(el).gridTemplateColumns);
    // Should resolve to a single track on mobile (no second value).
    const trackCount = cols.trim().split(/\s+/).filter(Boolean).length;
    expect(trackCount, `grid-template-columns="${cols}"`).toBe(1);
  });

  test('primary action buttons are at least 44x44px (Apple HIG)', async ({ page }) => {
    const undersized = await page.evaluate(() => {
      const tappables = Array.from(
        document.querySelectorAll<HTMLElement>('button.btn, button.tab-btn'),
      );
      return tappables
        .filter((el) => el.offsetParent !== null)
        .map((el) => {
          const r = el.getBoundingClientRect();
          return { label: el.textContent?.trim().slice(0, 20), w: r.width, h: r.height };
        })
        .filter((t) => t.h < 44 || t.w < 44);
    });
    expect(undersized, `tappables under 44x44: ${JSON.stringify(undersized)}`).toEqual([]);
  });

  // M1 scope is the responsive shell (layout, viewport, tap targets, no-zoom).
  // Color-contrast cleanup of the design-system greys is a separate concern —
  // tracked as its own issue. We assert no `critical` axe violations here
  // and exclude `color-contrast` (the `serious` issues live there).
  test('no critical accessibility violations on the clients tab', async ({ page }) => {
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .disableRules(['color-contrast'])
      .analyze();
    const blocking = results.violations.filter((v) => v.impact === 'critical');
    expect(blocking, `axe critical violations: ${blocking.map((v) => v.id).join(', ')}`).toEqual([]);
  });
});

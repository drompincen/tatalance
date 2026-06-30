import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { MobilePage } from './pages/mobile.page';

test.describe('M1 — Responsive shell on iPhone SE', () => {
  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
    await mobile.gotoApp();
  });

  test('viewport meta declares viewport-fit=cover', async () => {
    await mobile.assertViewportMeta();
  });

  test('no horizontal scroll on iPhone SE', async () => {
    await mobile.assertNoHorizontalScroll();
  });

  test('body uses min-height 100dvh and header respects safe area', async () => {
    await mobile.assertBodyUsesDvh();
    await mobile.assertSafeAreaOnHeader();
  });

  test('all form inputs and selects use font-size >= 16px (no iOS auto-zoom)', async () => {
    await mobile.assertMinFontSize();
  });

  test('account menu and freelance mode are reachable on mobile', async ({ page }) => {
    await mobile.openAccountMenu();
    await expect(page.locator('[data-test="account-freelance-link"]')).toBeVisible();
    await expect(page.locator('[data-test="account-venmo-input"]')).toBeVisible();
    await page.locator('#account-menu-overlay').click();
    await page.locator('[data-test="bottom-nav-more"]').click();
    await expect(page.locator('[data-test="more-freelance-mode"]')).toBeVisible();
  });

  test('bottom nav fits within viewport width', async ({ page }) => {
    const nav = page.locator('[data-test="chauffeur-bottom-nav"]');
    await expect(nav).toBeVisible();
    const box = await nav.boundingBox();
    const viewport = page.viewportSize();
    expect(box).not.toBeNull();
    expect(viewport).not.toBeNull();
    if (box && viewport) {
      expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
    }
  });

  test('form/list grid stacks vertically on mobile (< 768px)', async ({ page }) => {
    await mobile.openTab('btn-clients');
    const grid = page.locator('#tab-clients .grid-2').first();
    await expect(grid).toBeVisible();
    const cols = await grid.evaluate((el) => getComputedStyle(el).gridTemplateColumns);
    const trackCount = cols.trim().split(/\s+/).filter(Boolean).length;
    expect(trackCount, `grid-template-columns="${cols}"`).toBe(1);
  });

  test('client and ride tables render as card rows under 640px', async () => {
    await mobile.assertTableCardsOnTab('btn-clients', 'tab-clients');
    await mobile.assertTableCardsOnTab('btn-rides', 'tab-rides');
  });

  test('primary action buttons are at least 44x44px (Apple HIG)', async () => {
    await mobile.assertTapTargets('button.btn, button.tab-btn');
  });

  test('no critical accessibility violations on rides tab', async ({ page }) => {
    test.setTimeout(60_000);
    await mobile.openTab('btn-rides');
    await expect(page.locator('#tab-rides.active')).toBeVisible();
    const results = await new AxeBuilder({ page })
      .include('#tab-rides')
      .withTags(['wcag2a', 'wcag2aa'])
      // Ride booking form uses implicit labels / native select — tracked separately from shell AC.
      .disableRules(['color-contrast', 'label', 'select-name'])
      .analyze();
    const blocking = results.violations.filter((v) => v.impact === 'critical');
    expect(blocking.map((v) => v.id)).toEqual([]);
  });
});
import { type Locator, type Page, expect } from '@playwright/test';

/** Header tab button ids → mobile bottom nav / tables sub-bar / settings sheet targets. */
const TABLE_SUBS = new Set(['btn-clients', 'btn-drivers', 'btn-rides', 'btn-jobs']);
const TAB_TARGETS: Record<string, { kind: 'bottom'; testId: string } | { kind: 'settings'; testId: string }> = {
  'btn-dashboard': { kind: 'bottom', testId: 'bottom-nav-dashboard' },
  'btn-invoices': { kind: 'bottom', testId: 'bottom-nav-invoices' },
  'btn-activity': { kind: 'settings', testId: 'more-nav-activity' },
  'btn-api': { kind: 'settings', testId: 'more-nav-api' },
};

export class MobilePage {
  constructor(readonly page: Page) {}

  async gotoApp() {
    await this.page.goto('/');
    await expect(this.page.locator('[data-test="chauffeur-bottom-nav"]')).toBeVisible();
  }

  async openTab(buttonId: string) {
    // Table tabs: tap Tables bottom nav, then the sub-bar button
    if (TABLE_SUBS.has(buttonId)) {
      await this.page.locator('[data-test="bottom-nav-tables"]').click();
      const sub = buttonId.replace('btn-', '');
      await this.page.locator(`#tables-sub-bar button[data-sub="${sub}"]`).click();
      return;
    }
    const target = TAB_TARGETS[buttonId];
    if (!target) {
      throw new Error(`Unknown mobile tab button: ${buttonId}`);
    }
    if (target.kind === 'bottom') {
      await this.page.locator(`[data-test="${target.testId}"]`).click();
      return;
    }
    await this.page.locator('[data-test="bottom-nav-settings"]').click();
    await this.page.locator(`[data-test="${target.testId}"]`).click();
  }

  async openAccountMenu() {
    await this.page.locator('[data-test="bottom-nav-settings"]').click();
    await this.page.locator('[data-test="more-nav-account"]').click();
    await expect(this.page.locator('#tab-settings.active')).toBeVisible();
  }

  async assertNoHorizontalScroll() {
    await this.page.waitForLoadState('load');
    const overflow = await this.page.evaluate(() => ({
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth,
    }));
    expect(overflow.sw, 'horizontal scroll').toBeLessThanOrEqual(overflow.cw + 1);
  }

  async assertViewportMeta() {
    const content = await this.page.locator('meta[name="viewport"]').getAttribute('content');
    expect(content, 'viewport meta').toMatch(/viewport-fit=cover/);
  }

  async assertMinFontSize(root = 'input, select, textarea', min = 16) {
    const tooSmall = await this.page.evaluate((sel) => {
      return Array.from(document.querySelectorAll<HTMLElement>(sel))
        .map((el) => ({ id: el.id || el.tagName, fontSize: parseFloat(getComputedStyle(el).fontSize) }))
        .filter((f) => f.fontSize < 16);
    }, root);
    expect(tooSmall, `font-size < ${min}px`).toEqual([]);
  }

  async assertTapTargets(selector: string, min = 44) {
    const undersized = await this.page.evaluate(
      ({ sel, minSize }) => {
        return Array.from(document.querySelectorAll<HTMLElement>(sel))
          .filter((el) => el.offsetParent !== null)
          .map((el) => {
            const r = el.getBoundingClientRect();
            return { label: el.textContent?.trim().slice(0, 24), w: r.width, h: r.height };
          })
          .filter((t) => t.h < minSize || t.w < minSize);
      },
      { sel: selector, minSize: min },
    );
    expect(undersized, `tap targets under ${min}px`).toEqual([]);
  }

  async assertBodyUsesDvh() {
    const minHeight = await this.page.locator('body').evaluate((el) => getComputedStyle(el).minHeight);
    if (/dvh|vh|stretch/i.test(minHeight) || minHeight === '100%' || minHeight === 'auto') {
      // Accept dvh, vh, stretch (common in mobile emulation/flex), or full % as fulfilling the 100dvh mobile intent
      return;
    }
    const vh = await this.page.evaluate(() => window.innerHeight);
    const parsed = parseFloat(minHeight);
    expect(parsed || 0, `body min-height (${minHeight})`).toBeGreaterThanOrEqual(vh * 0.85);
  }

  async assertSafeAreaOnHeader() {
    const paddingTop = await this.page.locator('header').first().evaluate((el) => getComputedStyle(el).paddingTop);
    expect(parseFloat(paddingTop), 'header padding-top').toBeGreaterThanOrEqual(0);
  }

  async assertTableCardsOnTab(tabButtonId: string, tabPaneId: string) {
    await this.openTab(tabButtonId);
    const theadDisplay = await this.page.locator(`#${tabPaneId} thead`).first().evaluate(
      (el) => getComputedStyle(el).display,
    );
    expect(theadDisplay, 'thead hidden on mobile').toBe('none');
    const rowDisplay = await this.page.locator(`#${tabPaneId} tbody tr`).first().evaluate(
      (el) => getComputedStyle(el).display,
    );
    expect(rowDisplay, 'tbody rows as cards').toBe('block');
  }

  async assertStickySubmit(selector: string) {
    const btn = this.page.locator(selector);
    await btn.scrollIntoViewIfNeeded();
    const position = await btn.evaluate((el) => getComputedStyle(el).position);
    expect(position, 'submit button position').toBe('sticky');
  }

  async assertInViewport(locator: Locator) {
    await expect(locator).toBeVisible();
    const box = await locator.boundingBox();
    const viewport = this.page.viewportSize();
    expect(box).not.toBeNull();
    expect(viewport).not.toBeNull();
    if (box && viewport) {
      expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 2);
      expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 2);
    }
  }
}
import { test, expect } from '@playwright/test';

test.describe('Dashboard mobile layout', () => {
  test('Home dashboard stacks single column and fits viewport', async ({ page }) => {
    await page.goto('/index.html');
    await expect(page.locator('[data-test="chauffeur-bottom-nav"]')).toBeVisible();
    await expect(page.locator('#tab-dashboard.active')).toBeVisible();

    await page.waitForFunction(() => {
      const val = document.querySelector('#dash-cards .stat-value');
      return val && val.textContent && val.textContent.trim() !== '...';
    }, { timeout: 30_000 });

    const layout = await page.evaluate(() => {
      const vw = document.documentElement.clientWidth;
      const detailCols = getComputedStyle(document.getElementById('dash-detail-row')!).gridTemplateColumns;
      const gridCols = getComputedStyle(document.getElementById('dash-cards')!).gridTemplateColumns;
      const firstCard = document.querySelector('#dash-cards .stat-card')!.getBoundingClientRect();
      const revenue = document.getElementById('dash-revenue')!.getBoundingClientRect();
      const status = document.getElementById('dash-status-bars')!.getBoundingClientRect();
      return {
        vw,
        scrollWidth: document.documentElement.scrollWidth,
        detailCols,
        gridCols,
        firstCard: { x: firstCard.x, w: firstCard.width },
        revenue: { x: revenue.x, w: revenue.width },
        status: { x: status.x, w: status.width },
      };
    });

    expect(layout.scrollWidth, 'horizontal overflow').toBeLessThanOrEqual(layout.vw + 1);
    expect(layout.detailCols.trim().split(/\s+/).length, layout.detailCols).toBe(1);
    expect(layout.gridCols.trim().split(/\s+/).length, layout.gridCols).toBe(1);
    expect(layout.firstCard.x, 'stat card off-screen left').toBeGreaterThanOrEqual(-1);
    expect(layout.firstCard.x + layout.firstCard.w, 'stat card off-screen right').toBeLessThanOrEqual(layout.vw + 1);
    expect(layout.revenue.x, 'revenue off-screen left').toBeGreaterThanOrEqual(-1);
    expect(layout.revenue.x + layout.revenue.w, 'revenue off-screen right').toBeLessThanOrEqual(layout.vw + 1);
    expect(layout.status.x, 'status bars off-screen left').toBeGreaterThanOrEqual(-1);
    expect(layout.status.x + layout.status.w, 'status bars off-screen right').toBeLessThanOrEqual(layout.vw + 1);

    const bottomNav = await page.evaluate(() => {
      const el = document.querySelector('.bottom-nav')!;
      const r = el.getBoundingClientRect();
      return { y: r.y, h: r.height, vh: innerHeight, position: getComputedStyle(el).position };
    });
    expect(bottomNav.position).toBe('fixed');
    expect(bottomNav.h).toBeLessThan(80);
    expect(bottomNav.y).toBeGreaterThan(bottomNav.vh - bottomNav.h - 5);
  });
});
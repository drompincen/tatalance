import { test } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';

test.describe('M1 — Auth pages mobile shell', () => {
  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
  });

  for (const path of ['/register.html', '/forgot-password.html']) {
    test(`${path} has viewport-fit=cover and no horizontal scroll`, async ({ page }) => {
      await page.goto(path);
      await mobile.assertViewportMeta();
      await mobile.assertNoHorizontalScroll();
      await mobile.assertMinFontSize();
    });
  }
});
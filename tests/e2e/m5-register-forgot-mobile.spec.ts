import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';

test.describe('M5 — Register and forgot-password on iPhone', () => {
  test.use({ extraHTTPHeaders: {} });

  let mobile: MobilePage;

  test('register page is mobile-friendly', async ({ page }) => {
    mobile = new MobilePage(page);
    await page.goto('/register.html');
    await mobile.assertViewportMeta();
    await mobile.assertNoHorizontalScroll();
    await mobile.assertMinFontSize();
    const submit = page.locator('button[type="submit"]').first();
    await mobile.assertInViewport(submit);
    await expect(page.locator('#username')).toHaveAttribute('autocomplete', 'username');
  });

  test('forgot-password page is mobile-friendly', async ({ page }) => {
    mobile = new MobilePage(page);
    await page.goto('/forgot-password.html');
    await mobile.assertViewportMeta();
    await mobile.assertNoHorizontalScroll();
    await mobile.assertMinFontSize();
    await expect(page.locator('#username')).toBeVisible();
  });
});
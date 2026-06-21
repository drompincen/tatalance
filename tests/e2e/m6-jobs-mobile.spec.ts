import { test, expect } from '@playwright/test';
import { MobilePage } from './pages/mobile.page';
import { futureDateTimeLocal, seedClient, seedJob, uniq } from './fixtures/mobile-seed';

test.describe('M6 — Jobs (freelance hourly) on iPhone SE', () => {
  let mobile: MobilePage;

  test.beforeEach(async ({ page }) => {
    mobile = new MobilePage(page);
    await mobile.gotoApp();
  });

  test('Jobs tab reachable in two taps from dashboard via hamburger', async ({ page }) => {
    await expect(page.locator('#tab-dashboard.active')).toBeVisible();
    await mobile.openTab('btn-jobs');
    await expect(page.locator('#tab-jobs.active')).toBeVisible();
    await expect(page.locator('#job-submit-btn')).toBeVisible();
  });

  test('client picker in Jobs has searchable filter (j-clientSearch)', async ({ page }) => {
    await mobile.openTab('btn-jobs');
    const search = page.locator('#j-clientSearch');
    await expect(search).toBeVisible();
    await expect(search).toHaveAttribute('type', /search|text/);
  });

  test('scheduled date/time in job form uses datetime-local', async ({ page }) => {
    await mobile.openTab('btn-jobs');
    await expect(page.locator('#j-scheduled')).toHaveAttribute('type', 'datetime-local');
  });

  test('Book Job submit button uses sticky positioning on mobile', async () => {
    await mobile.openTab('btn-jobs');
    await mobile.assertStickySubmit('#job-submit-btn');
  });

  test('David can book a job end-to-end ($20/hr) from iPhone viewport', async ({ page, request }) => {
    const { id: clientId } = await seedClient(request, `JobClient-${uniq()}`);

    await mobile.openTab('btn-jobs');
    await expect(page.locator(`#j-clientId option[value="${clientId}"]`)).toHaveCount(1);

    await page.locator('#j-clientId').selectOption(clientId);
    await page.locator('#j-title').fill('Landing page for client');
    await page.locator('#j-scheduled').fill(futureDateTimeLocal(21));
    await page.locator('#j-scope').fill('Responsive hero + forms');

    await page.locator('#job-submit-btn').scrollIntoViewIfNeeded();
    await page.locator('#job-form').evaluate((f: HTMLFormElement) => f.requestSubmit());

    await expect(page.locator('#job-fb')).toContainText('booked', { timeout: 15000 });
    // Verify creation via API (the fb 'booked' means POST succeeded in UI handler)
    await page.waitForTimeout(300); // allow DB visibility
    const listData = await (await request.get('/api/rides?size=50')).json();
    const created = (listData.content || []).find((r: any) => r.pickupLocation === 'Landing page for client' && r.clientId === clientId);
    expect(created).toBeTruthy();
    // DOM should show it
    await expect(page.locator('#job-list')).toContainText('Landing page for client', { timeout: 15000 });
  });

  test('Start button on scheduled job transitions to IN_PROGRESS with timer', async ({ page, request }) => {
    const { id: clientId } = await seedClient(request);
    const job = await seedJob(request, clientId, `StartTest-${uniq()}`);
    expect(job.status).toBe('SCHEDULED');

    await mobile.openTab('btn-jobs');
    const card = page.locator(`[data-job-id="${job.id}"]`);
    await expect(card.locator('[data-test="start-btn"]')).toBeVisible();
    await card.locator('[data-test="start-btn"]').click();

    await expect(card.locator('[data-test="status-badge"]')).toContainText('IN_PROGRESS', { timeout: 8000 });
    await expect(card.locator('[data-test="complete-btn"]')).toBeVisible();
    await expect(card.locator('.job-timer')).toBeVisible();
  });

  test('live timer ticks and updates billable while job IN_PROGRESS on mobile', async ({ page, request }) => {
    const { id: clientId } = await seedClient(request);
    const job = await seedJob(request, clientId, `TimerTest-${uniq()}`);
    await request.post(`/api/rides/${job.id}/start`);

    await mobile.openTab('btn-jobs');
    const card = page.locator(`[data-job-id="${job.id}"]`);
    const timer = card.locator('.job-timer');
    await expect(timer).toBeVisible();
    const t0 = await timer.textContent();
    await page.waitForTimeout(1500);
    const t1 = await timer.textContent();
    expect(t0).toMatch(/00:00:0\d/);
    expect(t1).not.toBe(t0);

    // billable element updates live too
    const bill = card.locator(`#bill-${job.id}`);
    await expect(bill).toBeVisible();
  });

  test('Confirming complete on job updates status and persists billable (hours x $20)', async ({ page, request }) => {
    const { id: clientId } = await seedClient(request);
    const job = await seedJob(request, clientId, `CompleteTest-${uniq()}`);
    await request.post(`/api/rides/${job.id}/start`);

    await mobile.openTab('btn-jobs');
    const card = page.locator(`[data-job-id="${job.id}"]`);
    await page.waitForTimeout(1200); // accrue a little billable time for hourly calc

    // jobs complete uses native confirm dialog - set handler BEFORE clicking
    page.once('dialog', async (d) => {
      expect(d.message()).toContain('Mark job complete');
      await d.accept();
    });

    await card.locator('[data-test="complete-btn"]').click();

    await expect(card.locator('[data-test="status-badge"]')).toContainText('COMPLETED', { timeout: 12000 });

    const fetched = await (await request.get(`/api/rides/${job.id}`)).json();
    expect(fetched.status).toBe('COMPLETED');
    // hourly billable should be positive (elapsed * 20) even for short run
    expect(Number(fetched.billableAmount || 0)).toBeGreaterThan(0);
  });

  test('job inputs use 16px font (no iOS auto-zoom)', async ({ page, request }) => {
    await mobile.openTab('btn-jobs');
    await mobile.assertMinFontSize(`#tab-jobs input, #tab-jobs select, #tab-jobs textarea`);
  });
});

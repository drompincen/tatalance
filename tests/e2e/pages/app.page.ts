import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Generate a unique E.164 phone number (+1 followed by 10 digits) so tests
 * never collide with the persistent shared test database — the client
 * endpoint enforces per-user phone uniqueness and returns 409 on a repeat.
 * Combines the epoch-ms tail with randomness to stay unique across parallel
 * workers and re-runs. Matches the backend regex ^\+[1-9]\d{9,14}$.
 */
export function uniquePhone(): string {
  const tail = String(Date.now()).slice(-7) + Math.floor(Math.random() * 1000).toString().padStart(3, '0');
  return '+1' + tail;
}

/**
 * A pickup date/time safely in the future. The ride endpoint rejects pickups
 * in the past, so tests must not hardcode calendar dates that eventually
 * expire. Returns a full ISO-8601 instant (for API request bodies).
 */
export function futureDateTime(daysAhead = 7): string {
  return new Date(Date.now() + daysAhead * 86_400_000).toISOString();
}

/**
 * Same future pickup, formatted for an <input type="datetime-local">
 * (YYYY-MM-DDTHH:mm) — used when filling the booking form in the UI.
 */
export function futureDateTimeLocal(daysAhead = 7): string {
  return futureDateTime(daysAhead).slice(0, 16);
}

export class AppPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async goto() {
    await this.page.goto('/');
    await expect(this.page.getByRole('heading', { name: 'Tatalance' })).toBeVisible();
  }

  /** Click a nav tab by name */
  async switchTab(name: string) {
    await this.page.locator(`.tab-btn`, { hasText: name }).click();
  }

  /** Fill a form field by label text */
  async fillField(label: string, value: string) {
    const field = this.page.locator('.field', { hasText: label });
    const input = field.locator('input, select').first();
    const tag = await input.evaluate(el => el.tagName.toLowerCase());
    if (tag === 'select') {
      await input.selectOption({ label: value });
    } else {
      await input.fill(value);
    }
  }

  /** Read all rows from a table as arrays of cell text */
  async readTableRows(containerSelector: string): Promise<string[][]> {
    const rows = this.page.locator(`${containerSelector} table tbody tr`);
    const count = await rows.count();
    const result: string[][] = [];
    for (let i = 0; i < count; i++) {
      const cells = rows.nth(i).locator('td');
      const cellCount = await cells.count();
      const row: string[] = [];
      for (let j = 0; j < cellCount; j++) {
        row.push((await cells.nth(j).innerText()).trim());
      }
      result.push(row);
    }
    return result;
  }

  /** Get the count badge text, e.g. "(3)" */
  async getCount(selector: string): Promise<string> {
    return (await this.page.locator(selector).innerText()).trim();
  }

  /** Click a button by its text */
  async clickButton(text: string) {
    await this.page.locator('button', { hasText: text }).first().click();
  }

  /** Wait for a feedback message */
  async expectFeedback(containerSelector: string, text: string) {
    await expect(this.page.locator(`${containerSelector} .form-feedback`)).toContainText(text);
  }

  /** Delete all items via the API to clean up */
  async apiDelete(path: string) {
    const baseURL = this.page.url().replace(/\/[^/]*$/, '');
    await this.page.request.delete(`${baseURL}${path}`);
  }

  /**
   * GET and return JSON. List endpoints return a Spring `Page` object
   * ({ content: [...], totalElements, ... }); unwrap it to the `content`
   * array so callers can treat the result as a plain list. Non-paginated
   * responses (single objects) pass through unchanged.
   */
  async apiGet(path: string) {
    const resp = await this.page.request.get(path);
    const body = await resp.json();
    return body && Array.isArray(body.content) ? body.content : body;
  }

  async apiPost(path: string, data: object) {
    const resp = await this.page.request.post(path, { data });
    return resp.json();
  }
}

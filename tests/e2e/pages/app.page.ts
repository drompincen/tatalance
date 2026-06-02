import { type Page, type Locator, expect } from '@playwright/test';

export class AppPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async goto() {
    await this.page.goto('/');
    await this.page.waitForLoadState('networkidle');
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

  /** Make an API call and return JSON */
  async apiGet(path: string) {
    const resp = await this.page.request.get(path);
    return resp.json();
  }

  async apiPost(path: string, data: object) {
    const resp = await this.page.request.post(path, { data });
    return resp.json();
  }
}

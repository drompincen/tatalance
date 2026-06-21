import type { APIRequestContext, Page } from '@playwright/test';

/** Jobs tab scopes rides by active profile; E2E uses account-wide scope for stable assertions. */
export async function useAccountWideProfileScope(page: Page) {
  await page.addInitScript(() => localStorage.removeItem('activeProfileId'));
  await page.goto('/');
  const sel = page.locator('#profile-switcher');
  if (await sel.count()) {
    await sel.selectOption('');
  }
}

export const uniq = () => Math.random().toString(36).slice(2, 8);

export function uniquePhone(): string {
  const tail = String(Date.now()).slice(-7) + Math.floor(Math.random() * 1000).toString().padStart(3, '0');
  return '+1' + tail;
}

export function futureDateTime(daysAhead = 14): string {
  return new Date(Date.now() + daysAhead * 86_400_000).toISOString();
}

export function futureDateTimeLocal(daysAhead = 14): string {
  return futureDateTime(daysAhead).slice(0, 16);
}

/** Pickup later today (still same calendar day) — passes no-past-date validation. */
export function laterTodayISO(): string {
  const d = new Date(Date.now() + 60 * 60 * 1000);
  return d.toISOString();
}

function assertOk(ok: boolean, label: string) {
  if (!ok) throw new Error(label);
}

export async function seedClient(request: APIRequestContext, prefix = 'Mob') {
  const firstName = `${prefix}-${uniq()}`;
  const lastName = 'Client';
  const r = await request.post('/api/clients', {
    data: { firstName, lastName, phone: uniquePhone() },
  });
  assertOk(r.ok(), `seed client failed: ${r.status()}`);
  const body = await r.json();
  return {
    id: body.id as string,
    firstName,
    lastName,
    fullName: `${firstName} ${lastName}`,
  };
}

export async function seedRide(
  request: APIRequestContext,
  clientId: string,
  driverId: string,
  pickupISO: string,
  from: string,
  to: string,
  extra: Record<string, unknown> = {},
) {
  const r = await request.post('/api/rides', {
    data: {
      clientId,
      pickupDateTime: pickupISO,
      pickupLocation: from,
      dropoffLocation: to,
      assignedDriverId: driverId,
      ...extra,
    },
  });
  assertOk(r.ok(), `seed ride failed: ${r.status()}`);
  return await r.json();
}

export async function seedAssignedRide(
  request: APIRequestContext,
  clientId: string,
  driverId: string,
  basePrice = 100,
) {
  return seedRide(
    request,
    clientId,
    driverId,
    futureDateTime(30),
    'MIA',
    'Brickell',
    { basePrice },
  );
}

/** Seed a freelance/service job via the unified rides API (used by Jobs tab for #93).
 *  Sets HOURLY $20, title in pickupLocation, dropoff hack for distinction.
 */
export async function seedJob(
  request: APIRequestContext,
  clientId: string,
  title = 'Test Job',
) {
  const r = await request.post('/api/rides', {
    data: {
      clientId,
      pickupDateTime: futureDateTime(1),
      pickupLocation: title,
      dropoffLocation: 'Freelance Job',
      pricingMode: 'HOURLY',
      hourlyRate: 20,
      notes: 'EST:2|Mobile test scope',
    },
  });
  assertOk(r.ok(), `seed job failed: ${r.status()}`);
  return await r.json();
}
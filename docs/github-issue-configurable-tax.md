# Story: Configurable invoice tax rate (account + profile)

## Summary
Let business owners set their invoice tax rate (0–100%) instead of a hardcoded 8%, with sensible defaults for chauffeur vs freelance.

## Use Case
- **As a** business owner invoicing clients through Tatalance
- **I want to** set my default tax rate (and optionally per-profile overrides)
- **so that** new invoices reflect how I actually bill (8% rides, 0% freelance consulting) without manual math

## Acceptance Criteria

### Scenario: Chauffeur sets 8% tax on flat rides
- **Given** I am signed in to chauffeur ops (`index.html`)
- **and Given** my account has no explicit tax rate saved
- **When** I complete a FLAT ride and generate an invoice
- **Then** tax is 8% of subtotal (legacy default)

### Scenario: Freelancer sets 0% tax
- **Given** I open Freelance mode or set `businessMode=FREELANCE`
- **and Given** I save tax rate **0%** in Settings (or it auto-defaults to 0% on first freelance switch)
- **When** I complete an HOURLY job and generate an invoice
- **Then** tax is **$0.00** and total equals subtotal

### Scenario: Custom account tax rate applies to all new invoices
- **Given** I save `defaultTaxRatePercent: 10` via `PATCH /api/users/me/settings`
- **When** I generate an invoice for a completed job/ride
- **Then** tax = subtotal × 10% (rounded to cents)

### Scenario: Profile override wins over account default
- **Given** my account default tax is 8%
- **and Given** my ENGINEER profile has `taxRate: 0`
- **When** I invoice a job scoped to that ENGINEER profile
- **Then** tax is $0.00 regardless of account default

## Implementation notes (shipped on `luciano`)
- `AppUser.defaultTaxRate` + `PATCH /api/users/me/settings` (`defaultTaxRatePercent` 0–100)
- `Profile.taxRate` optional override; ENGINEER profiles default 0%, DRIVER 8%
- `TaxRateResolver` in invoice generation (profile → user → legacy fallbacks)
- UI: tax % in `freelance.html` Settings; chauffeur Account menu

## Test plan
- [ ] `TaxRateResolverTest` + `InvoiceControllerTest` custom rate cases
- [ ] `UserControllerTest` PATCH tax percent validation
- [ ] Manual: chauffeur ride invoice shows configured %; freelance invoice shows 0%

## Labels
`feature` `billing` `freelance`
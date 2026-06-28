# Story: Mobile UX — freelance mode, account menu, ENGINEER setup

## Summary
Make freelance/engineer workflows discoverable on phone: account settings, mode switch, profile creation, and freelance bottom navigation.

## Use Case
- **As a** freelancer (e.g. luchi) using Tatalance on my phone
- **I want to** switch to Freelance mode, create an ENGINEER profile, and manage tax/settings without a desktop
- **so that** I can run the full book → bill → invoice journey from mobile (#115)

## Acceptance Criteria

### Scenario: Chauffeur mobile — open account menu
- **Given** I am on `index.html` at mobile width (≤768px)
- **When** I tap **Account ▾** in the header
- **Then** a bottom sheet opens with profile switcher, tax rate, Freelance mode link, Manage profiles, and Logout

### Scenario: Chauffeur mobile — switch to Freelance mode
- **Given** I am on chauffeur mobile
- **When** I tap ☰ and select the purple **Freelance mode** card at the top of the drawer
- **Then** I land on `/freelance.html` with Jobs/timer/invoicing UI

### Scenario: Jobs tab prompts ENGINEER setup
- **Given** I have no ENGINEER profile
- **When** I open the **Jobs** tab on mobile or desktop
- **Then** I see a banner with **Open Freelance mode** and **Create ENGINEER profile**

### Scenario: Freelance mobile — bottom tab navigation
- **Given** I am on `/freelance.html` at mobile width
- **When** I use the fixed bottom bar (Home, Jobs, Clients, Invoices, Settings)
- **Then** each tab switches views and ENGINEER onboarding remains on Dashboard

### Scenario: Freelance mobile — chauffeur switch in header
- **Given** I am in freelance mode on mobile
- **When** I tap the 🚘 **Switch to Chauffeur ops** control in the top bar
- **Then** I return to `index.html` ride dispatch

## Implementation notes (shipped on `luciano`)
- Restored mobile Account menu (was hidden; only Logout showed)
- Prominent Freelance mode CTA in hamburger nav + account menu
- `jobs-freelance-banner` on Jobs tab
- `freelance.html` bottom nav; header chauffeur link on mobile
- Removed debug "LOCAL WIFI TEST" dashboard banner
- E2E: `m1-responsive-shell` account + freelance reachable test

## Test plan
- [ ] `m1-responsive-shell` — account menu + freelance CTA on iPhone SE
- [ ] `freelance-timer` / `m5-login-mobile` — bottom nav Jobs visible
- [ ] Manual #115 T2–T6 on phone after deploy

## Related
- #115 (Luchi freelancer MVP journey)
- #93 (Epic 11 freelance mode)

## Labels
`feature` `mobile` `freelance` `ux`
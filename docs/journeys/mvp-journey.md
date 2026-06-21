# MVP Journey: Book a Ride, Complete It, Get Paid

The most basic end-to-end journey that makes Tatalance usable as a real tool.
Everything else (VIP management, reports, driver self-service) is additive on top of this core loop.

## The Journey

```
Add Client --> Add Driver --> Create Ride --> Assign Driver --> Complete Ride --> Generate Invoice --> Record Payment
```

David (the owner/operator) performs every step. There is no driver-facing UI in the MVP —
David accepts and completes rides on behalf of his drivers.

---

## Stories

### Story 1: Add Client

**As** David, **I want to** add a new client with their contact details,
**so that** I can book rides for them.

**Scope:**
- Create client form: name (required), phone (required), email, notes
- Client list view showing all clients
- Search/filter clients by name, phone, or email

**Acceptance Criteria:**
- [ ] Submitting with empty name shows validation error
- [ ] Submitting with empty phone shows validation error
- [ ] Valid submit saves client and shows it in the list
- [ ] Client list loads on page visit
- [ ] Search filters the list in real time

**Data Model:**
```
Client { id, name, phone, email, notes, createdAt }
```

**API:**
- `POST /api/clients` — create
- `GET /api/clients` — list all

---

### Story 2: Add Driver

**As** David, **I want to** register a driver with their vehicle and rate info,
**so that** I can assign them to rides.

**Scope:**
- Create driver form: name (required), phone (required), email, vehicle, payout type (percentage/flat), payout rate
- Driver list view with availability status
- Toggle driver availability (available / on trip / off duty)

**Acceptance Criteria:**
- [ ] Submitting with empty name shows validation error
- [ ] Submitting with empty phone shows validation error
- [ ] Valid submit saves driver and shows them in the list
- [ ] Driver list shows availability status with visual indicator
- [ ] Can change a driver's availability from the list

**Data Model:**
```
Driver { id, name, phone, email, vehicle, payoutType, payoutRate, availability, active, createdAt }
```

**API:**
- `POST /api/drivers` — create
- `GET /api/drivers` — list all
- `PATCH /api/drivers/{id}/availability` — update availability status

---

### Story 3: Create Ride

**As** David, **I want to** book a ride for a client with pickup/dropoff details and a price,
**so that** the ride is scheduled and ready for driver assignment.

**Scope:**
- Create ride form: select client (required), pickup date/time (required), pickup location (required), dropoff location (required), price, notes
- Location fields use in-app map picker (Epic 12 #96) — search or drop pin → stored as text; table links open Google Maps (#73)
- Ride list view showing all rides with status
- New rides default to status `SCHEDULED`

**Acceptance Criteria:**
- [ ] Client dropdown is populated from existing clients
- [ ] Submitting without client, pickup time, pickup location, or dropoff location shows validation error
- [ ] Valid submit creates ride with status `SCHEDULED` and shows it in the list
- [ ] Ride list shows client name (denormalized), date, locations, and status badge
- [ ] After adding a client (Story 1), the "Add Ride" CTA pre-selects that client

**Data Model:**
```
Ride {
  id, clientId, clientName (denormalized),
  pickupDateTime, pickupLocation, dropoffLocation,
  basePrice, notes, status (SCHEDULED),
  assignedDriverId, createdAt
}
```

**API:**
- `POST /api/rides` — create (denormalizes clientName on write)
- `GET /api/rides` — list all
- `GET /api/clients/{id}/rides` — list rides for a client

---

### Story 4: Assign Driver to Ride

**As** David, **I want to** assign an available driver to a scheduled ride,
**so that** the driver knows they have an upcoming trip.

**Scope:**
- From the ride detail/list, select an available driver and assign them
- Ride status changes from `SCHEDULED` to `ASSIGNED`
- Driver availability changes to reflect the assignment

**Acceptance Criteria:**
- [ ] Only drivers with availability `available` appear in the assignment dropdown
- [ ] Assigning a driver updates the ride status to `ASSIGNED`
- [ ] Ride list/detail shows the assigned driver name
- [ ] Can reassign to a different driver before the ride starts

**Data Model (updates to Ride):**
```
Ride { ..., assignedDriverId, assignedDriverName (denormalized), assignmentStatus (ASSIGNED), assignedAt }
```

**API:**
- `POST /api/rides/{id}/assign` — body: `{ driverId }` — assigns driver, sets status to ASSIGNED

---

### Story 5: Complete Ride

**As** David, **I want to** mark a ride as completed and capture the actual details (duration, tolls, extras),
**so that** I can generate an accurate invoice.

**Scope:**
- From the ride detail, enter completion details: actual start/end times, waiting time, tolls, parking, additional charges
- System calculates the billable amount: basePrice + tolls + parking + additionalCharges
- Ride status changes to `COMPLETED`

**Acceptance Criteria:**
- [ ] Can only complete a ride that is in `ASSIGNED` status
- [ ] Actual start and end times are required
- [ ] Billable amount is calculated automatically and displayed before confirmation
- [ ] Ride status changes to `COMPLETED` after submission
- [ ] Completed ride shows all captured details (duration, extras, billable amount)

**Data Model (updates to Ride):**
```
Ride {
  ..., status (COMPLETED),
  actualStart, actualEnd, actualDuration,
  waitingTime, tolls, parking,
  additionalCharges, chargeDescription,
  billableAmount
}
```

**API:**
- `POST /api/rides/{id}/complete` — body: `{ actualStart, actualEnd, waitingTime, tolls, parking, additionalCharges, chargeDescription }`

---

### Story 6: Generate Invoice

**As** David, **I want to** generate an invoice from a completed ride,
**so that** I can bill the client.

**Scope:**
- From a completed ride, generate an invoice with base charge + additional charges + tax
- Invoice list view showing all invoices with status
- Invoice statuses: `OUTSTANDING`, `PAID`

**Acceptance Criteria:**
- [ ] Can only generate an invoice for a `COMPLETED` ride
- [ ] Invoice auto-populates: client info, ride details, base charge, additional charges from ride
- [ ] Tax is calculated (8% default — configurable later)
- [ ] Invoice total = base + additional + tax
- [ ] Invoice is created with status `OUTSTANDING`
- [ ] Invoice list shows invoice number, client, total, and status

**Data Model:**
```
Invoice {
  id, invoiceNumber (auto-generated),
  clientId, clientName, rideId,
  baseCharge, additionalCharges, tax, total,
  status (OUTSTANDING | PAID),
  createdAt
}
```

**API:**
- `POST /api/invoices` — body: `{ rideId }` — pulls all amounts from the completed ride
- `GET /api/invoices` — list all

---

### Story 7: Record Payment

**As** David, **I want to** record a payment against an invoice,
**so that** I can track who has paid.

**Scope:**
- From an outstanding invoice, record a payment: date, amount, method (cash/card/transfer/zelle/venmo), reference notes
- If payment covers the full balance, invoice status changes to `PAID`
- Payment history shown on the invoice

**Acceptance Criteria:**
- [ ] Can only record payment on an `OUTSTANDING` invoice
- [ ] Payment amount is required and must be > 0
- [ ] Payment method is required (cash, card, bank transfer, zelle, venmo, check)
- [ ] Full payment updates invoice status to `PAID`
- [ ] Invoice detail shows payment history (date, amount, method)
- [ ] Invoice list reflects updated status

**Data Model:**
```
Payment { date, amount, method, reference }  (embedded in Invoice.payments array)
```

**API:**
- `POST /api/invoices/{id}/payments` — body: `{ date, amount, method, reference }`

---

## Story Map

```
                     SETUP                          OPERATIONS                        BILLING
              -------------------          ---------------------------       -------------------------
              |                 |          |                         |       |                       |
          Story 1           Story 2     Story 3    Story 4    Story 5     Story 6              Story 7
         Add Client       Add Driver   Create Ride  Assign   Complete    Generate              Record
                                                    Driver     Ride      Invoice              Payment
              |                 |          |           |          |          |                    |
              v                 v          v           v          v          v                    v
           [CLIENT]         [DRIVER]    [RIDE]     [RIDE]     [RIDE]    [INVOICE]           [INVOICE]
            created          created    SCHEDULED  ASSIGNED  COMPLETED  OUTSTANDING            PAID
```

## Build Order & Dependencies

```
Story 1 (Add Client)      --+--> Story 3 (Create Ride) --> Story 4 (Assign) --> Story 5 (Complete) --> Story 6 (Invoice) --> Story 7 (Payment)
                            |
Story 2 (Add Driver)    ---+
```

- **Stories 1 and 2** are independent — build in parallel
- **Story 3** depends on both 1 and 2 (needs clients to book for, drivers exist in system)
- **Stories 4, 5, 6, 7** are strictly sequential — each depends on the previous status transition

## Polish Stories (improve the core journey)

These stories don't add new steps to the journey but improve quality and usability:

| # | Story | Why | Issue |
|---|---|---|---|
| 1 | User-friendly validation error messages | Generic "Bad Request" confuses David — needs specific field errors with examples | #25 |
| 2 | Edit and delete Clients | Can't fix typos or remove test data | #26 |
| 3 | Edit and delete Drivers | Can't update vehicle info or deactivate a driver | #27 |
| 4 | Edit and cancel Rides | Can't correct mistakes or handle client cancellations | #28 |
| 5 | Inline ride completion form | Browser popups are clunky — should be an inline panel with context | #29 |

---

## v2 Stories (UX evolution)

These stories evolve Tatalance from a fixed-tab app into a flexible client management tool.

| # | Story | Why | Issue |
|---|---|---|---|
| 1 | Remove payment processing | David manages clients — payment is between David and client directly. Invoices stay for record-keeping but "Pay" button removed. | #39 |
| 2 | Custom tables ("+" button) | David may need to track things we haven't thought of (contacts, expenses, notes). Let him create custom tables with user-defined columns. | #40 |
| 3 | Help page | First-time users need a quick tour of the UI. A "?" button with labeled slide panels. | #41 |

---

## What's NOT in the MVP

These are visible in the prototype but deferred to later iterations:

| Prototype Journey | Why Deferred |
|---|---|
| 5. Driver Accepts Ride | No driver-facing UI in MVP — David manages everything |
| 8. Driver Payout | Valuable but not needed to complete the core book-to-pay loop |
| 10. VIP Clients | Optimization — reuse past ride details, preferred drivers |
| 11. Reports/Performance | Analytics layer — needs data volume to be meaningful |
| Client preferences (vehicle type, language, VIP instructions) | Nice-to-have fields — can be added to the client form later |
| Airport-specific fields (flight number, waiting instructions) | Ride subtype — add when airport rides need special handling |
| i18n (EN/ES toggle) | Already prototyped in mock — port when UI is stable |

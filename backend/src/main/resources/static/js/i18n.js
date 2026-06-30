/**
 * Tatalance i18n — EN/ES translations, data-i18n attributes, localStorage persistence.
 * Default: stored preference, else Spanish when navigator.language starts with "es".
 */
(function () {
  'use strict';

  const STORAGE_KEY = 'tatalance-lang';

  const i18n = {
    en: {
      nav: {
        dashboard: 'Dashboard',
        clients: 'Clients',
        drivers: 'Drivers',
        rides: 'Rides',
        jobs: 'Jobs',
        invoices: 'Invoices',
        activity: 'Activity',
        api: 'API Spec',
        freelance: 'Freelance',
        chauffeurOps: 'Chauffeur ops',
        chauffeurOpsBack: '← Chauffeur ops',
        switchToChauffeur: 'Switch to Chauffeur ops',
        chauffeurOpsHint: 'Rides, drivers & dispatch',
        help: 'Help',
        home: 'Home',
        more: 'More',
      },
      account: {
        label: 'Account',
        profile: 'Profile',
        allProfiles: 'All (account)',
        manageProfiles: 'Manage profiles',
        payment: 'Payment',
        venmoPlaceholder: '@luchi',
        venmoHint: 'Shown on invoice PDFs for clients to pay you.',
        saveVenmo: 'Save Venmo',
        settings: 'Account & settings',
        linkGoogle: 'Link Google account',
        freelanceMode: 'Freelance mode',
        chauffeurMode: 'Chauffeur ops',
        changePassword: 'Change password',
        logout: 'Logout',
        search: 'Search clients, drivers, and rides',
        searchShort: 'Search…',
        openMenu: 'Open menu',
      },
      common: {
        cancel: 'Cancel',
        save: 'Save',
        delete: 'Delete',
        edit: 'Edit',
        close: 'Close',
        create: 'Create',
        actions: 'Actions',
        optional: '(optional)',
        required: 'required',
        saving: 'Saving…',
        select: 'Select',
        selectDots: 'Select…',
        all: 'All',
        to: 'to',
        clear: 'Clear',
        today: 'Today',
        thisWeek: 'This Week',
        thisMonth: 'This Month',
        selectMode: 'Select',
        del: 'Del',
        pay: 'Pay',
        yes: 'Yes',
        no: 'No',
        loading: 'Loading…',
        entries: 'entries',
        prev: '← Prev',
        next: 'Next →',
        showing: 'Showing {from}–{to} of {total}',
        selected: '{count} selected',
        deleteCount: 'Delete {count}',
        autoRefresh: 'Auto-refresh every 10s · last: {time}',
        cannotReachServer: 'Cannot reach server',
        exportCsv: 'Export CSV',
        live: 'LIVE',
        owed: 'OWED',
        paid: 'Paid',
        fixed: 'fixed',
        name: 'Name',
        type: 'Type',
        time: 'Time',
        details: 'Details',
        remove: 'Remove',
        add: 'Add',
        change: 'Change',
        done: 'Done',
        previous: 'Previous',
        nextBtn: 'Next',
        createCustomTable: 'Create custom table',
      },
      filter: {
        all: 'All',
        scheduled: 'Scheduled',
        assigned: 'Assigned',
        inProgress: 'In Progress',
        completed: 'Completed',
        cancelled: 'Cancelled',
        outstanding: 'Outstanding',
        paid: 'Paid',
      },
      status: {
        scheduled: 'Scheduled',
        assigned: 'Assigned',
        accepted: 'Accepted',
        inProgress: 'In Progress',
        completed: 'Completed',
        cancelled: 'Cancelled',
        outstanding: 'Outstanding',
        paid: 'Paid',
      },
      availability: {
        available: 'Available',
        onTrip: 'On Trip',
        offDuty: 'Off Duty',
      },
      pricing: {
        flat: 'Flat rate',
        hourly: 'Hourly',
        flatPlusHourly: 'Flat + Hourly',
        mode: 'Mode',
        base: 'Base',
        hourlyRate: 'Hourly rate',
        duration: 'Duration',
        tolls: 'Tolls',
        extras: 'Extras',
        total: 'Total',
        billable: 'Billable',
        flatSuffix: 'flat',
      },
      dashboard: {
        welcome: 'Welcome to Tatalance',
        subtitle: 'Your dashboard is empty. Follow these steps to get your chauffeur business running.',
        step1: '1. Add a client',
        step2: '2. Add a driver',
        step3: '3. Book a ride',
        clients: 'Clients',
        drivers: 'Drivers',
        activeJobs: 'Active Jobs',
        inProgressJobsTab: 'IN PROGRESS · Jobs tab',
        ridesToday: 'Rides today',
        thisWeek: '{count} this week',
        thisMonth: '{count} this month',
        revenueThisMonth: 'Revenue this month',
        allTime: '{amount} all time',
        outstanding: 'Outstanding',
        invoices: '{count} invoice',
        invoicesPlural: '{count} invoices',
        payoutsOwed: 'Driver Payouts Owed',
        ridesUnpaid: '{count} ride unpaid',
        ridesUnpaidPlural: '{count} rides unpaid',
        ridesByStatus: 'Rides by status',
        revenueSummary: 'Revenue summary',
        loadingStatus: 'Loading status…',
        loadingRevenue: 'Loading revenue…',
        couldNotLoadStats: 'Could not load stats.',
        noRidesYet: 'No rides yet — book one from the Rides tab.',
        bookRide: 'Book a ride',
        paidThisMonth: 'Paid (this month)',
        paidAllTime: 'Paid (all time)',
        outstandingLabel: 'Outstanding',
        driverPayoutsOwed: 'Driver payouts owed',
        totalRides: 'Total rides',
        jobsNote: 'Jobs use $20/hr · see Jobs tab for live timers',
      },
      client: {
        form: {
          addTitle: 'Add Client',
          editTitle: 'Edit Client',
          firstName: 'First Name',
          lastName: 'Last Name',
          phone: 'Phone',
          email: 'Email',
          notes: 'Notes',
          firstNamePlaceholder: 'First name',
          lastNamePlaceholder: 'Last name',
          notesPlaceholder: 'VIP preferences, special requests…',
          phoneHint: 'US number — 10 digits, or 11 starting with 1 (any format: +1…, (111) 222-3333, 111-222-3333)',
          firstNameRequired: 'First name is required',
          lastNameRequired: 'Last name is required',
          phoneE164: 'Enter a valid US phone: 10 digits, or 11 starting with 1',
          addBtn: 'Add Client',
          updateBtn: 'Update Client',
        },
        list: {
          title: 'Clients',
          searchPlaceholder: 'Search by name or phone…',
          select: 'Select',
        },
        table: {
          firstName: 'First Name',
          lastName: 'Last Name',
          phone: 'Phone',
          email: 'Email',
          notes: 'Notes',
          added: 'Added',
        },
        stats: {
          rides: 'Rides',
          completed: 'Completed',
          totalSpent: 'Total Spent',
          vip: 'VIP',
          lastRide: 'Last ride: {date}',
          loadingStats: 'Loading stats…',
        },
        empty: {
          noMatch: 'No clients match your search — try a different name or phone.',
          noClients: 'No clients yet — add your first client to get started.',
          addClient: 'Add client',
        },
      },
      driver: {
        form: {
          addTitle: 'Add Driver',
          editTitle: 'Edit Driver',
          firstName: 'First Name',
          lastName: 'Last Name',
          phone: 'Phone',
          email: 'Email',
          vehicle: 'Vehicle',
          payoutType: 'Payout Type',
          payoutRate: 'Payout Rate',
          payoutPercentage: 'Percentage',
          payoutFlat: 'Flat rate',
          firstNameRequired: 'First name is required',
          lastNameRequired: 'Last name is required',
          phoneE164: 'Enter a valid US phone: 10 digits, or 11 starting with 1',
          payoutRateRequired: 'Payout rate is required',
          addBtn: 'Add Driver',
          updateBtn: 'Update Driver',
        },
        list: {
          title: 'Drivers',
          select: 'Select',
        },
        table: {
          name: 'Name',
          phone: 'Phone',
          vehicle: 'Vehicle',
          payout: 'Payout',
          status: 'Status',
        },
        stats: {
          totalRides: 'Total Rides',
          completed: 'Completed',
          totalEarned: 'Total Earned',
          unpaid: 'Unpaid ({count})',
          loadingStats: 'Loading stats…',
        },
        empty: {
          noDrivers: 'No drivers yet — add your first driver to assign rides.',
          addDriver: 'Add driver',
        },
      },
      ride: {
        form: {
          bookTitle: 'Book a Ride',
          editTitle: 'Edit Ride',
          client: 'Client',
          clientSearch: 'Search by name…',
          selectClient: 'Select a client…',
          pickupDateTime: 'Pickup Date/Time',
          pickupHint: 'Defaults to the next full hour',
          pickupLocation: 'Pickup Location',
          dropoffLocation: 'Dropoff Location',
          pricingMode: 'Pricing Mode',
          basePrice: 'Base Price ($)',
          hourlyRate: 'Hourly Rate ($)',
          notes: 'Notes',
          clientRequired: 'Client is required',
          pickupRequired: 'Pickup date/time is required',
          pickupLocationRequired: 'Pickup location is required',
          dropoffLocationRequired: 'Dropoff location is required',
          bookBtn: 'Book Ride',
          updateBtn: 'Update Ride',
          booking: 'Booking…',
          rebooked: 'Rebooked — pickup defaults to next hour. Adjust if needed and submit.',
        },
        list: {
          title: 'Rides',
        },
        table: {
          client: 'Client',
          pickup: 'Pickup',
          from: 'From',
          to: 'To',
          driver: 'Driver',
          status: 'Status',
          payout: 'Payout',
        },
        filter: {
          clearDates: 'Clear dates',
        },
        clientHint: {
          count: '{count} client — type to filter',
          countPlural: '{count} clients — type to filter',
          noMatch: 'No clients match — try another name',
          oneMatch: '1 match — selected',
          matches: '{count} matches',
        },
        assign: 'Assign…',
        complete: {
          actualStart: 'Actual Start *',
          actualEnd: 'Actual End *',
          required: 'Required',
          hideExtras: '▾ Hide extras',
          moreDetails: '▸ More details (tolls, extras)',
          tolls: 'Tolls ($)',
          additional: 'Additional ($)',
          description: 'Description',
          descriptionPlaceholder: 'Extra stop, waiting, etc.',
          confirmBtn: 'Confirm Complete',
          completing: 'Completing…',
          total: 'Total: ${amount}',
        },
        detail: {
          statusTimeline: 'Status Timeline',
          noHistory: 'No history (created before tracking)',
          pricing: 'Pricing',
          driverPayout: 'Driver Payout',
          notes: 'Notes',
        },
        actions: {
          complete: 'Complete',
          invoice: 'Invoice',
          cancel: 'Cancel',
          rebook: 'Rebook',
        },
        empty: {
          filter: 'No {status} rides — try another filter.',
          noRides: 'No rides yet — book your first ride using the form above.',
          bookRide: 'Book a ride',
        },
      },
      job: {
        form: {
          bookTitle: 'Book a Job',
          client: 'Client',
          clientSearch: 'Search by name…',
          selectClient: 'Select a client…',
          title: 'Job Title',
          titlePlaceholder: 'Landing page development',
          scope: 'Scope / Description',
          scopePlaceholder: 'Build responsive landing page...',
          hourlyRate: 'Hourly Rate',
          rateFixed: '$/hr — fixed',
          estHours: 'Est. Hours',
          scheduled: 'Scheduled Date/Time',
          clientRequired: 'Client is required',
          titleRequired: 'Job title is required',
          scheduledRequired: 'Scheduled date/time is required',
          bookBtn: 'Book Job',
          updateBtn: 'Update Job',
          booking: 'Booking…',
        },
        list: {
          title: 'Jobs',
        },
        card: {
          untitled: 'Untitled job',
          logged: 'LOGGED',
          status: 'STATUS',
          live: '{hours}h live',
          loggedEst: '{logged}h / {est}h',
        },
        actions: {
          start: 'Start',
          complete: 'Complete',
          invoice: 'Invoice',
        },
        empty: {
          filter: 'No {status} jobs — try another filter.',
          noJobs: 'No jobs yet — book your first hourly job using the form above.',
          bookJob: 'Book a job',
        },
      },
      invoice: {
        list: {
          title: 'Invoices',
        },
        table: {
          number: 'Invoice #',
          client: 'Client',
          charge: 'Charge',
          extras: 'Extras',
          tax: 'Tax',
          total: 'Total',
          status: 'Status',
        },
        actions: {
          markPaid: 'Mark Paid',
          markUnpaid: 'Mark Unpaid',
          pdf: 'PDF',
        },
        empty: {
          filter: 'No {status} invoices — try another filter.',
          noInvoices: 'No invoices yet — complete a ride, then generate an invoice from the Rides tab.',
          goToRides: 'Go to rides',
        },
      },
      activity: {
        title: 'Activity Log',
        table: {
          time: 'Time',
          action: 'Action',
          type: 'Type',
          details: 'Details',
        },
        empty: 'No activity yet. Actions will appear here as you use the app.',
        couldNotLoad: 'Could not load activity log.',
        entries: '{count} entries',
      },
      modal: {
        changePassword: {
          title: 'Change Password',
          current: 'Current Password',
          new: 'New Password',
          confirm: 'Confirm New Password',
          changeBtn: 'Change',
        },
        profileManager: {
          title: 'Manage Profiles',
          type: 'Type',
          typeDriver: 'DRIVER (shows Rides)',
          typeEngineer: 'ENGINEER / Hourly',
          typeHandyman: 'HANDYMAN / Hourly',
          typeOther: 'OTHER / Hourly',
          nameOptional: 'Name (optional display)',
          namePlaceholder: 'e.g. Main Taxi or Freelance Dev',
          createNew: 'Create New',
          saveChanges: 'Save Changes',
          loading: 'Loading…',
          noProfiles: 'No profiles yet. Create one below.',
          edit: 'edit',
          saved: 'Saved.',
          created: 'Profile created.',
        },
        createTable: {
          title: 'Create Custom Table',
          tableName: 'Table Name',
          namePlaceholder: 'e.g. Vehicles, Expenses…',
          nameRequired: 'Table name is required',
          columns: 'Columns',
          columnName: 'Column name',
          colTypeText: 'Text',
          colTypeNumber: 'Number',
          colTypeBool: 'Yes/No',
          colTypeDate: 'Date',
          colTypeLink: 'Link',
          trueLabel: 'True label (e.g. Paid)',
          falseLabel: 'False label (e.g. Unpaid)',
          selectTableLink: 'Select table to link…',
          addColumn: '+ Add column',
          colsRequired: 'At least one column is required',
          createBtn: 'Create Table',
        },
      },
      customTable: {
        addRow: 'Add Row',
        editRow: 'Edit Row',
        updateRow: 'Update Row',
        addColumn: 'Add Column',
        editColumn: 'Edit Column',
        linkToTable: 'Link to table',
        selectTable: 'Select table…',
        trueLabel: 'True label',
        falseLabel: 'False label',
        deleteColumn: 'Delete Column',
        noRows: 'No rows yet',
        addColumnTitle: 'Add column',
      },
      search: {
        noResults: 'No results',
        clients: 'Clients',
        rides: 'Rides',
        invoices: 'Invoices',
      },
      messages: {
        personAdded: '✓ {name} added',
        personUpdated: '✓ {name} updated',
        rideBooked: '✓ Ride booked',
        rideUpdated: '✓ Ride updated',
        jobBooked: '✓ Job booked ($20/hr)',
        jobUpdated: '✓ Job updated',
        jobCompleted: '✓ Job completed. Total hours × rate calculated.',
        rowAdded: '✓ Row added',
        rowUpdated: '✓ Row updated',
        saveFailed: 'Could not save your changes — check the form and try again.',
        saveFailedShort: 'Could not save — try again.',
        requestFailed: 'Something went wrong — please try again.',
        networkError: 'Could not reach the server — check your connection and try again.',
        notFoundGeneric: 'That item was not found — refresh the page and try again.',
        conflictGeneric: 'This action is not allowed right now — refresh and check the status.',
        assignFailed: 'Could not assign the driver — they may no longer be available.',
        scheduledPast: 'Scheduled date/time must be in the future — pick a later time.',
        clientNotFound: 'Client not found — refresh the list and try again.',
        profileNotFound: 'Profile not found — switch to "All (account)" in the menu and try again.',
        rideNotFound: 'Ride or job not found.',
        driverNotFound: 'Driver not found.',
        duplicatePhone: 'A client with this phone number already exists.',
        invalidRequest: 'Invalid request — check your entries and try again.',
        scheduledRequired: 'Scheduled date/time is required.',
        onlyScheduledEdit: 'Only scheduled jobs can be edited.',
        sessionExpired: 'Session expired — refresh the page and log in again.',
        serverError: 'Server error — try again in a moment.',
        httpError: 'The server rejected this request (error {status}). Refresh the page and try again.',
        deleteConfirm: 'Delete {name}?',
        deleteClients: 'Delete {count} client(s)?',
        deleteDrivers: 'Delete {count} driver(s)?',
        deleteRows: 'Delete {count} row(s)?',
        deleteRow: 'Delete this row?',
        deleteColumn: 'Delete column "{name}"?',
        deleteTable: 'Delete table "{name}" and all its rows?',
        cannotDeleteClient: 'Cannot delete — client has active rides',
        cannotDeleteDriver: 'Cannot delete — driver has active rides',
        deleteFailed: 'Could not delete — refresh the page and try again.',
        someDeletesFailed: 'Some deletes failed:\n{errors}',
        cancelRide: 'Cancel this ride?',
        cannotCancelRide: 'Cannot cancel this ride',
        cancelFailed: 'Could not cancel this ride — it may already be finished.',
        markPayoutPaid: 'Mark this driver payout as paid?',
        markPayoutFailed: 'Could not mark payout as paid — only completed rides qualify.',
        completeFailed: 'Could not complete this ride — check the times and try again.',
        completeTimesRequired: 'Enter both actual start and end times before completing.',
        completeEndAfterStart: 'End time must be after start time.',
        completeBillableHours: 'Enter billable hours before completing this scheduled job.',
        completeWrongStatus: 'This ride cannot be completed yet — assign a driver and enter start/end times, or start the timer first.',
        completeStartTimerFirst: 'Start the ride timer before completing, or enter actual start and end times.',
        cannotStartRide: 'This ride cannot be started — check its status and try again.',
        cannotCancelRideStatus: 'This ride cannot be cancelled because it is already finished or cancelled.',
        driverNotAvailable: 'That driver is not available — choose another driver.',
        driverIdRequired: 'Select a driver before assigning.',
        invoiceRideNotCompleted: 'The ride must be completed before you can create an invoice.',
        payoutOnlyCompleted: 'Payout can only be marked on completed rides.',
        rideIdRequired: 'Ride is required.',
        invoiceNotFound: 'Invoice not found — refresh the list and try again.',
        tableNotFound: 'Table not found — refresh and try again.',
        availabilityRequired: 'Select a driver availability status.',
        payoutTypeInvalid: 'Payout type must be Percentage or Flat rate.',
        availabilityInvalid: 'Availability must be Available, On trip, or Off duty.',
        columnNameExists: 'A column with that name already exists.',
        linkedTableNotFound: 'The linked table was not found.',
        completeJobConfirm: 'Mark job complete now? (uses live elapsed since start × $20)',
        startJobFailed: 'Could not start the job — make sure it is still scheduled.',
        invoiceFailed: 'Could not create invoice: {message}',
        failed: 'Something went wrong: {message}',
        failedCreateTable: 'Could not create the table — check the name and columns.',
        columnNameRequired: 'Column name is required',
        selectLinkTable: 'Please select a table to link to',
        typeRequired: 'Type required',
        failedLoadProfiles: 'Could not load profiles — refresh the page.',
        failedSave: 'Could not save — try again.',
        failedCreate: 'Could not create — check your entries.',
        currentPasswordRequired: 'Current password is required',
        passwordMinLength: 'New password must be at least 4 characters',
        passwordsNoMatch: 'Passwords do not match',
        passwordChanged: 'Password changed successfully',
        changePasswordFailed: 'Could not change password — check your current password.',
        linkGoogleFailed: 'Could not start Google linking — try again in a moment.',
      },
      help: {
        welcome: {
          title: 'Welcome to Tatalance',
          content: '<h3>Your chauffeur client management tool</h3>'
            + '<p>Tatalance helps you manage clients, drivers, rides, invoices, and any custom data you need — all in one place.</p>'
            + '<div class="help-tip"><span class="tip-icon">📋</span><div class="tip-text"><strong>Tabs at the top</strong> — switch between Clients, Drivers, Rides, Invoices, and any custom tables you create.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">➕</span><div class="tip-text"><strong>The + button</strong> — create your own custom tables for anything (vehicles, expenses, notes, etc).</div></div>'
            + '<div class="help-tip"><span class="tip-icon">❓</span><div class="tip-text"><strong>The ? button</strong> — you\'re here! Come back anytime for a refresher.</div></div>',
        },
        clients: {
          title: 'Clients',
          content: '<h3>Add and manage your clients</h3>'
            + '<div class="help-tip"><span class="tip-icon">👤</span><div class="tip-text"><strong>Add a client</strong> — fill in first name, last name, and phone (international format: +1 then digits). Email is optional.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">✏️</span><div class="tip-text"><strong>Edit</strong> — click the Edit button on any row to update their info. The form switches to edit mode.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🗑️</span><div class="tip-text"><strong>Delete</strong> — click Del to remove a client. You can\'t delete a client who has active rides.</div></div>'
            + '<p>The list auto-refreshes every 10 seconds so you always see the latest data.</p>',
        },
        drivers: {
          title: 'Drivers',
          content: '<h3>Manage your driver roster</h3>'
            + '<div class="help-tip"><span class="tip-icon">🚗</span><div class="tip-text"><strong>Add a driver</strong> — name, phone, optional vehicle, and payout info (percentage or flat rate).</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🟢</span><div class="tip-text"><strong>Availability</strong> — change a driver\'s status using the dropdown: Available, On Trip, or Off Duty.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">💰</span><div class="tip-text"><strong>Payout</strong> — set to Percentage (e.g. 70%) or Flat (e.g. $50 per ride). This shows on invoices.</div></div>',
        },
        rides: {
          title: 'Rides',
          content: '<h3>Book and manage rides</h3>'
            + '<div class="help-tip"><span class="tip-icon">📅</span><div class="tip-text"><strong>Book a ride</strong> — pick a client, set pickup date/time, locations, and optional base price.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🔗</span><div class="tip-text"><strong>Assign a driver</strong> — once booked, use the dropdown in the Driver column to assign an available driver.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">✅</span><div class="tip-text"><strong>Complete</strong> — after assignment, click Complete. Fill in actual start/end times, tolls, and extras.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">❌</span><div class="tip-text"><strong>Cancel</strong> — cancel a ride at any time before completion. The driver becomes available again.</div></div>',
        },
        invoices: {
          title: 'Invoices',
          content: '<h3>Generate and track invoices</h3>'
            + '<div class="help-tip"><span class="tip-icon">📄</span><div class="tip-text"><strong>Generate</strong> — after completing a ride, click the Invoice button on that ride. It calculates base + extras + tax.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">💵</span><div class="tip-text"><strong>Mark Paid / Unpaid</strong> — toggle an invoice\'s status with one click. No complex payment processing needed.</div></div>'
            + '<p>Invoices show the breakdown: base charge, additional charges, tax, and total.</p>',
        },
        customTables: {
          title: 'Custom Tables',
          content: '<h3>Create your own data tables</h3>'
            + '<div class="help-tip"><span class="tip-icon">➕</span><div class="tip-text"><strong>Create a table</strong> — click the + button in the nav bar. Name your table and add columns with types: Text, Number, Yes/No, or Date.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">📋</span><div class="tip-text"><strong>Use it</strong> — your table appears as a new tab. Add rows, edit them, delete them — just like Clients or Drivers.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🗑️</span><div class="tip-text"><strong>Delete a table</strong> — hover over the tab name and click the x. This removes the table and all its rows.</div></div>'
            + '<p>Ideas: Vehicles, Expenses, Fuel Logs, Client Notes, anything you need to track.</p>',
        },
      },
      auth: {
        login: {
          pageTitle: 'Sign in — Tatalance',
          title: 'Sign in',
          subtitle: 'Tatalance — chauffeur platform demo',
          username: 'Username',
          password: 'Password',
          submit: 'Sign in',
          submitting: 'Signing in…',
          forgotPassword: 'Forgot password?',
          googleSignIn: 'Sign in with Google',
          or: 'or',
          noAccount: "Don't have an account?",
          createOne: 'Create one',
          wrongCredentials: 'Wrong username or password.',
          signedOut: 'You have been signed out.',
          serverError: 'Could not reach server. Try again.',
        },
        register: {
          pageTitle: 'Create Account — Tatalance',
          title: 'Create Account',
          subtitle: 'Tatalance — chauffeur platform',
          username: 'Username',
          password: 'Password',
          confirmPassword: 'Confirm Password',
          securityQuestion: 'Security Question',
          securityOptional: '(optional — for password recovery)',
          yourAnswer: 'Your Answer',
          submit: 'Create Account',
          hasAccount: 'Already have an account?',
          signIn: 'Sign in',
          usernameRequired: 'Username is required',
          passwordMin: 'Password must be at least 4 characters',
          passwordsNoMatch: 'Passwords do not match',
          answerRequired: 'Please provide an answer to the security question',
          registrationFailed: 'Could not create your account — check your username and password.',
          accountCreated: 'Account created!',
          serverError: 'Could not reach server',
        },
        forgot: {
          pageTitle: 'Reset Password — Tatalance',
          title: 'Reset Password',
          subtitle: 'Answer your security question to reset',
          username: 'Username',
          yourAnswer: 'Your Answer',
          newPassword: 'New Password',
          confirmPassword: 'Confirm New Password',
          next: 'Next',
          submit: 'Reset Password',
          backToSignIn: 'Back to sign in',
          usernameRequired: 'Username is required',
          answerRequired: 'Answer is required',
          passwordMin: 'Password must be at least 4 characters',
          passwordsNoMatch: 'Passwords do not match',
          accountNotFound: 'Account not found',
          resetFailed: 'Could not reset your password — try again or request a new reset link.',
          serverError: 'Could not reach server',
        },
        securityQuestions: {
          none: '— none —',
          pet: "What is your pet's name?",
          city: 'What city were you born in?',
          food: 'What is your favorite food?',
          car: 'What was your first car?',
          maiden: "What is your mother's maiden name?",
        },
      },
      lang: {
        switch: 'Switch language / Cambiar idioma',
      },
    },
    es: {
      nav: {
        dashboard: 'Panel',
        clients: 'Clientes',
        drivers: 'Choferes',
        rides: 'Viajes',
        jobs: 'Trabajos',
        invoices: 'Facturas',
        activity: 'Actividad',
        api: 'API',
        freelance: 'Freelance',
        chauffeurOps: 'Operaciones chauffeur',
        chauffeurOpsBack: '← Operaciones chauffeur',
        switchToChauffeur: 'Ir a operaciones chauffeur',
        chauffeurOpsHint: 'Viajes, choferes y despacho',
        help: 'Ayuda',
        home: 'Inicio',
        more: 'Más',
      },
      account: {
        label: 'Cuenta',
        profile: 'Perfil',
        allProfiles: 'Todos (cuenta)',
        manageProfiles: 'Gestionar perfiles',
        payment: 'Pago',
        venmoPlaceholder: '@luchi',
        venmoHint: 'Aparece en los PDF de facturas para que te paguen.',
        saveVenmo: 'Guardar Venmo',
        settings: 'Cuenta y ajustes',
        linkGoogle: 'Vincular cuenta de Google',
        freelanceMode: 'Modo freelance',
        chauffeurMode: 'Operaciones chauffeur',
        changePassword: 'Cambiar contraseña',
        logout: 'Cerrar sesión',
        search: 'Buscar clientes, choferes y viajes',
        searchShort: 'Buscar…',
        openMenu: 'Abrir menú',
      },
      common: {
        cancel: 'Cancelar',
        save: 'Guardar',
        delete: 'Eliminar',
        edit: 'Editar',
        close: 'Cerrar',
        create: 'Crear',
        actions: 'Acciones',
        optional: '(opcional)',
        required: 'obligatorio',
        saving: 'Guardando…',
        select: 'Seleccionar',
        selectDots: 'Seleccionar…',
        all: 'Todos',
        to: 'a',
        clear: 'Limpiar',
        today: 'Hoy',
        thisWeek: 'Esta semana',
        thisMonth: 'Este mes',
        selectMode: 'Seleccionar',
        del: 'Elim.',
        pay: 'Pagar',
        yes: 'Sí',
        no: 'No',
        loading: 'Cargando…',
        entries: 'registros',
        prev: '← Ant.',
        next: 'Sig. →',
        showing: 'Mostrando {from}–{to} de {total}',
        selected: '{count} seleccionados',
        deleteCount: 'Eliminar {count}',
        autoRefresh: 'Actualización cada 10 s · última: {time}',
        cannotReachServer: 'No se puede conectar al servidor',
        exportCsv: 'Exportar CSV',
        live: 'EN VIVO',
        owed: 'PENDIENTE',
        paid: 'Pagado',
        fixed: 'fijo',
        name: 'Nombre',
        type: 'Tipo',
        time: 'Hora',
        details: 'Detalles',
        remove: 'Quitar',
        add: 'Agregar',
        change: 'Cambiar',
        done: 'Listo',
        previous: 'Anterior',
        nextBtn: 'Siguiente',
        createCustomTable: 'Crear tabla personalizada',
      },
      filter: {
        all: 'Todos',
        scheduled: 'Programado',
        assigned: 'Asignado',
        inProgress: 'En progreso',
        completed: 'Completado',
        cancelled: 'Cancelado',
        outstanding: 'Pendiente',
        paid: 'Pagada',
      },
      status: {
        scheduled: 'Programado',
        assigned: 'Asignado',
        accepted: 'Aceptado',
        inProgress: 'En progreso',
        completed: 'Completado',
        cancelled: 'Cancelado',
        outstanding: 'Pendiente',
        paid: 'Pagada',
      },
      availability: {
        available: 'Disponible',
        onTrip: 'En viaje',
        offDuty: 'Fuera de servicio',
      },
      pricing: {
        flat: 'Tarifa fija',
        hourly: 'Por hora',
        flatPlusHourly: 'Fija + por hora',
        mode: 'Modo',
        base: 'Base',
        hourlyRate: 'Tarifa por hora',
        duration: 'Duración',
        tolls: 'Peajes',
        extras: 'Extras',
        total: 'Total',
        billable: 'Facturable',
        flatSuffix: 'fija',
      },
      dashboard: {
        welcome: 'Bienvenido a Tatalance',
        subtitle: 'Su panel está vacío. Siga estos pasos para poner en marcha su negocio de chofer.',
        step1: '1. Agregar un cliente',
        step2: '2. Agregar un chofer',
        step3: '3. Reservar un viaje',
        clients: 'Clientes',
        drivers: 'Choferes',
        activeJobs: 'Trabajos activos',
        inProgressJobsTab: 'EN PROGRESO · pestaña Trabajos',
        ridesToday: 'Viajes hoy',
        thisWeek: '{count} esta semana',
        thisMonth: '{count} este mes',
        revenueThisMonth: 'Ingresos este mes',
        allTime: '{amount} histórico',
        outstanding: 'Pendiente',
        invoices: '{count} factura',
        invoicesPlural: '{count} facturas',
        payoutsOwed: 'Pagos a choferes pendientes',
        ridesUnpaid: '{count} viaje sin pagar',
        ridesUnpaidPlural: '{count} viajes sin pagar',
        ridesByStatus: 'Viajes por estado',
        revenueSummary: 'Resumen de ingresos',
        loadingStatus: 'Cargando estados…',
        loadingRevenue: 'Cargando ingresos…',
        couldNotLoadStats: 'No se pudieron cargar las estadísticas.',
        noRidesYet: 'Aún no hay viajes — reserve uno en la pestaña Viajes.',
        bookRide: 'Reservar viaje',
        paidThisMonth: 'Pagado (este mes)',
        paidAllTime: 'Pagado (histórico)',
        outstandingLabel: 'Pendiente',
        driverPayoutsOwed: 'Pagos a choferes pendientes',
        totalRides: 'Total de viajes',
        jobsNote: 'Trabajos a $20/hr · ver pestaña Trabajos para temporizadores en vivo',
      },
      client: {
        form: {
          addTitle: 'Agregar cliente',
          editTitle: 'Editar cliente',
          firstName: 'Nombre',
          lastName: 'Apellido',
          phone: 'Teléfono',
          email: 'Correo electrónico',
          notes: 'Notas',
          firstNamePlaceholder: 'Nombre',
          lastNamePlaceholder: 'Apellido',
          notesPlaceholder: 'Preferencias VIP, solicitudes especiales…',
          phoneHint: 'Número EE. UU. — 10 dígitos, o 11 empezando con 1 (cualquier formato)',
          firstNameRequired: 'El nombre es obligatorio',
          lastNameRequired: 'El apellido es obligatorio',
          phoneE164: 'Ingrese un teléfono válido: 10 dígitos, o 11 empezando con 1',
          addBtn: 'Agregar cliente',
          updateBtn: 'Actualizar cliente',
        },
        list: {
          title: 'Clientes',
          searchPlaceholder: 'Buscar por nombre o teléfono…',
          select: 'Seleccionar',
        },
        table: {
          firstName: 'Nombre',
          lastName: 'Apellido',
          phone: 'Teléfono',
          email: 'Correo',
          notes: 'Notas',
          added: 'Agregado',
        },
        stats: {
          rides: 'Viajes',
          completed: 'Completados',
          totalSpent: 'Total gastado',
          vip: 'VIP',
          lastRide: 'Último viaje: {date}',
          loadingStats: 'Cargando estadísticas…',
        },
        empty: {
          noMatch: 'Ningún cliente coincide — pruebe otro nombre o teléfono.',
          noClients: 'Aún no hay clientes — agregue su primer cliente para comenzar.',
          addClient: 'Agregar cliente',
        },
      },
      driver: {
        form: {
          addTitle: 'Agregar chofer',
          editTitle: 'Editar chofer',
          firstName: 'Nombre',
          lastName: 'Apellido',
          phone: 'Teléfono',
          email: 'Correo electrónico',
          vehicle: 'Vehículo',
          payoutType: 'Tipo de pago',
          payoutRate: 'Tasa de pago',
          payoutPercentage: 'Porcentaje',
          payoutFlat: 'Tarifa fija',
          firstNameRequired: 'El nombre es obligatorio',
          lastNameRequired: 'El apellido es obligatorio',
          phoneE164: 'Ingrese un teléfono válido: 10 dígitos, o 11 empezando con 1',
          payoutRateRequired: 'La tasa de pago es obligatoria',
          addBtn: 'Agregar chofer',
          updateBtn: 'Actualizar chofer',
        },
        list: {
          title: 'Choferes',
          select: 'Seleccionar',
        },
        table: {
          name: 'Nombre',
          phone: 'Teléfono',
          vehicle: 'Vehículo',
          payout: 'Pago',
          status: 'Estado',
        },
        stats: {
          totalRides: 'Total de viajes',
          completed: 'Completados',
          totalEarned: 'Total ganado',
          unpaid: 'Sin pagar ({count})',
          loadingStats: 'Cargando estadísticas…',
        },
        empty: {
          noDrivers: 'Aún no hay choferes — agregue su primer chofer para asignar viajes.',
          addDriver: 'Agregar chofer',
        },
      },
      ride: {
        form: {
          bookTitle: 'Reservar viaje',
          editTitle: 'Editar viaje',
          client: 'Cliente',
          clientSearch: 'Buscar por nombre…',
          selectClient: 'Seleccionar un cliente…',
          pickupDateTime: 'Fecha/hora de recogida',
          pickupHint: 'Por defecto, la próxima hora en punto',
          pickupLocation: 'Lugar de recogida',
          dropoffLocation: 'Destino',
          pricingMode: 'Modo de precio',
          basePrice: 'Precio base ($)',
          hourlyRate: 'Tarifa por hora ($)',
          notes: 'Notas',
          clientRequired: 'El cliente es obligatorio',
          pickupRequired: 'La fecha/hora de recogida es obligatoria',
          pickupLocationRequired: 'El lugar de recogida es obligatorio',
          dropoffLocationRequired: 'El destino es obligatorio',
          bookBtn: 'Reservar viaje',
          updateBtn: 'Actualizar viaje',
          booking: 'Reservando…',
          rebooked: 'Re-reservado — la recogida usa la próxima hora. Ajuste si hace falta y envíe.',
        },
        list: {
          title: 'Viajes',
        },
        table: {
          client: 'Cliente',
          pickup: 'Recogida',
          from: 'Desde',
          to: 'Hasta',
          driver: 'Chofer',
          status: 'Estado',
          payout: 'Pago',
        },
        filter: {
          clearDates: 'Limpiar fechas',
        },
        clientHint: {
          count: '{count} cliente — escriba para filtrar',
          countPlural: '{count} clientes — escriba para filtrar',
          noMatch: 'Ningún cliente coincide — pruebe otro nombre',
          oneMatch: '1 coincidencia — seleccionado',
          matches: '{count} coincidencias',
        },
        assign: 'Asignar…',
        complete: {
          actualStart: 'Inicio real *',
          actualEnd: 'Fin real *',
          required: 'Obligatorio',
          hideExtras: '▾ Ocultar extras',
          moreDetails: '▸ Más detalles (peajes, extras)',
          tolls: 'Peajes ($)',
          additional: 'Adicional ($)',
          description: 'Descripción',
          descriptionPlaceholder: 'Parada extra, espera, etc.',
          confirmBtn: 'Confirmar completado',
          completing: 'Completando…',
          total: 'Total: ${amount}',
        },
        detail: {
          statusTimeline: 'Línea de tiempo',
          noHistory: 'Sin historial (creado antes del seguimiento)',
          pricing: 'Precios',
          driverPayout: 'Pago al chofer',
          notes: 'Notas',
        },
        actions: {
          complete: 'Completar',
          invoice: 'Factura',
          cancel: 'Cancelar',
          rebook: 'Re-reservar',
        },
        empty: {
          filter: 'No hay viajes {status} — pruebe otro filtro.',
          noRides: 'Aún no hay viajes — reserve su primer viaje con el formulario.',
          bookRide: 'Reservar viaje',
        },
      },
      job: {
        form: {
          bookTitle: 'Reservar trabajo',
          client: 'Cliente',
          clientSearch: 'Buscar por nombre…',
          selectClient: 'Seleccionar un cliente…',
          title: 'Título del trabajo',
          titlePlaceholder: 'Desarrollo de landing page',
          scope: 'Alcance / descripción',
          scopePlaceholder: 'Construir landing page responsive...',
          hourlyRate: 'Tarifa por hora',
          rateFixed: '$/hr — fijo',
          estHours: 'Horas est.',
          scheduled: 'Fecha/hora programada',
          clientRequired: 'El cliente es obligatorio',
          titleRequired: 'El título es obligatorio',
          scheduledRequired: 'La fecha/hora programada es obligatoria',
          bookBtn: 'Reservar trabajo',
          updateBtn: 'Actualizar trabajo',
          booking: 'Reservando…',
        },
        list: {
          title: 'Trabajos',
        },
        card: {
          untitled: 'Trabajo sin título',
          logged: 'REGISTRADO',
          status: 'ESTADO',
          live: '{hours}h en vivo',
          loggedEst: '{logged}h / {est}h',
        },
        actions: {
          start: 'Iniciar',
          complete: 'Completar',
          invoice: 'Factura',
        },
        empty: {
          filter: 'No hay trabajos {status} — pruebe otro filtro.',
          noJobs: 'Aún no hay trabajos — reserve su primer trabajo por hora con el formulario.',
          bookJob: 'Reservar trabajo',
        },
      },
      invoice: {
        list: {
          title: 'Facturas',
        },
        table: {
          number: 'Factura #',
          client: 'Cliente',
          charge: 'Cargo',
          extras: 'Extras',
          tax: 'Impuesto',
          total: 'Total',
          status: 'Estado',
        },
        actions: {
          markPaid: 'Marcar pagada',
          markUnpaid: 'Marcar impaga',
          pdf: 'PDF',
        },
        empty: {
          filter: 'No hay facturas {status} — pruebe otro filtro.',
          noInvoices: 'Aún no hay facturas — complete un viaje y genere una factura en Viajes.',
          goToRides: 'Ir a viajes',
        },
      },
      activity: {
        title: 'Registro de actividad',
        table: {
          time: 'Hora',
          action: 'Acción',
          type: 'Tipo',
          details: 'Detalles',
        },
        empty: 'Aún no hay actividad. Las acciones aparecerán aquí al usar la app.',
        couldNotLoad: 'No se pudo cargar el registro de actividad.',
        entries: '{count} registros',
      },
      modal: {
        changePassword: {
          title: 'Cambiar contraseña',
          current: 'Contraseña actual',
          new: 'Nueva contraseña',
          confirm: 'Confirmar nueva contraseña',
          changeBtn: 'Cambiar',
        },
        profileManager: {
          title: 'Gestionar perfiles',
          type: 'Tipo',
          typeDriver: 'DRIVER (muestra Viajes)',
          typeEngineer: 'ENGINEER / Por hora',
          typeHandyman: 'HANDYMAN / Por hora',
          typeOther: 'OTHER / Por hora',
          nameOptional: 'Nombre (opcional)',
          namePlaceholder: 'ej. Taxi principal o Dev freelance',
          createNew: 'Crear nuevo',
          saveChanges: 'Guardar cambios',
          loading: 'Cargando…',
          noProfiles: 'Aún no hay perfiles. Cree uno abajo.',
          edit: 'editar',
          saved: 'Guardado.',
          created: 'Perfil creado.',
        },
        createTable: {
          title: 'Crear tabla personalizada',
          tableName: 'Nombre de tabla',
          namePlaceholder: 'ej. Vehículos, Gastos…',
          nameRequired: 'El nombre de tabla es obligatorio',
          columns: 'Columnas',
          columnName: 'Nombre de columna',
          colTypeText: 'Texto',
          colTypeNumber: 'Número',
          colTypeBool: 'Sí/No',
          colTypeDate: 'Fecha',
          colTypeLink: 'Enlace',
          trueLabel: 'Etiqueta verdadero (ej. Pagado)',
          falseLabel: 'Etiqueta falso (ej. Impago)',
          selectTableLink: 'Seleccionar tabla a enlazar…',
          addColumn: '+ Agregar columna',
          colsRequired: 'Se requiere al menos una columna',
          createBtn: 'Crear tabla',
        },
      },
      customTable: {
        addRow: 'Agregar fila',
        editRow: 'Editar fila',
        updateRow: 'Actualizar fila',
        addColumn: 'Agregar columna',
        editColumn: 'Editar columna',
        linkToTable: 'Enlazar a tabla',
        selectTable: 'Seleccionar tabla…',
        trueLabel: 'Etiqueta verdadero',
        falseLabel: 'Etiqueta falso',
        deleteColumn: 'Eliminar columna',
        noRows: 'Aún no hay filas',
        addColumnTitle: 'Agregar columna',
      },
      search: {
        noResults: 'Sin resultados',
        clients: 'Clientes',
        rides: 'Viajes',
        invoices: 'Facturas',
      },
      messages: {
        personAdded: '✓ {name} agregado',
        personUpdated: '✓ {name} actualizado',
        rideBooked: '✓ Viaje reservado',
        rideUpdated: '✓ Viaje actualizado',
        jobBooked: '✓ Trabajo reservado ($20/hr)',
        jobUpdated: '✓ Trabajo actualizado',
        jobCompleted: '✓ Trabajo completado. Horas totales × tarifa calculadas.',
        rowAdded: '✓ Fila agregada',
        rowUpdated: '✓ Fila actualizada',
        saveFailed: 'No se pudo guardar — revisa el formulario e intenta de nuevo.',
        saveFailedShort: 'No se pudo guardar — intenta de nuevo.',
        requestFailed: 'Algo salió mal — intenta de nuevo.',
        networkError: 'No se pudo conectar al servidor — revisa tu conexión.',
        notFoundGeneric: 'No se encontró — actualiza la página e intenta de nuevo.',
        conflictGeneric: 'Esta acción no está permitida ahora — actualiza y revisa el estado.',
        assignFailed: 'No se pudo asignar el chofer — puede que ya no esté disponible.',
        scheduledPast: 'La fecha/hora debe ser en el futuro — elige un momento más tarde.',
        clientNotFound: 'Cliente no encontrado — actualiza la lista e intenta de nuevo.',
        profileNotFound: 'Perfil no encontrado — cambia a "Todos (cuenta)" en el menú e intenta de nuevo.',
        rideNotFound: 'Viaje o trabajo no encontrado.',
        driverNotFound: 'Chofer no encontrado.',
        duplicatePhone: 'Ya existe un cliente con este número de teléfono.',
        invalidRequest: 'Solicitud inválida — revisa los datos e intenta de nuevo.',
        scheduledRequired: 'La fecha/hora programada es obligatoria.',
        onlyScheduledEdit: 'Solo se pueden editar trabajos programados.',
        sessionExpired: 'Sesión expirada — actualiza la página e inicia sesión de nuevo.',
        serverError: 'Error del servidor — intenta de nuevo en un momento.',
        httpError: 'El servidor rechazó la solicitud (error {status}). Actualice la página e intente de nuevo.',
        deleteConfirm: '¿Eliminar {name}?',
        deleteClients: '¿Eliminar {count} cliente(s)?',
        deleteDrivers: '¿Eliminar {count} chofer(es)?',
        deleteRows: '¿Eliminar {count} fila(s)?',
        deleteRow: '¿Eliminar esta fila?',
        deleteColumn: '¿Eliminar columna "{name}"?',
        deleteTable: '¿Eliminar tabla "{name}" y todas sus filas?',
        cannotDeleteClient: 'No se puede eliminar — el cliente tiene viajes activos',
        cannotDeleteDriver: 'No se puede eliminar — el chofer tiene viajes activos',
        deleteFailed: 'No se pudo eliminar — actualiza la página e intenta de nuevo.',
        someDeletesFailed: 'Algunas eliminaciones fallaron:\n{errors}',
        cancelRide: '¿Cancelar este viaje?',
        cannotCancelRide: 'No se puede cancelar este viaje',
        cancelFailed: 'No se pudo cancelar el viaje — puede que ya esté terminado.',
        markPayoutPaid: '¿Marcar este pago al chofer como pagado?',
        markPayoutFailed: 'No se pudo marcar el pago — solo aplica a viajes completados.',
        completeFailed: 'No se pudo completar el viaje — revise las horas e intente de nuevo.',
        completeTimesRequired: 'Ingrese hora de inicio y fin antes de completar.',
        completeEndAfterStart: 'La hora de fin debe ser posterior a la de inicio.',
        completeBillableHours: 'Ingrese las horas facturables antes de completar este trabajo.',
        completeWrongStatus: 'Este viaje aún no se puede completar — asigne chofer e ingrese horas, o inicie el temporizador.',
        completeStartTimerFirst: 'Inicie el temporizador del viaje o ingrese hora de inicio y fin.',
        cannotStartRide: 'No se puede iniciar este viaje — revise su estado.',
        cannotCancelRideStatus: 'No se puede cancelar — el viaje ya está terminado o cancelado.',
        driverNotAvailable: 'Ese chofer no está disponible — elija otro.',
        driverIdRequired: 'Seleccione un chofer antes de asignar.',
        invoiceRideNotCompleted: 'El viaje debe estar completado para crear la factura.',
        payoutOnlyCompleted: 'El pago solo se puede marcar en viajes completados.',
        rideIdRequired: 'El viaje es obligatorio.',
        invoiceNotFound: 'Factura no encontrada — actualice la lista.',
        tableNotFound: 'Tabla no encontrada — actualice e intente de nuevo.',
        availabilityRequired: 'Seleccione el estado de disponibilidad del chofer.',
        payoutTypeInvalid: 'El tipo de pago debe ser porcentaje o tarifa fija.',
        availabilityInvalid: 'La disponibilidad debe ser disponible, en viaje o fuera de servicio.',
        columnNameExists: 'Ya existe una columna con ese nombre.',
        linkedTableNotFound: 'No se encontró la tabla enlazada.',
        completeJobConfirm: '¿Marcar trabajo como completado ahora? (usa tiempo transcurrido × $20)',
        startJobFailed: 'No se pudo iniciar el trabajo — verifique que siga programado.',
        invoiceFailed: 'No se pudo crear la factura: {message}',
        failed: 'Algo salió mal: {message}',
        failedCreateTable: 'No se pudo crear la tabla — revise el nombre y las columnas.',
        columnNameRequired: 'El nombre de columna es obligatorio',
        selectLinkTable: 'Seleccione una tabla para enlazar',
        typeRequired: 'El tipo es obligatorio',
        failedLoadProfiles: 'No se pudieron cargar los perfiles — actualice la página.',
        failedSave: 'No se pudo guardar — intente de nuevo.',
        failedCreate: 'No se pudo crear — revise los datos.',
        currentPasswordRequired: 'La contraseña actual es obligatoria',
        passwordMinLength: 'La nueva contraseña debe tener al menos 4 caracteres',
        passwordsNoMatch: 'Las contraseñas no coinciden',
        passwordChanged: 'Contraseña cambiada correctamente',
        changePasswordFailed: 'No se pudo cambiar la contraseña — revise la contraseña actual.',
        linkGoogleFailed: 'No se pudo vincular con Google — intente de nuevo.',
      },
      help: {
        welcome: {
          title: 'Bienvenido a Tatalance',
          content: '<h3>Tu herramienta de gestión de clientes chauffeur</h3>'
            + '<p>Tatalance te ayuda a gestionar clientes, choferes, viajes, facturas y cualquier dato personalizado — todo en un solo lugar.</p>'
            + '<div class="help-tip"><span class="tip-icon">📋</span><div class="tip-text"><strong>Pestañas arriba</strong> — cambia entre Clientes, Choferes, Viajes, Facturas y las tablas personalizadas que crees.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">➕</span><div class="tip-text"><strong>El botón +</strong> — crea tus propias tablas para lo que necesites (vehículos, gastos, notas, etc.).</div></div>'
            + '<div class="help-tip"><span class="tip-icon">❓</span><div class="tip-text"><strong>El botón ?</strong> — ¡estás aquí! Vuelve cuando quieras para repasar.</div></div>',
        },
        clients: {
          title: 'Clientes',
          content: '<h3>Agregar y gestionar tus clientes</h3>'
            + '<div class="help-tip"><span class="tip-icon">👤</span><div class="tip-text"><strong>Agregar un cliente</strong> — completa nombre, apellido y teléfono (formato internacional: +1 y dígitos). El correo es opcional.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">✏️</span><div class="tip-text"><strong>Editar</strong> — haz clic en Editar en cualquier fila para actualizar sus datos. El formulario pasa a modo edición.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🗑️</span><div class="tip-text"><strong>Eliminar</strong> — haz clic en Elim. para quitar un cliente. No puedes eliminar un cliente con viajes activos.</div></div>'
            + '<p>La lista se actualiza sola cada 10 segundos para que siempre veas los datos más recientes.</p>',
        },
        drivers: {
          title: 'Choferes',
          content: '<h3>Gestiona tu lista de choferes</h3>'
            + '<div class="help-tip"><span class="tip-icon">🚗</span><div class="tip-text"><strong>Agregar un chofer</strong> — nombre, teléfono, vehículo opcional e información de pago (porcentaje o tarifa fija).</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🟢</span><div class="tip-text"><strong>Disponibilidad</strong> — cambia el estado del chofer con el menú: Disponible, En viaje o Fuera de servicio.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">💰</span><div class="tip-text"><strong>Pago</strong> — configura Porcentaje (ej. 70%) o Fijo (ej. $50 por viaje). Aparece en las facturas.</div></div>',
        },
        rides: {
          title: 'Viajes',
          content: '<h3>Reservar y gestionar viajes</h3>'
            + '<div class="help-tip"><span class="tip-icon">📅</span><div class="tip-text"><strong>Reservar un viaje</strong> — elige un cliente, fecha/hora de recogida, lugares y precio base opcional.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🔗</span><div class="tip-text"><strong>Asignar chofer</strong> — una vez reservado, usa el menú en la columna Chofer para asignar uno disponible.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">✅</span><div class="tip-text"><strong>Completar</strong> — después de asignar, haz clic en Completar. Indica hora real de inicio/fin, peajes y extras.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">❌</span><div class="tip-text"><strong>Cancelar</strong> — cancela un viaje en cualquier momento antes de completarlo. El chofer vuelve a estar disponible.</div></div>',
        },
        invoices: {
          title: 'Facturas',
          content: '<h3>Generar y seguir facturas</h3>'
            + '<div class="help-tip"><span class="tip-icon">📄</span><div class="tip-text"><strong>Generar</strong> — al completar un viaje, haz clic en Factura en ese viaje. Calcula base + extras + impuesto.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">💵</span><div class="tip-text"><strong>Marcar pagada / pendiente</strong> — cambia el estado de la factura con un clic. Sin procesamiento de pagos complejo.</div></div>'
            + '<p>Las facturas muestran el desglose: tarifa base, cargos adicionales, impuesto y total.</p>',
        },
        customTables: {
          title: 'Tablas personalizadas',
          content: '<h3>Crea tus propias tablas de datos</h3>'
            + '<div class="help-tip"><span class="tip-icon">➕</span><div class="tip-text"><strong>Crear una tabla</strong> — haz clic en el botón + en la barra de navegación. Nombra la tabla y agrega columnas: Texto, Número, Sí/No o Fecha.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">📋</span><div class="tip-text"><strong>Usarla</strong> — tu tabla aparece como una pestaña nueva. Agrega filas, edítalas, elimínalas — igual que Clientes o Choferes.</div></div>'
            + '<div class="help-tip"><span class="tip-icon">🗑️</span><div class="tip-text"><strong>Eliminar una tabla</strong> — pasa el cursor sobre el nombre de la pestaña y haz clic en la x. Se elimina la tabla y todas sus filas.</div></div>'
            + '<p>Ideas: Vehículos, Gastos, Combustible, Notas de clientes, lo que necesites registrar.</p>',
        },
      },
      auth: {
        login: {
          pageTitle: 'Iniciar sesión — Tatalance',
          title: 'Iniciar sesión',
          subtitle: 'Tatalance — plataforma de chofer demo',
          username: 'Usuario',
          password: 'Contraseña',
          submit: 'Iniciar sesión',
          submitting: 'Iniciando sesión…',
          forgotPassword: '¿Olvidaste tu contraseña?',
          googleSignIn: 'Iniciar sesión con Google',
          or: 'o',
          noAccount: '¿No tienes cuenta?',
          createOne: 'Crear una',
          wrongCredentials: 'Usuario o contraseña incorrectos.',
          signedOut: 'Has cerrado sesión.',
          serverError: 'No se pudo conectar al servidor. Intenta de nuevo.',
        },
        register: {
          pageTitle: 'Crear cuenta — Tatalance',
          title: 'Crear cuenta',
          subtitle: 'Tatalance — plataforma de chofer',
          username: 'Usuario',
          password: 'Contraseña',
          confirmPassword: 'Confirmar contraseña',
          securityQuestion: 'Pregunta de seguridad',
          securityOptional: '(opcional — para recuperar contraseña)',
          yourAnswer: 'Tu respuesta',
          submit: 'Crear cuenta',
          hasAccount: '¿Ya tienes cuenta?',
          signIn: 'Iniciar sesión',
          usernameRequired: 'El usuario es obligatorio',
          passwordMin: 'La contraseña debe tener al menos 4 caracteres',
          passwordsNoMatch: 'Las contraseñas no coinciden',
          answerRequired: 'Responde la pregunta de seguridad',
          registrationFailed: 'No se pudo crear la cuenta — revise usuario y contraseña.',
          accountCreated: '¡Cuenta creada!',
          serverError: 'No se pudo conectar al servidor',
        },
        forgot: {
          pageTitle: 'Restablecer contraseña — Tatalance',
          title: 'Restablecer contraseña',
          subtitle: 'Responde tu pregunta de seguridad',
          username: 'Usuario',
          yourAnswer: 'Tu respuesta',
          newPassword: 'Nueva contraseña',
          confirmPassword: 'Confirmar nueva contraseña',
          next: 'Siguiente',
          submit: 'Restablecer contraseña',
          backToSignIn: 'Volver a iniciar sesión',
          usernameRequired: 'El usuario es obligatorio',
          answerRequired: 'La respuesta es obligatoria',
          passwordMin: 'La contraseña debe tener al menos 4 caracteres',
          passwordsNoMatch: 'Las contraseñas no coinciden',
          accountNotFound: 'Cuenta no encontrada',
          resetFailed: 'No se pudo restablecer la contraseña — intente de nuevo.',
          serverError: 'No se pudo conectar al servidor',
        },
        securityQuestions: {
          none: '— ninguna —',
          pet: '¿Cuál es el nombre de tu mascota?',
          city: '¿En qué ciudad naciste?',
          food: '¿Cuál es tu comida favorita?',
          car: '¿Cuál fue tu primer auto?',
          maiden: '¿Cuál es el apellido de soltera de tu madre?',
        },
      },
      lang: {
        switch: 'Cambiar idioma / Switch language',
      },
    },
  };

  let currentLang = 'en';

  function resolveLang() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'en' || stored === 'es') return stored;
    } catch (_) { /* private browsing */ }
    const nav = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
    return nav.startsWith('es') ? 'es' : 'en';
  }

  function t(key, vars) {
    const parts = key.split('.');
    let obj = i18n[currentLang];
    for (const part of parts) {
      if (obj == null) return key;
      obj = obj[part];
    }
    let val = obj ?? key;
    if (vars && typeof val === 'string') {
      Object.keys(vars).forEach((k) => {
        val = val.replace(new RegExp('\\{' + k + '\\}', 'g'), vars[k]);
      });
    }
    return val;
  }

  function applyTranslations(root) {
    const scope = root || document;

    scope.querySelectorAll('[data-i18n]').forEach((el) => {
      const val = t(el.dataset.i18n);
      if (val && val !== el.dataset.i18n) el.textContent = val;
    });

    scope.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
      const val = t(el.dataset.i18nPlaceholder);
      if (val) el.placeholder = val;
    });

    scope.querySelectorAll('[data-i18n-title]').forEach((el) => {
      const val = t(el.dataset.i18nTitle);
      if (val) el.title = val;
    });

    scope.querySelectorAll('[data-i18n-aria]').forEach((el) => {
      const val = t(el.dataset.i18nAria);
      if (val) el.setAttribute('aria-label', val);
    });
  }

  function updateToggleUI() {
    const toggle = document.getElementById('langToggle');
    if (!toggle) return;
    toggle.classList.toggle('es', currentLang === 'es');
    toggle.querySelectorAll('.lang-option').forEach((opt) => {
      opt.classList.toggle('active', opt.dataset.lang === currentLang);
    });
  }

  function setLang(lang) {
    if (lang !== 'en' && lang !== 'es') return;
    currentLang = lang;
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (_) { /* private browsing */ }
    document.documentElement.lang = lang;
    updateToggleUI();
    applyTranslations();
    document.dispatchEvent(new CustomEvent('tatalance:langchange', { detail: { lang } }));
  }

  function toggleLang() {
    setLang(currentLang === 'en' ? 'es' : 'en');
  }

  function getLang() {
    return currentLang;
  }

  function init() {
    currentLang = resolveLang();
    document.documentElement.lang = currentLang;
    updateToggleUI();
    applyTranslations();
  }

  window.t = t;
  window.getLang = getLang;
  window.setLang = setLang;
  window.toggleLang = toggleLang;
  window.applyI18n = applyTranslations;

  /** Attach Accept-Language on /api/* calls so validation errors match UI locale (#106). */
  (function installApiLanguageHeader() {
    const nativeFetch = window.fetch.bind(window);
    window.fetch = function (input, init) {
      const url = typeof input === 'string' ? input : (input && input.url) || '';
      if (url.includes('/api/')) {
        init = init ? { ...init } : {};
        const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
        if (!headers.has('Accept-Language')) {
          headers.set('Accept-Language', currentLang === 'es' ? 'es' : 'en');
        }
        init.headers = headers;
      }
      return nativeFetch(input, init);
    };
  })();

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
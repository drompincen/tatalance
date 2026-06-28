/**
 * Maps API / network errors to user-friendly messages (EN/ES via i18n when loaded).
 */
(function () {
    function tr(key, vars) {
        if (typeof tf === 'function') return tf(key, vars);
        if (typeof t === 'function') return t(key, vars);
        return key;
    }

    const EXACT = {
        'Pickup date/time cannot be in the past': 'messages.scheduledPast',
        'Pickup date/time is required': 'messages.scheduledRequired',
        'Client not found': 'messages.clientNotFound',
        'Profile not found or does not belong to user': 'messages.profileNotFound',
        'Profile not found': 'messages.profileNotFound',
        'Ride not found': 'messages.rideNotFound',
        'Driver not found': 'messages.driverNotFound',
        'A client with this phone number already exists': 'messages.duplicatePhone',
        'Invalid request body': 'messages.invalidRequest',
        'Only SCHEDULED rides can be edited': 'messages.onlyScheduledEdit',
        'actualStart and actualEnd are required to complete an assigned ride': 'messages.completeTimesRequired',
        'actualEnd must be after actualStart': 'messages.completeEndAfterStart',
        'billableHours is required to complete a scheduled job without starting the timer': 'messages.completeBillableHours',
        'driverId is required': 'messages.driverIdRequired',
        'Driver is not available': 'messages.driverNotAvailable',
        'Ride must be COMPLETED to generate invoice': 'messages.invoiceRideNotCompleted',
        'Only completed rides can have payouts marked': 'messages.payoutOnlyCompleted',
        'rideId is required': 'messages.rideIdRequired',
        'Invoice not found': 'messages.invoiceNotFound',
        'Table not found': 'messages.tableNotFound',
        'availability is required': 'messages.availabilityRequired',
    };

    const PARTIAL = [
        ['cannot be in the past', 'messages.scheduledPast'],
        ['Profile not found', 'messages.profileNotFound'],
        ['Client not found', 'messages.clientNotFound'],
        ['Ride not found', 'messages.rideNotFound'],
        ['Driver not found', 'messages.driverNotFound'],
        ['phone number already exists', 'messages.duplicatePhone'],
        ['Invalid request body', 'messages.invalidRequest'],
        ['Only SCHEDULED rides can be edited', 'messages.onlyScheduledEdit'],
        ['actualStart and actualEnd are required', 'messages.completeTimesRequired'],
        ['actualEnd must be after actualStart', 'messages.completeEndAfterStart'],
        ['billableHours is required', 'messages.completeBillableHours'],
        ['Cannot complete a ride in status', 'messages.completeWrongStatus'],
        ['start the timer first', 'messages.completeStartTimerFirst'],
        ['Cannot start a ride in status', 'messages.cannotStartRide'],
        ['Cannot cancel a', 'messages.cannotCancelRideStatus'],
        ['Cannot delete client with active rides', 'messages.cannotDeleteClient'],
        ['Cannot delete driver with active rides', 'messages.cannotDeleteDriver'],
        ['Driver is not available', 'messages.driverNotAvailable'],
        ['Phone must have 10 digits', 'client.form.phoneE164'],
        ['E.164', 'client.form.phoneE164'],
        ['Phone must start with +', 'client.form.phoneE164'],
        ['Payout type must be one of', 'messages.payoutTypeInvalid'],
        ['Availability must be one of', 'messages.availabilityInvalid'],
        ['Column name already exists', 'messages.columnNameExists'],
        ['Linked table not found', 'messages.linkedTableNotFound'],
    ];

    function friendlyApiMessage(raw, status) {
        let text = (raw || '').trim();
        if (text === 'Failed to fetch' || text.includes('NetworkError') || text === 'Load failed') {
            return tr('messages.networkError');
        }
        if (!text || text === 'Bad Request' || text === 'Internal Server Error' || text === 'Conflict') {
            if (status === 401 || status === 403) return tr('messages.sessionExpired');
            if (status === 404) return tr('messages.notFoundGeneric');
            if (status === 409) return tr('messages.conflictGeneric');
            if (status >= 500) return tr('messages.serverError');
            if (status === 400) return tr('messages.invalidRequest');
            if (status) return tr('messages.httpError', { status });
            return tr('messages.requestFailed');
        }
        if (EXACT[text]) return tr(EXACT[text]);
        for (const [needle, key] of PARTIAL) {
            if (text.includes(needle)) return tr(key);
        }
        if (/^complete failed:/i.test(text) || /^start failed:/i.test(text)) {
            return status === 409 ? tr('messages.completeWrongStatus')
                : status === 400 ? tr('messages.completeTimesRequired')
                : tr('messages.completeFailed');
        }
        return text;
    }

    async function apiErrorFromResponse(r) {
        const text = await r.text().catch(() => '');
        let err = {};
        if (text) {
            try {
                err = JSON.parse(text);
            } catch (_) {
                return friendlyApiMessage(text.replace(/\s+/g, ' ').trim(), r.status);
            }
        }
        const msgs = Array.isArray(err.errors)
            ? err.errors.map(e => e && e.message).filter(Boolean)
            : [];
        const raw = (msgs.join('; ')
            || err.detail
            || err.message
            || (typeof err.error === 'string' && err.error !== 'Bad Request' ? err.error : '')
            || '').trim();
        return friendlyApiMessage(raw, r.status);
    }

    function formError(msg, fallbackKey) {
        const fb = fallbackKey || 'messages.requestFailed';
        let m = (msg || tr(fb)).trim();
        if (!m) m = tr(fb);
        return m.startsWith('✗') ? m : '✗ ' + m;
    }

    function resolveError(err, fallbackKey) {
        const fb = fallbackKey || 'messages.requestFailed';
        if (!err) return tr(fb);
        const msg = (err.message || '').trim();
        if (!msg) return tr(fb);
        return friendlyApiMessage(msg, 0);
    }

    window.friendlyApiMessage = friendlyApiMessage;
    window.apiErrorFromResponse = apiErrorFromResponse;
    window.formError = formError;
    window.resolveError = resolveError;
})();
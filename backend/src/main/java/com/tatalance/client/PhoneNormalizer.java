package com.tatalance.client;

/**
 * Accepts common US phone formats and stores E.164 (+1XXXXXXXXXX).
 * 10 digits (no country code) or 11 digits starting with 1.
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }
        throw new IllegalArgumentException(
                "Phone must have 10 digits, or 11 digits starting with 1 (e.g. 2223334444 or +12223334444)");
    }
}
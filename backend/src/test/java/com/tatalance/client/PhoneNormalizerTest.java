package com.tatalance.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "2125551234",
            "12125551234",
            "+12125551234",
            "(212) 555-1234",
            "212-555-1234",
            "1-222-333-4444",
            "+1 222 333 4444"
    })
    void normalize_acceptsCommonUsFormats(String raw) {
        String result = PhoneNormalizer.normalize(raw);
        assertTrue(result.startsWith("+1"));
        assertEquals(12, result.length());
    }

    @Test
    void normalize_tenDigits_prependsCountryCode() {
        assertEquals("+12125551234", PhoneNormalizer.normalize("2125551234"));
    }

    @Test
    void normalize_elevenDigitsStartingWithOne_keepsCountryCode() {
        assertEquals("+12125551234", PhoneNormalizer.normalize("12125551234"));
    }

    @Test
    void normalize_rejectsTooShort() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize("+123"));
    }

    @Test
    void normalize_rejectsTooLong() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize("121255512341"));
    }

    @Test
    void normalize_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize("   "));
    }
}
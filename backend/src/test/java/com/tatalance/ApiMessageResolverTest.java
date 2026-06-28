package com.tatalance;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiMessageResolverTest {

    @Test
    void wantsSpanish_acceptsEsHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Accept-Language", "es");
        assertTrue(ApiMessageResolver.wantsSpanish(req));
    }

    @Test
    void wantsSpanish_acceptsEsMxHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Accept-Language", "es-MX,es;q=0.9");
        assertTrue(ApiMessageResolver.wantsSpanish(req));
    }

    @Test
    void wantsSpanish_rejectsEnglishHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Accept-Language", "en");
        assertFalse(ApiMessageResolver.wantsSpanish(req));
    }

    @Test
    void fieldMessage_returnsSpanishPickupLocation() {
        assertEquals("El lugar de recogida es obligatorio",
                ApiMessageResolver.fieldMessage("pickupLocation", "must not be blank", true));
    }

    @Test
    void fieldMessage_returnsEnglishPickupLocation() {
        assertEquals("Pickup location is required",
                ApiMessageResolver.fieldMessage("pickupLocation", "must not be blank", false));
    }

    @Test
    void translate_returnsSpanishClientNotFound() {
        assertEquals("Cliente no encontrado — actualiza la lista e intenta de nuevo.",
                ApiMessageResolver.translate("Client not found", true));
    }

    @Test
    void translate_leavesEnglishWhenNotSpanish() {
        assertEquals("Client not found",
                ApiMessageResolver.translate("Client not found", false));
    }
}
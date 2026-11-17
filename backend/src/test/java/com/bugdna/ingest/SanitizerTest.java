package com.bugdna.ingest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizerTest {

    private Sanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new Sanitizer();
    }

    @Test
    @DisplayName("Sanitizer masks emails, bearer tokens, long hex tokens, and long digit runs")
    void testSanitizeTextPiiMasking() {
        String raw = "Failed checkout for user alice.smith@example.com with card 4111222233334444 and auth Bearer eyJhbGciOiJIUzI1NiJ9.abc123def";
        String clean = sanitizer.sanitizeText(raw, 500);

        assertFalse(clean.contains("alice.smith@example.com"));
        assertTrue(clean.contains("[email]"));
        assertFalse(clean.contains("4111222233334444"));
        assertTrue(clean.contains("[number]"));
        assertFalse(clean.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(clean.contains("Bearer [token]"));
    }

    @Test
    @DisplayName("Sanitizer strips query strings, hashes, and replaces numeric/UUID path IDs")
    void testSanitizePath() {
        assertEquals("/product/:id", sanitizer.sanitizePath("/product/999?promo=SAVE20#reviews"));
        assertEquals("/orders/:id/receipt", sanitizer.sanitizePath("/orders/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/receipt?secret=123"));
        assertEquals("/checkout", sanitizer.sanitizePath("/checkout/"));
    }
}

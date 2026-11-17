package com.bugdna.ingest;

import com.bugdna.bug.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintServiceTest {

    private FingerprintService fingerprintService;

    @BeforeEach
    void setUp() {
        fingerprintService = new FingerprintService();
    }

    @Test
    @DisplayName("Bug 104 canonical string SHA-256 equals the seeded fingerprint in V3__seed.sql")
    void testBug104FingerprintMatchesSeed() {
        // Bug 104 in V3__seed.sql:
        // Canonical string: v1|API_ERROR|POST|/demo-api/cart/coupon|400|INVALID_COUPON_FORMAT
        String canonical = fingerprintService.computeCanonical(
                ErrorType.API_ERROR,
                "POST",
                "/demo-api/cart/coupon",
                400,
                "INVALID_COUPON_FORMAT",
                null,
                null,
                null
        );

        assertEquals("v1|API_ERROR|POST|/demo-api/cart/coupon|400|INVALID_COUPON_FORMAT", canonical);

        String fingerprint = fingerprintService.computeFingerprint(canonical);
        // This MUST equal the SHA2 in MySQL for bug 104
        assertEquals("87564c7cca08eb024a86fd2954cd906551d6149d061490bf46a97c597706e968", fingerprint);
    }

    @Test
    @DisplayName("API Error normalization collapses numeric IDs and query strings")
    void testApiErrorEndpointNormalization() {
        String canonical1 = fingerprintService.computeCanonical(
                ErrorType.API_ERROR,
                "get",
                "/demo-api/products/123?ref=sidebar&sort=desc",
                404,
                "PRODUCT_NOT_FOUND",
                null,
                null,
                null
        );

        String canonical2 = fingerprintService.computeCanonical(
                ErrorType.API_ERROR,
                "GET",
                "/demo-api/products/999#reviews",
                404,
                "PRODUCT_NOT_FOUND",
                null,
                null,
                null
        );

        assertEquals(canonical1, canonical2);
        assertEquals("v1|API_ERROR|GET|/demo-api/products/<id>|404|PRODUCT_NOT_FOUND", canonical1);
        assertEquals(
                fingerprintService.computeFingerprint(canonical1),
                fingerprintService.computeFingerprint(canonical2)
        );
    }

    @Test
    @DisplayName("API Error normalization collapses UUIDs")
    void testApiErrorUuidNormalization() {
        String canonical = fingerprintService.computeCanonical(
                ErrorType.API_ERROR,
                "DELETE",
                "/demo-api/orders/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                500,
                "INTERNAL_ERROR",
                null,
                null,
                null
        );

        assertEquals("v1|API_ERROR|DELETE|/demo-api/orders/<id>|500|INTERNAL_ERROR", canonical);
    }

    @Test
    @DisplayName("JS Error normalization strips line/col and normalizes quotes and numbers")
    void testJsErrorNormalization() {
        String canonical1 = fingerprintService.computeCanonical(
                ErrorType.JS_ERROR,
                null,
                null,
                null,
                null,
                "TypeError",
                "Cannot read properties of undefined (reading 'price')",
                "productPage.js:renderPrice:142:25"
        );

        String canonical2 = fingerprintService.computeCanonical(
                ErrorType.JS_ERROR,
                null,
                null,
                null,
                null,
                "TypeError",
                "cannot read properties of undefined (reading \"price\")",
                "productpage.js:renderprice:88:10"
        );

        assertEquals(canonical1, canonical2);
        assertEquals("v1|JS_ERROR|TypeError|cannot read properties of undefined (reading <s>)|productpage.js:renderprice", canonical1);
    }

    @Test
    @DisplayName("Network Error normalization retains method and normalized endpoint")
    void testNetworkErrorNormalization() {
        String canonical = fingerprintService.computeCanonical(
                ErrorType.NETWORK_ERROR,
                "post",
                "/demo-api/analytics/heartbeat?ts=1700000000",
                null,
                null,
                null,
                null,
                null
        );

        assertEquals("v1|NETWORK_ERROR|POST|/demo-api/analytics/heartbeat", canonical);
    }

    @Test
    @DisplayName("Different errorCode or status produces distinct fingerprints; same normalized error produces identical fingerprint")
    void testTableDrivenFingerprintEquivalenceAndSeparation() {
        record Case(String method, String ep1, String ep2, int status1, int status2, String code1, String code2, boolean shouldMatch) {}

        var cases = java.util.List.of(
                new Case("POST", "/demo-api/orders/101?debug=1", "/demo-api/orders/999?debug=2", 500, 500, "DEMO_COUPON_RULE_ERROR", "DEMO_COUPON_RULE_ERROR", true),
                new Case("GET", "/demo-api/products/12", "/demo-api/products/84729", 404, 404, "PRODUCT_NOT_FOUND", "PRODUCT_NOT_FOUND", true),
                new Case("POST", "/demo-api/checkout", "/demo-api/checkout", 500, 500, "DEMO_COUPON_RULE_ERROR", "PAYMENT_DECLINED", false),
                new Case("POST", "/demo-api/checkout", "/demo-api/checkout", 500, 400, "DEMO_COUPON_RULE_ERROR", "DEMO_COUPON_RULE_ERROR", false)
        );

        for (Case c : cases) {
            String fp1 = fingerprintService.computeFingerprint(
                    fingerprintService.computeCanonical(ErrorType.API_ERROR, c.method(), c.ep1(), c.status1(), c.code1(), null, null, null));
            String fp2 = fingerprintService.computeFingerprint(
                    fingerprintService.computeCanonical(ErrorType.API_ERROR, c.method(), c.ep2(), c.status2(), c.code2(), null, null, null));
            if (c.shouldMatch()) {
                assertEquals(fp1, fp2, "Expected matching fingerprints for " + c);
            } else {
                assertNotEquals(fp1, fp2, "Expected distinct fingerprints for " + c);
            }
        }
    }
}


package com.bugdna.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DevTokenStoreTest {

    private DevTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new DevTokenStore();
    }

    @Test
    @DisplayName("Create token returns valid token info with 8h TTL")
    void testCreateAndValidateToken() {
        var tokenInfo = tokenStore.createToken("dev", "Lead Developer");
        assertNotNull(tokenInfo.token());
        assertEquals("dev", tokenInfo.username());
        assertEquals("Lead Developer", tokenInfo.displayName());
        assertFalse(tokenInfo.isExpired());

        Optional<DevTokenStore.TokenInfo> retrieved = tokenStore.validateToken(tokenInfo.token());
        assertTrue(retrieved.isPresent());
        assertEquals("dev", retrieved.get().username());
    }

    @Test
    @DisplayName("Invalid, null or removed tokens return empty")
    void testInvalidToken() {
        assertTrue(tokenStore.validateToken(null).isEmpty());
        assertTrue(tokenStore.validateToken("").isEmpty());
        assertTrue(tokenStore.validateToken("non-existent-uuid").isEmpty());

        var tokenInfo = tokenStore.createToken("dev", "Dev User");
        tokenStore.removeToken(tokenInfo.token());
        assertTrue(tokenStore.validateToken(tokenInfo.token()).isEmpty());
    }
}


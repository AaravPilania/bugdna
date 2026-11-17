package com.bugdna.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DevTokenStore {

    public record TokenInfo(String token, String username, String displayName, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public TokenInfo createToken(String username, String displayName) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(8, ChronoUnit.HOURS);
        TokenInfo tokenInfo = new TokenInfo(token, username, displayName, expiresAt);
        tokens.put(token, tokenInfo);
        cleanExpired();
        return tokenInfo;
    }

    public Optional<TokenInfo> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            return Optional.empty();
        }
        if (tokenInfo.isExpired()) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(tokenInfo);
    }

    public void removeToken(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    private void cleanExpired() {
        tokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}


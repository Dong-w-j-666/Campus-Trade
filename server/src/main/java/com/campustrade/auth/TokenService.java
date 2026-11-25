package com.campustrade.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;
    private final long tokenHours;

    public TokenService(
            @Value("${app.auth.secret}") String secret,
            @Value("${app.auth.token-hours:168}") long tokenHours
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenHours = tokenHours;
    }

    public String issue(long userId) {
        long expiresAt = Instant.now().plus(tokenHours, ChronoUnit.HOURS).getEpochSecond();
        String payload = userId + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public Optional<Long> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            int separator = token.lastIndexOf('.');
            if (separator <= 0) {
                return Optional.empty();
            }
            String encodedPayload = token.substring(0, separator);
            String signature = token.substring(separator + 1);
            if (!constantTimeEquals(sign(encodedPayload), signature)) {
                return Optional.empty();
            }
            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedPayload),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }
            long userId = Long.parseLong(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt < Instant.now().getEpochSecond() || userId <= 0) {
                return Optional.empty();
            }
            return Optional.of(userId);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign authentication token", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }
}

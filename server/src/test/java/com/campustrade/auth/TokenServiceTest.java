package com.campustrade.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    @Test
    void issuesAndVerifiesAUserToken() {
        TokenService service = new TokenService("test-secret-that-is-long-enough", 1);

        String token = service.issue(42);

        assertThat(service.verify(token)).contains(42L);
    }

    @Test
    void rejectsTamperedToken() {
        TokenService service = new TokenService("test-secret-that-is-long-enough", 1);
        String token = service.issue(42);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThat(service.verify(tampered)).isEmpty();
    }

    @Test
    void rejectsExpiredAndMalformedTokens() {
        TokenService expired = new TokenService("test-secret-that-is-long-enough", -1);

        assertThat(expired.verify(expired.issue(7))).isEmpty();
        assertThat(expired.verify("not-a-token")).isEmpty();
        assertThat(expired.verify("")).isEmpty();
    }
}

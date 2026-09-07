package com.wordiga.global.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderUnitTest {
    private final JwtTokenProvider tokens = new JwtTokenProvider(
            "test-secret-key-for-wordiga-security-tests-1234567890", 3_600_000, 2_592_000_000L);

    @Test
    void separatesAccessAndRefreshTokens() {
        String access = tokens.create(1L);
        String refresh = tokens.createRefresh(1L);

        assertThat(tokens.isValidAccess(access)).isTrue();
        assertThat(tokens.isValidRefresh(access)).isFalse();
        assertThat(tokens.isValidRefresh(refresh)).isTrue();
        assertThat(tokens.isValidAccess(refresh)).isFalse();
        assertThat(tokens.getMemberId(refresh)).isEqualTo(1L);
        assertThat(tokens.getRefreshExpiration()).isEqualTo(2_592_000_000L);
    }
}

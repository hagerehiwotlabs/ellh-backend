package com.ellh.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JWTService.
 * No Spring context — pure unit test with the service instantiated directly.
 * Section 4.5.1 — JWTService specification.
 * Design Goal g — all AI calls mockable; here all auth calls are testable
 * without Spring Security context.
 */
class JWTServiceTest {

    private JWTService jwtService;
    private static final String TEST_SECRET =
            "test_jwt_secret_minimum_32_chars_long_for_hmac_sha256";
    private static final Long TEST_USER_ID = 42L;
    private static final String TEST_USER_TYPE = "FOREIGN_LEARNER";

    @BeforeEach
    void setUp() {
        jwtService = new JWTService(TEST_SECRET, 24L, 30L);
    }

    // ── generateAccessToken ───────────────────────────────────────────────────

    @Test
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateAccessToken_extractsCorrectUserId() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        assertThat(jwtService.extractUserId(token)).isEqualTo(TEST_USER_ID.toString());
    }

    @Test
    void generateAccessToken_extractsCorrectUserType() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        assertThat(jwtService.extractUserType(token)).isEqualTo(TEST_USER_TYPE);
    }

    // ── generateRefreshToken ──────────────────────────────────────────────────

    @Test
    void generateRefreshToken_isValidRefreshToken() {
        String token = jwtService.generateRefreshToken(TEST_USER_ID);
        assertThat(jwtService.isRefreshTokenValid(token)).isTrue();
    }

    @Test
    void generateRefreshToken_extractsCorrectUserId() {
        String token = jwtService.generateRefreshToken(TEST_USER_ID);
        assertThat(jwtService.extractUserId(token)).isEqualTo(TEST_USER_ID.toString());
    }

    @Test
    void generateAccessToken_isNotValidAsRefreshToken() {
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        // An access token must not pass the refresh token validator (wrong tokenType claim)
        assertThat(jwtService.isRefreshTokenValid(accessToken)).isFalse();
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        com.ellh.user.entity.User mockUser = com.ellh.user.entity.User.builder()
                .id(TEST_USER_ID)
                .email("test@ellh.com")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .build();
        assertThat(jwtService.isTokenValid(token, mockUser)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForWrongUserId() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        com.ellh.user.entity.User differentUser = com.ellh.user.entity.User.builder()
                .id(999L) // different ID
                .email("other@ellh.com")
                .passwordHash("hashed")
                .firstName("Other")
                .lastName("User")
                .build();
        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_USER_TYPE);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        com.ellh.user.entity.User user = com.ellh.user.entity.User.builder()
                .id(TEST_USER_ID).email("t@t.com").passwordHash("x")
                .firstName("A").lastName("B").build();
        assertThat(jwtService.isTokenValid(tampered, user)).isFalse();
    }

    // ── constructor guard ─────────────────────────────────────────────────────

    @Test
    void constructor_throwsIfSecretTooShort() {
        assertThatThrownBy(() -> new JWTService("tooshort", 24L, 30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }

    @Test
    void constructor_acceptsExactly32CharSecret() {
        // Should not throw
        assertThatNoException().isThrownBy(() ->
                new JWTService("exactly32charactersthisisexactly", 24L, 30L));
    }
}

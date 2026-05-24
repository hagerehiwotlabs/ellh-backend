package com.ellh.user.service;

import com.ellh.infrastructure.cache.SessionCacheService;
import com.ellh.infrastructure.exception.DuplicateResourceException;
import com.ellh.infrastructure.exception.UnauthorizedException;
import com.ellh.infrastructure.security.JWTService;
import com.ellh.user.dto.LoginRequest;
import com.ellh.user.dto.RefreshTokenRequest;
import com.ellh.user.dto.RegisterRequest;
import com.ellh.user.entity.*;
import com.ellh.user.repository.LearnerProfileRepository;
import com.ellh.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * All dependencies are mocked via Mockito — no Spring context, no database.
 * Section 4.5.1 — AuthService specification.
 * Design Goal g — testability: AuthService is testable in isolation because
 * all dependencies are injected interfaces (Trade-off i).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository            userRepository;
    @Mock LearnerProfileRepository  learnerProfileRepository;
    @Mock PasswordEncoder           passwordEncoder;
    @Mock JWTService                jwtService;
    @Mock SessionCacheService       sessionCacheService;
    @Mock AuthenticationManager     authenticationManager;

    @InjectMocks
    AuthService authService;

    private static final String EMAIL       = "test@ellh.com";
    private static final String PASSWORD    = "Password123!";
    private static final Long   USER_ID     = 1L;
    private static final String ACCESS_TKN  = "access.token.value";
    private static final String REFRESH_TKN = "refresh.token.value";
    private static final String TEST_USER_EMAIL = "test@test.com";

    @BeforeEach
    void setUpJwtMocks() {
        // JWT stubs moved into individual tests that need them
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_createsUserAndLearnerProfile() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed_password");

        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(ACCESS_TKN);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(REFRESH_TKN);

        User savedUser = User.builder()
                .id(USER_ID).email(EMAIL).passwordHash("hashed_password")
                .firstName("Abebe").lastName("Kebede").userType(UserType.FOREIGN_LEARNER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        LearnerProfile savedProfile = LearnerProfile.builder()
                .id(1L).user(savedUser).pathwayType(PathwayType.FOREIGN_LEARNER).build();
        when(learnerProfileRepository.save(any(LearnerProfile.class))).thenReturn(savedProfile);
        when(learnerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(savedProfile));

        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFirstName("Abebe");
        request.setLastName("Kebede");
        request.setUserType(UserType.FOREIGN_LEARNER);

        var response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TKN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TKN);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        verify(learnerProfileRepository).save(any(LearnerProfile.class));
        verify(sessionCacheService).createSession(eq(USER_ID), anyString());
    }

    @Test
    void register_throwsDuplicateException_whenEmailExists() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFirstName("A");
        request.setLastName("B");
        request.setUserType(UserType.FOREIGN_LEARNER);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_contentAdmin_doesNotCreateLearnerProfile() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(ACCESS_TKN);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(REFRESH_TKN);

        User admin = User.builder().id(USER_ID).email(EMAIL).passwordHash("hashed")
                .firstName("Admin").lastName("User")
                .userType(UserType.CONTENT_ADMIN)
                .build();

        when(userRepository.save(any())).thenReturn(admin);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setUserType(UserType.CONTENT_ADMIN);

        authService.register(request);

        verify(learnerProfileRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        User user = User.builder()
                .id(USER_ID).email(EMAIL).passwordHash("hashed")
                .firstName("A").lastName("B")
                .userType(UserType.FOREIGN_LEARNER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(ACCESS_TKN);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(REFRESH_TKN);

        LearnerProfile profile = LearnerProfile.builder()
                .user(user).onboardingComplete(false).pathwayType(PathwayType.FOREIGN_LEARNER).build();
        when(learnerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        var response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TKN);
        assertThat(response.isOnboardingComplete()).isFalse();
        verify(userRepository).resetFailedAttempts(USER_ID);
        verify(sessionCacheService).createSession(eq(USER_ID), anyString());
    }

    @Test
    void login_badCredentials_throwsUnauthorizedException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(
                User.builder().id(USER_ID).email(EMAIL).passwordHash("x")
                        .firstName("A").lastName("B").userType(UserType.FOREIGN_LEARNER)
                        .build()));

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository).incrementFailedAttempts(USER_ID);
        verify(sessionCacheService, never()).createSession(any(), any());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        when(jwtService.isRefreshTokenValid(REFRESH_TKN)).thenReturn(true);
        when(jwtService.extractSubject(REFRESH_TKN)).thenReturn(EMAIL);   // ← fix: use extractSubject + email

        User user = User.builder()
                .id(USER_ID).email(EMAIL).passwordHash("h")
                .firstName("A").lastName("B")
                .userType(UserType.FOREIGN_LEARNER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(ACCESS_TKN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(learnerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TKN);

        var response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TKN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TKN); // unchanged
        verify(sessionCacheService).extendSession(USER_ID);
    }

    @Test
    void refresh_invalidToken_throwsUnauthorizedException() {
        when(jwtService.isRefreshTokenValid("invalid.token")).thenReturn(false);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalid or expired");
    }
}

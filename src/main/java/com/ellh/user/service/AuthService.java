package com.ellh.user.service;

import com.ellh.infrastructure.cache.SessionCacheService;
import com.ellh.infrastructure.exception.DuplicateResourceException;
import com.ellh.infrastructure.exception.UnauthorizedException;
import com.ellh.infrastructure.security.JWTService;
import com.ellh.user.dto.*;
import com.ellh.user.entity.*;
import com.ellh.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Optional;

/**
 * Handles user registration, login, and JWT token lifecycle.
 * Section 4.5.1 — User Domain (AuthService).
 * Section 4.5.5.2 FLOW-01 — Authentication and JWT Lifecycle.
 *
 * Three operations:
 *   register()  — creates User + LearnerProfile (for learner types), mints tokens
 *   login()     — authenticates credentials, enforces lockout, mints tokens
 *   refresh()   — validates refresh token, mints new access token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository          userRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PasswordEncoder          passwordEncoder;
    private final JWTService               jwtService;
    private final SessionCacheService      sessionCacheService;
    private final AuthenticationManager    authenticationManager;

    // ── register ──────────────────────────────────────────────────────────────

    /**
     * Registers a new user account.
     * For FOREIGN_LEARNER and BILINGUAL_LEARNER types a LearnerProfile is created
     * with default values (pathway set during DiagnosticAssessment in Sprint 3).
     *
     * @throws DuplicateResourceException if email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException(
                    "An account with this email address already exists");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .accountStatus(AccountStatus.ACTIVE) // skip email verification for v1.0
                .authProvider(AuthProvider.LOCAL)
                .userType(request.getUserType())
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={} type={}", saved.getId(), request.getUserType());

        log.info("Registration - email: {}, raw password: {}, encoded password: {}", 
            request.getEmail(), request.getPassword(), saved.getPasswordHash());

        // Create LearnerProfile for learner accounts (ContentAdmin has no profile)
        boolean isLearner = request.getUserType() == UserType.FOREIGN_LEARNER
                         || request.getUserType() == UserType.BILINGUAL_LEARNER;

        if (isLearner) {
            LearnerProfile profile = LearnerProfile.builder()
                    .user(saved)
                    .pathwayType(PathwayType.FOREIGN_LEARNER) // default; updated in DiagnosticAssessment
                    .currentCefrLevel(CefrLevel.A1)
                    .onboardingComplete(false)
                    .build();
            learnerProfileRepository.save(profile);
        }

        return buildAuthResponse(saved, false);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user and returns JWT tokens.
     * Enforces the 5-attempt account lockout (User.isAccountNonLocked()).
     * Updates last_active on successful login.
     *
     * @throws UnauthorizedException if credentials are invalid or account is locked
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // --- DEBUG START ---
        log.info("Login attempt - email: {}, raw password: '{}'", request.getEmail(), request.getPassword());
        userRepository.findByEmail(request.getEmail().toLowerCase())
            .ifPresent(user -> log.info("Stored password hash for {}: {}", request.getEmail(), user.getPasswordHash()));
        // --- DEBUG END ---
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            // Increment the failed attempt counter
            userRepository.findByEmail(request.getEmail().toLowerCase())
                    .ifPresent(u -> userRepository.incrementFailedAttempts(u.getId()));
            log.warn("BadCredentialsException for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        log.info("Login attempt - email: {}", request.getEmail());
        log.info("Stored password hash: {}", user.getPasswordHash());
        log.info("Raw password from request: {}", request.getPassword());
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Password matches: {}", matches);
        // Manual password match test
        boolean manualMatch = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Manual password match: {}", manualMatch);

        // Reset failed attempts and record login time
        userRepository.resetFailedAttempts(user.getId());
        user.recordSuccessfulLogin();
        userRepository.save(user);

        boolean onboardingComplete = false;
        if (user.getUserType() == UserType.FOREIGN_LEARNER
                || user.getUserType() == UserType.BILINGUAL_LEARNER) {
            onboardingComplete = learnerProfileRepository.findByUserId(user.getId())
                    .map(LearnerProfile::isOnboardingComplete)
                    .orElse(false);
        }

        log.info("User logged in: id={}", user.getId());
        return buildAuthResponse(user, onboardingComplete);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    /**
     * Validates the refresh token and mints a new access token.
     * The refresh token itself is NOT rotated — it retains its 30-day expiry.
     * Android calls this silently via OkHttp Authenticator when it receives a 401.
     *
     * @throws UnauthorizedException if refresh token is invalid or expired
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        String email = jwtService.extractSubject(refreshToken);  
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is not active");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getUserType().name());

        // Extend Redis session TTL
        sessionCacheService.extendSession(user.getId());

        log.debug("Token refreshed for userId={}", user.getId());

        boolean onboardingComplete = false;
        if (user.getUserType() == UserType.FOREIGN_LEARNER
                || user.getUserType() == UserType.BILINGUAL_LEARNER) {
            onboardingComplete = learnerProfileRepository.findByUserId(user.getId())
                    .map(LearnerProfile::isOnboardingComplete)
                    .orElse(false);
        }

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) 
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .userType(user.getUserType())
                .onboardingComplete(onboardingComplete)
                .accessTokenExpiresInSeconds(24 * 60 * 60L)
                .build();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, boolean onboardingComplete) {
        String accessToken  = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getUserType().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        // Store session in Redis (non-blocking; session loss on Redis outage is acceptable)
        sessionCacheService.createSession(user.getId(), accessToken.substring(0, 8));

        Optional<PathwayType> pathway = (user.getUserType() == UserType.FOREIGN_LEARNER
                || user.getUserType() == UserType.BILINGUAL_LEARNER)
                ? learnerProfileRepository.findByUserId(user.getId())
                        .map(LearnerProfile::getPathwayType)
                : Optional.empty();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .userType(user.getUserType())
                .pathwayType(pathway.orElse(null))
                .onboardingComplete(onboardingComplete)
                .accessTokenExpiresInSeconds(24 * 60 * 60L)
                .build();
    }
}

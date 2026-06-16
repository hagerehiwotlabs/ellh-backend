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

// ── ADDED IMPORTS FOR LATE REGISTRATION SUPPORT ──
import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.learning.entity.LearnerLanguage;
import com.ellh.learning.repository.LearnerLanguageRepository;
import com.ellh.infrastructure.exception.ResourceNotFoundException;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository                  userRepository;
    private final LearnerProfileRepository        learnerProfileRepository;
    private final PasswordEncoder                  passwordEncoder;
    private final JWTService                       jwtService;
    private final SessionCacheService              sessionCacheService;
    private final AuthenticationManager            authenticationManager;
    
    // ── INJECTED REPOSITORY ──
    private final LanguageRepository languageRepository;
    private final LearnerLanguageRepository        learnerLanguageRepository;

    // ── register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException(
                    "An account with this email address already exists");
        }

        // 1. Save the Base User
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .accountStatus(AccountStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .userType(request.getUserType())
                .build();

        User savedUser = userRepository.save(user);

        boolean isLearner = request.getUserType() == UserType.FOREIGN_LEARNER
                         || request.getUserType() == UserType.BILINGUAL_LEARNER;

        if (isLearner) {
            // 2. Parse onboarding payload
            PathwayType pathway = request.getPathwayType() != null 
                    ? PathwayType.valueOf(request.getPathwayType()) 
                    : PathwayType.FOREIGN_LEARNER;
                    
            CefrLevel cefr = request.getCurrentCefrLevel() != null 
                    ? CefrLevel.valueOf(request.getCurrentCefrLevel().toUpperCase()) 
                    : CefrLevel.A1;
                    
            int goalMinutes = request.getDailyGoalMinutes() != null ? request.getDailyGoalMinutes() : 10;

            // 3. Save the LearnerProfile (Marking Onboarding Complete instantly!)
            LearnerProfile profile = LearnerProfile.builder()
                    .user(savedUser)
                    .pathwayType(pathway)
                    .currentCefrLevel(cefr)
                    .dailyGoalMinutes(goalMinutes)
                    .nativeLanguageId(request.getNativeLanguageId())
                    .targetLanguageId(request.getTargetLanguageId())
                    .onboardingComplete(true) // No separate PUT call needed!
                    .build();
            learnerProfileRepository.save(profile);

            // 4. Create and Save the Active Target Language record
            if (request.getTargetLanguageId() != null) {
                Language targetLanguage = languageRepository.findById(request.getTargetLanguageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Language", request.getTargetLanguageId()));
                        
                LearnerLanguage learnerLanguage = LearnerLanguage.builder()
                        .user(savedUser)
                        .language(targetLanguage)
                        .cefrLevel(cefr)
                        .isActive(true)
                        .build();
                learnerLanguageRepository.save(learnerLanguage);
            }
        }

        return buildAuthResponse(savedUser, isLearner);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt - email: {}, raw password: '{}'", request.getEmail(), request.getPassword());
        userRepository.findByEmail(request.getEmail().toLowerCase())
            .ifPresent(user -> log.info("Stored password hash for {}: {}", request.getEmail(), user.getPasswordHash()));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            userRepository.findByEmail(request.getEmail().toLowerCase())
                    .ifPresent(u -> userRepository.incrementFailedAttempts(u.getId()));
            log.warn("BadCredentialsException for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

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

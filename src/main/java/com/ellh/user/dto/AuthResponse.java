package com.ellh.user.dto;

import com.ellh.user.entity.PathwayType;
import com.ellh.user.entity.UserType;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for POST /api/v1/auth/login and POST /api/v1/auth/refresh.
 * Android stores accessToken and refreshToken in EncryptedSharedPreferences.
 * onboardingComplete tells the Android nav graph whether to show the lesson
 * dashboard or redirect to the onboarding flow.
 */
@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String firstName;
    private UserType userType;
    private PathwayType pathwayType;   // null for ContentAdmin/SystemAdmin
    private boolean onboardingComplete;
    private long accessTokenExpiresInSeconds;
}

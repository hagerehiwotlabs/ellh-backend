package com.ellh.user.controller;

import com.ellh.user.dto.*;
import com.ellh.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 * Section 4.5.5.2 FLOW-01 — Authentication and JWT Lifecycle.
 *
 * All three endpoints are public (no JWT required) — configured in SecurityConfig.
 * Input validation via @Valid + GlobalExceptionHandler (400 on failure).
 *
 * API:
 *   POST /api/v1/auth/register  → 201 AuthResponse
 *   POST /api/v1/auth/login     → 200 AuthResponse
 *   POST /api/v1/auth/refresh   → 200 AuthResponse
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     * Returns 201 with AuthResponse (access + refresh tokens) on success.
     * Returns 400 if request validation fails.
     * Returns 409 if email is already registered.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * Authenticate with email and password.
     * Returns 200 with AuthResponse on success.
     * Returns 401 on invalid credentials or locked account.
     * Rate limited to 10 requests/minute per IP by RateLimitFilter.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchange a valid refresh token for a new access token.
     * Returns 200 with AuthResponse (new accessToken, same refreshToken).
     * Returns 401 if refresh token is expired or invalid.
     * Called silently by Android OkHttp Authenticator on 401 responses.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}

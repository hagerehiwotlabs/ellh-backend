package com.ellh.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown for business-logic authorisation failures (distinct from Spring
 * Security's AccessDeniedException which handles role checks).
 * Maps to HTTP 401. Example: expired refresh token on /api/v1/auth/refresh.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

package com.ellh.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.Map;

/**
 * Standard JSON error envelope returned by GlobalExceptionHandler.
 * All API errors have the same shape — Android client deserialises this.
 *
 * Example:
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/v1/auth/register",
 *   "timestamp": "2024-03-01T10:00:00Z",
 *   "fieldErrors": {"email": "must not be blank"}
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors; // only for 400 validation errors
}

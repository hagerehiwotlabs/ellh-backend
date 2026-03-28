package com.ellh.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by AIServiceGateway when all AI providers (Colab primary, Colab
 * overflow, Hugging Face) have failed or the circuit breaker is OPEN.
 * Maps to HTTP 503. Android displays AIUnavailableBottomSheet on this response.
 * Section 4.5.4.4 — circuit breaker behaviour.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AIServiceUnavailableException extends RuntimeException {
    public AIServiceUnavailableException(String message) {
        super(message);
    }
}

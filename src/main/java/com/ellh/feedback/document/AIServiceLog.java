package com.ellh.feedback.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * Per-invocation audit log for every AI service call.
 * Section 4.5.2.4 — ai_service_logs collection specification.
 * Section 4.5.1 — Feedback & Audit Domain (AIServiceLog @document).
 *
 * Written by AIServiceGateway (Sprint 5) after EVERY call — success or failure.
 * Semi-structured: requestPayload and responsePayload are Map<String,Object>
 * because Colab and Hugging Face have different response schemas.
 *
 * TTL index: created_at with expireAfterSeconds=7776000 (90 days).
 * Applied via create_indexes.js — NOT via @Document annotation because
 * Atlas M0 does not support TTL index creation from the driver on startup.
 *
 * Audio data is NEVER logged here (Section 4.5.5.5 — Audio Data Privacy
 * enforcement point). Only metadata (duration_ms, audio_hash) is stored.
 */
@Document(collection = "ai_service_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIServiceLog {

    @Id
    private String id;

    /** PRONUNCIATION | TRANSLATION */
    @Field("service_type")
    private String serviceType;

    /** COLAB_PRIMARY | COLAB_OVERFLOW | HUGGING_FACE | MOCK */
    @Field("provider")
    private String provider;

    @Field("user_id")
    private Long userId;

    /**
     * Sanitised request metadata — NEVER include raw audio bytes.
     * For pronunciation: {languageCode, exerciseId, audioDurationMs, audioHash}
     * For translation:   {sourceLanguage, targetLanguage, textLength}
     */
    @Field("request_metadata")
    private Map<String, Object> requestMetadata;

    /**
     * AI response summary.
     * For pronunciation: {confidenceScore, feedbackText}
     * For translation:   {targetText, cached}
     */
    @Field("response_summary")
    private Map<String, Object> responseSummary;

    /** End-to-end response time in milliseconds. */
    @Field("processing_time_ms")
    private Integer processingTimeMs;

    /** SUCCESS | TIMEOUT | CIRCUIT_OPEN | ERROR */
    @Field("status")
    private String status;

    /** Error message if status != SUCCESS. Null on success. */
    @Field("error_message")
    private String errorMessage;

    /**
     * TTL index is on this field (90-day auto-expiry).
     * Must be Instant / Date for MongoDB TTL to work correctly.
     */
    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}

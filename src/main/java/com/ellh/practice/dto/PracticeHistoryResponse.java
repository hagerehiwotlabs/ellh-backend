package com.ellh.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Practice history item response DTO.
 * Response from GET /api/v1/practice/history?languageId={id}&limit={n}
 *
 * Represents a completed practice session in user's history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("mode_id")
    private Long modeId;

    @JsonProperty("mode_name")
    private String modeName;

    @JsonProperty("score")
    private Integer score;  // percentage 0-100

    @JsonProperty("total_questions")
    private Integer totalQuestions;

    @JsonProperty("completed_at")
    private Long completedAt;  // Unix timestamp in milliseconds

    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @JsonProperty("difficulty")
    private String difficulty;
}

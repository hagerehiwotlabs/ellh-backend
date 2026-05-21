package com.ellh.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Practice result submission response DTO.
 * Response from POST /api/v1/practice/sessions/{sessionId}/results
 *
 * Contains results and feedback for the completed practice session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeResultResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("correct_answers")
    private Integer correctAnswers;

    @JsonProperty("total_questions")
    private Integer totalQuestions;

    @JsonProperty("score")
    private Integer score;  // percentage 0-100

    @JsonProperty("xp_earned")
    private Integer xpEarned;

    @JsonProperty("passed_session")
    private Boolean passedSession;

    @JsonProperty("feedback")
    private String feedback;

    @JsonProperty("completed_at")
    private Long completedAt;  // Unix timestamp in milliseconds
}

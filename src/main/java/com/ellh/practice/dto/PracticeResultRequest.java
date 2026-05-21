package com.ellh.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Practice result submission request DTO.
 * Request body for POST /api/v1/practice/sessions/{sessionId}/results
 *
 * Contains user's answers to practice questions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeResultRequest {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("answers")
    private java.util.List<AnswerItem> answers;

    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    /**
     * Individual answer submission
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerItem {
        @JsonProperty("question_id")
        private String questionId;

        @JsonProperty("user_answer")
        private String userAnswer;
    }
}

package com.ellh.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Practice session start response DTO.
 * Response from POST /api/v1/practice/sessions
 *
 * Contains the new session ID and initial questions for the practice session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSessionResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("mode_id")
    private Long modeId;

    @JsonProperty("mode_name")
    private String modeName;

    @JsonProperty("total_questions")
    private Integer totalQuestions;

    @JsonProperty("questions")
    private List<PracticeQuestionResponse> questions;

    /**
     * Individual practice question in a session
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PracticeQuestionResponse {
        @JsonProperty("question_id")
        private String questionId;

        @JsonProperty("question_index")
        private Integer questionIndex;

        @JsonProperty("question_text")
        private String questionText;

        @JsonProperty("type")
        private String type;  // MULTIPLE_CHOICE, FILL_BLANK, MATCHING, etc.

        @JsonProperty("options")
        private List<String> options;  // For multiple choice questions

        @JsonProperty("image_url")
        private String imageUrl;  // Optional image for visual questions
    }
}

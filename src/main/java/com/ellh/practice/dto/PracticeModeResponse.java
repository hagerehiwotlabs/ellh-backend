package com.ellh.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Practice mode response DTO.
 * Response from GET /api/v1/practice/modes?languageId={id}
 *
 * Represents an available practice mode (exercise type) for a language.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeModeResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;  // VOCABULARY, LISTENING, SPEAKING, GRAMMAR, etc.

    @JsonProperty("question_count")
    private Integer questionCount;

    @JsonProperty("estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @JsonProperty("difficulty")
    private String difficulty;  // BEGINNER, INTERMEDIATE, ADVANCED
}

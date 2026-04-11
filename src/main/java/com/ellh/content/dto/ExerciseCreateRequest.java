package com.ellh.content.dto;

import com.ellh.content.entity.ExerciseType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** Request body for POST /api/v1/lessons/{id}/exercises (CONTENT_ADMIN only). */
@Data
public class ExerciseCreateRequest {

    @NotNull(message = "Exercise type is required")
    private ExerciseType exerciseType;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    private List<Map<String, Object>> options; // required for MULTIPLE_CHOICE

    private String hintText;
    private String audioUrl;
    private String imageUrl;

    @NotNull(message = "Order index is required")
    @Min(value = 1)
    private Integer orderIndex;

    @Min(value = 0)
    private int points = 10;
}

package com.ellh.content.dto;

import com.ellh.content.entity.ExerciseType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Response DTO for exercise endpoints. */
@Getter
@Builder
public class ExerciseResponse {
    private Long id;
    private Long lessonId;
    private ExerciseType exerciseType;
    private String questionText;
    private String correctAnswer;
    private List<Map<String, Object>> options;
    private String hintText;
    private String audioUrl;
    private String imageUrl;
    private int orderIndex;
    private int points;
}

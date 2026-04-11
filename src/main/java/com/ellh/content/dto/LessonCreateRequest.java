package com.ellh.content.dto;

import com.ellh.infrastructure.validation.ValidCefrLevel;
import com.ellh.infrastructure.validation.ValidIso639Code;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/v1/lessons (CONTENT_ADMIN only).
 * Creates both the PostgreSQL lessons row and the MongoDB lesson_content document.
 */
@Data
public class LessonCreateRequest {

    @NotNull(message = "Language ID is required")
    private Long languageId;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    private String description;

    @NotBlank(message = "CEFR level is required")
    @ValidCefrLevel
    private String cefrLevel;

    @NotNull(message = "Order index is required")
    @Min(value = 1, message = "Order index must be at least 1")
    private Integer orderIndex;

    @Min(value = 0, message = "XP reward must be non-negative")
    private int xpReward = 25;

    private List<Long> prerequisites;

    @Min(value = 1)
    private int estimatedMinutes = 8;

    // ── MongoDB content fields ──────────────────────────────────────────────
    @NotNull(message = "Lesson exercises are required")
    @Size(min = 1, message = "A lesson must have at least one exercise")
    private List<Map<String, Object>> exercises;

    private List<Map<String, Object>> vocabulary;

    private List<String> culturalNotes;
}

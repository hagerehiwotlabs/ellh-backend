package com.ellh.content.dto;

import com.ellh.infrastructure.validation.ValidCefrLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for PUT /api/v1/lessons/{id} (CONTENT_ADMIN only).
 * All fields are optional — only non-null fields are applied.
 * Triggers: cache eviction + ContentUpdateLog write + MongoDB version_stamp increment.
 */
@Data
public class LessonUpdateRequest {

    @Size(max = 255)
    private String title;

    private String description;

    @ValidCefrLevel
    private String cefrLevel;

    private Integer orderIndex;
    private Integer xpReward;
    private Integer estimatedMinutes;
    private List<Long> prerequisites;
    private Boolean active;

    // MongoDB content update fields (all optional)
    private List<Map<String, Object>> exercises;
    private List<Map<String, Object>> vocabulary;
    private List<String> culturalNotes;
}

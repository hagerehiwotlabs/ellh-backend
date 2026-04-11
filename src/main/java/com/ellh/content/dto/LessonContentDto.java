package com.ellh.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Embedded content block within LessonResponse.
 * Populated only on GET /api/v1/lessons/{id} (single lesson fetch).
 * Contains the full MongoDB document data.
 */
@Getter
@Builder
public class LessonContentDto {
    private List<Map<String, Object>> exercises;
    private List<Map<String, Object>> vocabulary;
    private List<String> culturalNotes;
    private Long versionStamp;
}

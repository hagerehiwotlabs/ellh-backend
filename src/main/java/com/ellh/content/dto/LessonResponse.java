package com.ellh.content.dto;

import com.ellh.user.entity.CefrLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for GET /api/v1/lessons/{id} and GET /api/v1/lessons (list).
 *
 * The content field is only populated on single-lesson fetch (not list).
 * On list fetch, content is null — Android only downloads full content
 * when the user taps a specific lesson card (lazy loading).
 *
 * versionStamp is included so Android can compare against the locally
 * cached version_stamp in downloaded_content SQLite table.
 */
@Getter
@Builder
public class LessonResponse {
    private Long id;
    private Long languageId;
    private String languageCode;   // ISO 639-3
    private String title;
    private String description;
    private CefrLevel cefrLevel;
    private int orderIndex;
    private int xpReward;
    private String contentId;      // MongoDB ObjectId — null until bridge set
    private List<Long> prerequisites;
    private int estimatedMinutes;
    private Long versionStamp;     // from MongoDB LessonContent
    private LessonContentDto content; // null on list requests; populated on single fetch
    private Instant createdAt;
    private Instant updatedAt;
}

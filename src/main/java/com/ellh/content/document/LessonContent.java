package com.ellh.content.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Full lesson content document stored in MongoDB lesson_content collection.
 * Section 4.5.2.4 — lesson_content collection specification.
 *
 * The _id (MongoDB ObjectId) is referenced by lessons.content_id in PostgreSQL.
 * That VARCHAR(24) column is the cross-store bridge (Section 4.5.2.6).
 *
 * version_stamp is incremented on every update and used by:
 *   - CacheEvictionService to invalidate Redis lesson cache
 *   - Android downloaded_content table to detect stale offline content
 *
 * Compound index: (language_code, cefr_level, version_stamp) — applied via
 * scripts/mongodb/create_indexes.js (not via @CompoundIndex because Atlas M0
 * does not allow background index creation from the driver on startup).
 */
@Document(collection = "lesson_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonContent {

    @Id
    private String id; // MongoDB ObjectId as hex string

    @Field("lesson_id")
    private Long lessonId; // PostgreSQL lessons.id back-reference

    @Field("language_code")
    private String languageCode; // ISO 639-3

    @Field("cefr_level")
    private String cefrLevel; // A1 | A2 | B1

    @Field("title")
    private String title;

    /**
     * Rich exercise content not suitable for relational storage.
     * Each element is a map representing one exercise block — flexible
     * schema allows audio references, image URLs, option arrays, etc.
     */
    @Field("exercises")
    private List<Map<String, Object>> exercises;

    /**
     * Vocabulary items introduced in this lesson.
     * Structure: [{word, translation, transliteration, audioUrl}]
     */
    @Field("vocabulary")
    private List<Map<String, Object>> vocabulary;

    /**
     * Cultural notes and grammar explanations for this lesson.
     * Free-form rich text — stored in MongoDB because structure varies per lesson.
     */
    @Field("cultural_notes")
    private List<String> culturalNotes;

    /**
     * Monotonically incrementing version — updated on every content edit.
     * Android clients compare stored version with server version to detect
     * stale cached content (Section 4.5.5.4 SYNC-01).
     */
    @Field("version_stamp")
    @Builder.Default
    private Long versionStamp = 1L;

    @Field("is_active")
    @Builder.Default
    private boolean active = true;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /** Called by LessonService on every content update. */
    public void incrementVersion() {
        this.versionStamp++;
        this.updatedAt = Instant.now();
    }
}

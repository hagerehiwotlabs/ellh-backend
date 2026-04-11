package com.ellh.content.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standalone vocabulary sets not tied to a specific lesson.
 * Section 4.5.2.4 — vocabulary_sets collection specification.
 *
 * Used by the Practice tab (nav_practice — Sprint 4) for standalone
 * vocabulary drilling outside the lesson flow.
 *
 * Index: (language_code, cefr_level, category) — see create_indexes.js
 */
@Document(collection = "vocabulary_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularySet {

    @Id
    private String id;

    @Field("language_code")
    private String languageCode; // ISO 639-3

    @Field("cefr_level")
    private String cefrLevel; // A1 | A2 | B1

    /**
     * Category: GREETINGS | NUMBERS | FOOD | FAMILY | COLOURS | TRAVEL | BODY | TIME
     * Used for filtering in the Practice tab UI.
     */
    @Field("category")
    private String category;

    @Field("title")
    private String title;

    /**
     * Vocabulary entries. Structure:
     * [{word, translation, transliteration, audioUrl, imageUrl, exampleSentence}]
     */
    @Field("entries")
    private List<Map<String, Object>> entries;

    @Field("version_stamp")
    @Builder.Default
    private Long versionStamp = 1L;

    @Field("is_active")
    @Builder.Default
    private boolean active = true;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}

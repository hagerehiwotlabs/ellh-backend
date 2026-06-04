package com.ellh.learning.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Data
@Document(collection = "contrastive_rules")
public class ContrastiveRule {
    @Id
    private String id;

    @Field("source_language")
    private String sourceLanguage;

    @Field("target_language")
    private String targetLanguage;

    @Field("applicable_lessons")
    private List<Long> applicableLessons;

    @Field("rule_category")
    private String ruleCategory;

    @Field("rule_title")               // <-- note the underscore in MongoDB, but
    private String ruleTitle;          //     Lombok generates getRuleTitle()

    @Field("contrast_description")
    private String contrastDescription; // Lombok generates getContrastDescription()

    @Field("version_stamp")
    private String versionStamp;

    @Field("is_active")
    private boolean active = true;
}


// package com.ellh.learning.document;

// import lombok.*;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.mongodb.core.mapping.Document;
// import org.springframework.data.mongodb.core.mapping.Field;

// import java.time.Instant;
// import java.util.List;

// /**
//  * A single contrastive linguistic rule for the Leveraged Learning pathway.
//  * Section 4.5.2.4 — contrastive_rules collection specification.
//  * Section 4.5.1 — Learning Engine Domain (ContrastiveRule @document).
//  *
//  * Rules contrast two Ethiopian languages (or an Ethiopian language vs English)
//  * to surface phonological, grammatical, and lexical similarities and differences.
//  * ContrastiveAnalysisEngine (Sprint 6) queries this collection by language pair
//  * and lessonId.
//  *
//  * Index: (source_language, target_language, applicable_lessons, is_active)
//  * — see create_indexes.js
//  *
//  * version_stamp: compared against Android cached_contrastive_notes.version_stamp
//  * to detect staleness (Section 4.5.5.4 SYNC-01).
//  */
// @Document(collection = "contrastive_rules")
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class ContrastiveRule {

//     @Id
//     private String id;

//     @Field("source_language")
//     private String sourceLanguage; // ISO 639-3

//     @Field("target_language")
//     private String targetLanguage; // ISO 639-3

//     /**
//      * Category: PHONOLOGY | GRAMMAR | VOCABULARY | SCRIPT | SYNTAX
//      * Displayed as a chip on the SCR-09 tip overlay (Section 4.5.3.4).
//      */
//     @Field("rule_category")
//     private String ruleCategory;

//     /** Short headline for the tip overlay (≤ 60 chars). */
//     @Field("tip_title")
//     private String tipTitle;

//     /** Full explanation of the contrastive relationship. */
//     @Field("description")
//     private String description;

//     /**
//      * Example in the source language with transliteration.
//      * Structure: {text, transliteration, translation}
//      */
//     @Field("source_example")
//     private java.util.Map<String, String> sourceExample;

//     /**
//      * Corresponding example in the target language.
//      * Structure: {text, transliteration, translation}
//      */
//     @Field("target_example")
//     private java.util.Map<String, String> targetExample;

//     /**
//      * List of PostgreSQL lesson IDs where this rule applies.
//      * ContrastiveAnalysisEngine filters by: applicable_lessons contains lessonId.
//      * Section 4.5.1 — ContrastiveRule.findByLanguagePair().
//      */
//     @Field("applicable_lessons")
//     private List<Long> applicableLessons;

//     @Field("version_stamp")
//     @Builder.Default
//     private Long versionStamp = 1L;

//     @Field("is_active")
//     @Builder.Default
//     private boolean active = true;

//     @Field("created_at")
//     @Builder.Default
//     private Instant createdAt = Instant.now();

//     @Field("updated_at")
//     @Builder.Default
//     private Instant updatedAt = Instant.now();

//     public void incrementVersion() {
//         this.versionStamp++;
//         this.updatedAt = Instant.now();
//     }
// }

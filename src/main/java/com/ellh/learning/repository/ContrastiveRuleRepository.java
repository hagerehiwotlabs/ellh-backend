package com.ellh.learning.repository;

import com.ellh.learning.document.ContrastiveRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the contrastive_rules collection.
 * Section 4.5.1 — ContrastiveRule.findByLanguagePair().
 *
 * The compound index on (source_language, target_language, applicable_lessons)
 * makes the primary query performant even on large rule sets.
 */
@Repository
public interface ContrastiveRuleRepository extends MongoRepository<ContrastiveRule, String> {

    /**
     * Primary query for ContrastiveAnalysisEngine (Sprint 6).
     * Finds all active rules for a language pair that apply to a specific lesson.
     * Uses the compound index on (source_language, target_language, applicable_lessons).
     */
    @Query("{ 'source_language': ?0, 'target_language': ?1, " +
           "'applicable_lessons': ?2, 'is_active': true }")
    List<ContrastiveRule> findByLanguagePairAndLesson(
            String sourceLanguage, String targetLanguage, Long lessonId);

    /**
     * Returns all active rules for a language pair (used by admin content view).
     */
    List<ContrastiveRule> findBySourceLanguageAndTargetLanguageAndActiveTrue(
            String sourceLanguage, String targetLanguage);
}

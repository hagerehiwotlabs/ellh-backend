package com.ellh.content.repository;

import com.ellh.content.document.LessonContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Data MongoDB repository for the lesson_content collection.
 * Section 4.5.2.4 — lesson_content collection.
 *
 * The primary lookup is by lessonId (the PostgreSQL FK).
 * The compound index on (language_code, cefr_level, version_stamp) supports
 * the lesson listing queries used by the lesson dashboard.
 */
@Repository
public interface LessonContentRepository extends MongoRepository<LessonContent, String> {

    Optional<LessonContent> findByLessonId(Long lessonId);

    List<LessonContent> findByLanguageCodeAndCefrLevel(
            String languageCode, String cefrLevel);

    /** Used by the content_id bridge to verify MongoDB document was saved. */
    boolean existsByLessonId(Long lessonId);

    /**
     * Soft delete — mark is_active=false rather than physical deletion.
     * Physical deletion is handled by the scheduled content cleanup job (Sprint 9).
     */
    @Query("{ 'lesson_id': ?0 }")
    Optional<LessonContent> findByLessonIdForUpdate(Long lessonId);

    /** 
     * HIGH PERFORMANCE PROJECTION: 
     * Only fetches the _id field over the network. Prevents JVM OOM crashes.
     */
    @Query(value = "{}", fields = "{ '_id' : 1 }")
    List<LessonContent> findAllJustIds();

    default List<String> findAllIds() {
        return findAllJustIds().stream().map(LessonContent::getId).collect(Collectors.toList());
    }
}

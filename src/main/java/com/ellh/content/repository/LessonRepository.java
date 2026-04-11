package com.ellh.content.repository;

import com.ellh.content.entity.Lesson;
import com.ellh.user.entity.CefrLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Section 4.5.2.3 indexes used by this repository:
 *   idx_lessons_lang_cefr  — findByLanguageIdAndCefrLevel (lesson dashboard)
 *   idx_lessons_lang_order — findByLanguageIdOrderByOrderIndex (lesson sequence)
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByLanguageIdAndCefrLevelAndActiveTrue(
            Long languageId, CefrLevel cefrLevel);

    List<Lesson> findByLanguageIdAndActiveTrueOrderByOrderIndex(Long languageId);

    Optional<Lesson> findByIdAndActiveTrue(Long id);

    /** Used to verify no content_id collision before bridge update. */
    boolean existsByContentId(String contentId);

    /**
     * Atomic content_id bridge update — called AFTER MongoDB insert succeeds.
     * Section 4.5.2.6 — Cross-Store Consistency compensating transaction.
     */
    @Modifying
    @Query("UPDATE Lesson l SET l.contentId = :contentId WHERE l.id = :lessonId")
    void updateContentId(
            @Param("lessonId") Long lessonId,
            @Param("contentId") String contentId);

    /**
     * Soft delete — sets is_active=false.
     * Hard deletion deferred to Sprint 9 scheduled cleanup job.
     */
    @Modifying
    @Query("UPDATE Lesson l SET l.active = false WHERE l.id = :id")
    void softDelete(@Param("id") Long id);

    /** Count active lessons per language for admin dashboard stats. */
    long countByLanguageIdAndActiveTrue(Long languageId);
}

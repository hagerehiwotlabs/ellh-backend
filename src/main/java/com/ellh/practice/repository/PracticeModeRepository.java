package com.ellh.practice.repository;

import com.ellh.practice.entity.PracticeMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PracticeMode entity.
 * Handles CRUD operations and queries for practice modes.
 */
@Repository
public interface PracticeModeRepository extends JpaRepository<PracticeMode, Long> {

    /**
     * Find all active practice modes for a language, ordered by difficulty.
     */
    @Query("SELECT pm FROM PracticeMode pm WHERE pm.language.id = :languageId AND pm.isActive = true ORDER BY pm.difficulty")
    List<PracticeMode> findByLanguageIdOrderByDifficulty(@Param("languageId") Long languageId);

    /**
     * Find active practice modes by language and difficulty level.
     */
    @Query("SELECT pm FROM PracticeMode pm WHERE pm.language.id = :languageId AND pm.difficulty = :difficulty AND pm.isActive = true")
    List<PracticeMode> findByLanguageIdAndDifficulty(@Param("languageId") Long languageId, @Param("difficulty") String difficulty);
}

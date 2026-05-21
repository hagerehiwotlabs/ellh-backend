package com.ellh.practice.repository;

import com.ellh.practice.entity.PracticeAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PracticeAnswer entity.
 * Handles CRUD operations and queries for practice answers.
 */
@Repository
public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, Long> {

    /**
     * Find all answers for a practice session.
     */
    @Query("SELECT pa FROM PracticeAnswer pa WHERE pa.session.id = :sessionId ORDER BY pa.createdAt")
    List<PracticeAnswer> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Count correct answers in a session.
     */
    @Query("SELECT COUNT(pa) FROM PracticeAnswer pa WHERE pa.session.id = :sessionId AND pa.isCorrect = true")
    Long countCorrectAnswers(@Param("sessionId") Long sessionId);
}

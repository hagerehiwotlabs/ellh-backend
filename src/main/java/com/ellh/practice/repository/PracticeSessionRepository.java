package com.ellh.practice.repository;

import com.ellh.practice.entity.PracticeSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PracticeSession entity.
 * Handles CRUD operations and queries for practice sessions.
 */
@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    /**
     * Find session by session ID (UUID string).
     */
    Optional<PracticeSession> findBySessionId(String sessionId);

    /**
     * Find completed sessions for a user, ordered by completion date descending.
     * Used for history endpoint.
     */
    @Query("SELECT ps FROM PracticeSession ps WHERE ps.user.id = :userId AND ps.status = 'COMPLETED' AND ps.mode.language.id = :languageId ORDER BY ps.completedAt DESC")
    List<PracticeSession> findCompletedSessionsByUserAndLanguage(
            @Param("userId") Long userId,
            @Param("languageId") Long languageId,
            PageRequest pageRequest);

    /**
     * Find active session for a user and mode.
     */
    @Query("SELECT ps FROM PracticeSession ps WHERE ps.user.id = :userId AND ps.mode.id = :modeId AND ps.status = 'ACTIVE'")
    Optional<PracticeSession> findActiveSessionByUserAndMode(@Param("userId") Long userId, @Param("modeId") Long modeId);
}

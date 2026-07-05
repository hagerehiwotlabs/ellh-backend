package com.ellh.learning.repository;

import com.ellh.learning.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId " +
           "AND up.lesson.id = :lessonId " +
           "AND (:exerciseId IS NULL AND up.exercise IS NULL OR up.exercise.id = :exerciseId)")
    Optional<UserProgress> findProgress(@Param("userId") Long userId, @Param("lessonId") Long lessonId, @Param("exerciseId") Long exerciseId);

    List<UserProgress> findByUserId(Long userId);

    // ── Gamification Evaluation Queries ──

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.exercise IS NULL AND up.status = 'COMPLETED'")
    long countCompletedLessonsForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(up.score), 0) FROM UserProgress up WHERE up.user.id = :userId AND up.exercise IS NOT NULL")
    int findMaxExerciseScoreForUser(@Param("userId") Long userId);

    // ── HIGH PERFORMANCE AGGREGATE QUERY (Prevents N+1) ──
    @Query("SELECT e.exerciseType, AVG(up.score), SUM(up.attempts) " +
           "FROM UserProgress up JOIN up.exercise e " +
           "WHERE up.user.id = :userId AND e IS NOT NULL " +
           "GROUP BY e.exerciseType")
    List<Object[]> getAggregatedSkillBreakdown(@Param("userId") Long userId);
}

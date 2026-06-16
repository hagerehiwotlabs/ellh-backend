package com.ellh.learning.repository;

import com.ellh.learning.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId " +
           "AND up.lesson.id = :lessonId " +
           "AND (:exerciseId IS NULL AND up.exercise IS NULL OR up.exercise.id = :exerciseId)")
    Optional<UserProgress> findProgress(
            @Param("userId") Long userId,
            @Param("lessonId") Long lessonId,
            @Param("exerciseId") Long exerciseId);

     /**
     * Finds all progress records (lessons and exercises) completed by a user.
     * Used to calculate daily analytics and skill breakdown percentages.
     */
    java.util.List<UserProgress> findByUserId(Long userId);
}

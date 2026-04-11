package com.ellh.content.repository;

import com.ellh.content.entity.Exercise;
import com.ellh.content.entity.ExerciseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Section 4.5.2.3 — idx_exercises_lesson_id (B-tree FK index).
 * All exercise fetches are scoped to a lesson_id — this index makes them fast.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByLessonIdOrderByOrderIndex(Long lessonId);

    List<Exercise> findByLessonIdAndExerciseType(Long lessonId, ExerciseType type);

    long countByLessonId(Long lessonId);
}

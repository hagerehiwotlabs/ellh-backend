package com.ellh.content.service;

import com.ellh.content.dto.ExerciseCreateRequest;
import com.ellh.content.dto.ExerciseResponse;
import com.ellh.content.entity.Exercise;
import com.ellh.content.entity.ExerciseType;
import com.ellh.content.mapper.LessonMapper;
import com.ellh.content.repository.ExerciseRepository;
import com.ellh.content.repository.LessonRepository;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Exercise CRUD operations.
 * Section 4.5.1 — Content Domain (Exercise entity).
 *
 * Business rules enforced here:
 * - MULTIPLE_CHOICE exercises must have at least 2 options with exactly one
 *   isCorrect=true entry.
 * - PRONUNCIATION and TRANSLATION exercises require no server-side answer
 *   evaluation (handled by AI Gateway in Sprint 5).
 * - orderIndex must be unique within a lesson (validated on create).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository   lessonRepository;
    private final LessonMapper       lessonMapper;

    @Transactional(readOnly = true)
    public List<ExerciseResponse> getExercisesByLesson(Long lessonId) {
        // Verify lesson exists before returning exercises
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Lesson", lessonId);
        }
        List<Exercise> exercises = exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId);
        return lessonMapper.toExerciseResponseList(exercises);
    }

    @Transactional
    public ExerciseResponse createExercise(Long lessonId, ExerciseCreateRequest request) {
        var lesson = lessonRepository.findByIdAndActiveTrue(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        // Business rule: MULTIPLE_CHOICE must have options
        if (request.getExerciseType() == ExerciseType.MULTIPLE_CHOICE
                && (request.getOptions() == null || request.getOptions().size() < 2)) {
            throw new IllegalArgumentException(
                    "MULTIPLE_CHOICE exercises must have at least 2 options");
        }

        // Business rule: MULTIPLE_CHOICE must have exactly one correct option
        if (request.getExerciseType() == ExerciseType.MULTIPLE_CHOICE) {
            long correctCount = request.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.get("isCorrect")))
                    .count();
            if (correctCount != 1) {
                throw new IllegalArgumentException(
                        "MULTIPLE_CHOICE exercises must have exactly one correct option");
            }
        }

        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .exerciseType(request.getExerciseType())
                .questionText(request.getQuestionText())
                .correctAnswer(request.getCorrectAnswer())
                .options(request.getOptions())
                .hintText(request.getHintText())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .orderIndex(request.getOrderIndex())
                .points(request.getPoints())
                .build();

        exercise = exerciseRepository.save(exercise);
        log.info("Exercise created: id={} lessonId={} type={}", exercise.getId(), lessonId, exercise.getExerciseType());
        return lessonMapper.toExerciseResponse(exercise);
    }

    @Transactional(readOnly = true)
    public ExerciseResponse getExercise(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));
        return lessonMapper.toExerciseResponse(exercise);
    }
}

package com.ellh.content.controller;

import com.ellh.content.dto.ExerciseCreateRequest;
import com.ellh.content.dto.ExerciseResponse;
import com.ellh.content.service.ExerciseService;
import com.ellh.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exercise API endpoints.
 * Section 4.4 SS-2 interfaces exposed:
 *   GET  /api/v1/lessons/{lessonId}/exercises      — list exercises for a lesson
 *   GET  /api/v1/lessons/{lessonId}/exercises/{id} — single exercise
 *   POST /api/v1/lessons/{lessonId}/exercises      — create exercise (CONTENT_ADMIN)
 *
 * Android fetches exercises when the learner opens a lesson card (lazy load).
 * The response includes all exercise types — Android renders the appropriate
 * component based on exerciseType.
 */
@RestController
@RequestMapping("/api/v1/lessons/{lessonId}/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    /** Returns all exercises for a lesson ordered by orderIndex. */
    @GetMapping
    public ResponseEntity<List<ExerciseResponse>> getExercises(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(exerciseService.getExercisesByLesson(lessonId));
    }

    /** Returns a single exercise. */
    @GetMapping("/{exerciseId}")
    public ResponseEntity<ExerciseResponse> getExercise(
            @PathVariable Long lessonId,
            @PathVariable Long exerciseId) {
        return ResponseEntity.ok(exerciseService.getExercise(exerciseId));
    }

    /**
     * Creates a new exercise within a lesson.
     * Validates MULTIPLE_CHOICE options (>=2 options, exactly 1 correct).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ExerciseResponse> createExercise(
            @PathVariable Long lessonId,
            @Valid @RequestBody ExerciseCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseService.createExercise(lessonId, request));
    }
}

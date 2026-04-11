package com.ellh.content.controller;

import com.ellh.content.dto.*;
import com.ellh.content.service.LessonService;
import com.ellh.user.entity.CefrLevel;
import com.ellh.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lesson API endpoints.
 * Section 4.4 SS-2 interfaces exposed:
 *   GET  /api/v1/lessons/{id}           — single lesson with full content
 *   GET  /api/v1/lessons?languageId=&cefrLevel= — lesson list (dashboard)
 *   POST /api/v1/lessons                — create lesson (CONTENT_ADMIN only)
 *   PUT  /api/v1/lessons/{id}           — update lesson (CONTENT_ADMIN only)
 *   DELETE /api/v1/lessons/{id}         — soft delete (CONTENT_ADMIN only)
 *
 * Write endpoints are additionally protected by @PreAuthorize beyond
 * the SecurityConfig URL-level role check — defence in depth.
 */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    /**
     * Single lesson fetch with full MongoDB content.
     * Implements FLOW-02 (Section 4.5.5.2) — cache hit/miss paths in LessonService.
     * All authenticated users can access any active lesson.
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getLesson(lessonId));
    }

    /**
     * Lesson list — metadata only, no MongoDB content (lightweight).
     * Used by lesson dashboard (SCR-05/06).
     * Both languageId and cefrLevel are optional filters.
     */
    @GetMapping
    public ResponseEntity<List<LessonResponse>> getLessons(
            @RequestParam Long languageId,
            @RequestParam(required = false) String cefrLevel) {

        List<LessonResponse> lessons = cefrLevel != null
                ? lessonService.getLessonsByLanguageAndLevel(
                        languageId, CefrLevel.valueOf(cefrLevel.toUpperCase()))
                : lessonService.getLessonsByLanguage(languageId);

        return ResponseEntity.ok(lessons);
    }

    /**
     * Create a new lesson — CONTENT_ADMIN or SYSTEM_ADMIN only.
     * Triggers content_id bridge (PostgreSQL + MongoDB dual insert).
     * Returns 201 with the created lesson including its MongoDB content_id.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody LessonCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(request, currentUser));
    }

    /**
     * Update an existing lesson — CONTENT_ADMIN or SYSTEM_ADMIN only.
     * All fields in LessonUpdateRequest are optional (partial update).
     * Triggers: cache eviction + MongoDB version_stamp increment.
     */
    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, request, currentUser));
    }

    /**
     * Soft delete a lesson — CONTENT_ADMIN or SYSTEM_ADMIN only.
     * Sets is_active=false in PostgreSQL and MongoDB. Hard deletion via Sprint 9 job.
     */
    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User currentUser) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }
}

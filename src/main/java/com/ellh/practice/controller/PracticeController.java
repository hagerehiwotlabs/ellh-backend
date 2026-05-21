package com.ellh.practice.controller;

import com.ellh.practice.dto.*;
import com.ellh.practice.service.PracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Practice API endpoints.
 *
 * Endpoints:
 *  - GET /api/v1/practice/modes?languageId={id} — list available practice modes
 *  - GET /api/v1/practice/history?languageId={id}&limit={n} — user's practice history
 *  - POST /api/v1/practice/sessions — start a new practice session
 *  - POST /api/v1/practice/sessions/{sessionId}/results — submit session results
 *
 * All endpoints require JWT authentication.
 *
 * Section 4.5.5.2 — Practice flow and session management.
 * Cache: practice modes (24h), history (1h).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * Get available practice modes for a language.
     *
     * Query Parameters:
     *  - languageId (required): The language ID
     *
     * Returns list of PracticeModeResponse with metadata about available practice modes.
     *
     * HTTP 200: Success
     * HTTP 400: Invalid or missing languageId
     * HTTP 404: Language not found
     * HTTP 401: Unauthorized
     */
    @GetMapping("/modes")
    public ResponseEntity<List<PracticeModeResponse>> getPracticeModes(
            @RequestParam(name = "languageId", required = true) Long languageId) {
        
        log.debug("Fetching practice modes for languageId: {}", languageId);
        List<PracticeModeResponse> modes = practiceService.getPracticeModes(languageId);
        
        return ResponseEntity.ok(modes);
    }

    /**
     * Get user's practice history for a language.
     *
     * Query Parameters:
     *  - languageId (required): The language ID
     *  - limit (optional): Maximum number of items to return (default: 10, max: 100)
     *
     * Returns list of PracticeHistoryResponse ordered by completion date descending.
     *
     * HTTP 200: Success
     * HTTP 400: Invalid parameters
     * HTTP 404: Language not found
     * HTTP 401: Unauthorized
     */
    @GetMapping("/history")
    public ResponseEntity<List<PracticeHistoryResponse>> getPracticeHistory(
            @RequestParam(name = "languageId", required = true) Long languageId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        
        log.debug("Fetching practice history for languageId: {}, limit: {}", languageId, limit);
        List<PracticeHistoryResponse> history = practiceService.getPracticeHistory(languageId, limit);
        
        return ResponseEntity.ok(history);
    }

    /**
     * Start a new practice session.
     *
     * Request Body:
     *  - practiceModeId (required): The practice mode ID to start
     *
     * Returns PracticeSessionResponse with session ID and initial questions.
     *
     * HTTP 201: Session created
     * HTTP 400: Invalid request body
     * HTTP 404: Practice mode not found
     * HTTP 401: Unauthorized
     */
    @PostMapping("/sessions")
    public ResponseEntity<PracticeSessionResponse> startPracticeSession(
            @RequestBody Long practiceModeId) {
        
        log.debug("Starting practice session for modeId: {}", practiceModeId);
        PracticeSessionResponse session = practiceService.startPracticeSession(practiceModeId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Submit practice session results.
     *
     * Path Parameters:
     *  - sessionId (required): The session ID to submit results for
     *
     * Request Body:
     *  - sessionId: Session ID (for validation)
     *  - answers: List of user answers with question IDs
     *  - durationSeconds: How long the session took
     *
     * Returns PracticeResultResponse with score, XP earned, and feedback.
     *
     * HTTP 200: Results processed
     * HTTP 400: Invalid request body
     * HTTP 404: Session not found
     * HTTP 401: Unauthorized
     */
    @PostMapping("/sessions/{sessionId}/results")
    public ResponseEntity<PracticeResultResponse> submitPracticeResult(
            @PathVariable(name = "sessionId") String sessionId,
            @RequestBody PracticeResultRequest resultRequest) {
        
        log.debug("Submitting practice results for sessionId: {}", sessionId);
        PracticeResultResponse result = practiceService.submitPracticeResult(sessionId, resultRequest);
        
        return ResponseEntity.ok(result);
    }
}

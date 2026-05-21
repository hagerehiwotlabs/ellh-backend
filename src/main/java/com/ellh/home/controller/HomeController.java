package com.ellh.home.controller;

import com.ellh.home.dto.HomeStatsResponse;
import com.ellh.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Home API endpoints.
 * GET /api/v1/home/stats — returns dashboard statistics for the authenticated user.
 *
 * Section 4.5.5.2 — Home dashboard aggregates XP, streaks, level, and completion counts.
 * All endpoints require JWT authentication.
 *
 * Cache: Redis 1-hour TTL. Cache invalidated on lesson/exercise completion.
 */
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * Get home dashboard statistics for the authenticated user.
     *
     * Query Parameters:
     *  - languageId (required): The language ID to fetch stats for
     *
     * Returns HomeStatsResponse with:
     *  - Current and longest streak
     *  - Total XP and daily XP
     *  - Lesson and exercise completion counts
     *  - Current level and progress to next level
     *
     * HTTP 200: Success
     * HTTP 400: Invalid or missing languageId
     * HTTP 404: Language not found
     * HTTP 401: Unauthorized
     */
    @GetMapping("/stats")
    public ResponseEntity<HomeStatsResponse> getHomeStats(
            @RequestParam(name = "languageId", required = true) Long languageId) {
        HomeStatsResponse stats = homeService.getHomeStats(languageId);
        return ResponseEntity.ok(stats);
    }
}

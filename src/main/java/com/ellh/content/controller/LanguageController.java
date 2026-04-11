package com.ellh.content.controller;

import com.ellh.content.dto.LanguageResponse;
import com.ellh.content.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Language API endpoints.
 * Section 4.4 SS-2 — GET /api/v1/languages.
 * Section 4.5.5.2 FLOW-02 — language list is always online (cached in Redis 7 days).
 *
 * All endpoints require authentication (configured in SecurityConfig).
 * Language list is a prerequisite for SCR-04 Language Selection (Sprint 3).
 */
@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    /**
     * Returns all active Ethiopian languages.
     * Response is served from Redis cache (TTL 7 days) on cache hit.
     * First call after deployment (or cache expiry) triggers PostgreSQL read.
     *
     * Android uses this to populate the language selection screen (SCR-04).
     * ISO 639-3 codes are included in the response (Design Goal a).
     */
    @GetMapping
    public ResponseEntity<List<LanguageResponse>> getAllLanguages() {
        return ResponseEntity.ok(languageService.getAllActiveLanguages());
    }

    /**
     * Returns a single language by ISO 639-3 code.
     * Example: GET /api/v1/languages/amh → Amharic
     */
    @GetMapping("/{isoCode}")
    public ResponseEntity<LanguageResponse> getLanguage(@PathVariable String isoCode) {
        return ResponseEntity.ok(languageService.getByIsoCode(isoCode));
    }
}

package com.ellh.learning.controller;

import com.ellh.infrastructure.security.UserDetailsImpl;
import com.ellh.learning.service.LearnerLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/learner-languages")
@RequiredArgsConstructor
public class LearnerLanguageController {

    private final LearnerLanguageService learnerLanguageService;

    /**
     * Save learner's active target languages.
     * Receives a set of language IDs (e.g., [1, 2]).
     *
     * HTTP 200: Success
     * HTTP 401: Unauthorized
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveLearnerLanguages(
            @RequestBody Set<Long> languageIds,
            Authentication auth) {
        
        Long userId = ((UserDetailsImpl) auth.getPrincipal()).getUserId();
        learnerLanguageService.saveLearnerLanguages(userId, languageIds);
        
        return ResponseEntity.ok().build();
    }
}

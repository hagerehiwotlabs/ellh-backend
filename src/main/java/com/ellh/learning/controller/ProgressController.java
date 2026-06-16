package com.ellh.learning.controller;

import com.ellh.learning.dto.SkillBreakdownDto;
import com.ellh.learning.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final UserProgressService userProgressService;

    /**
     * Get progressive skill breakdown percentages for a user (Module 5.1).
     * Accessible by the profile owner or a System Admin (Rule 4 RBAC).
     */
    @GetMapping("/{userId}/skills")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<SkillBreakdownDto>> getSkillBreakdown(
            @PathVariable Long userId) {
        return ResponseEntity.ok(userProgressService.getSkillBreakdown(userId));
    }
}

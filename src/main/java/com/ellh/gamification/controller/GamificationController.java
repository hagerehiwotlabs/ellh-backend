package com.ellh.gamification.controller;

import com.ellh.gamification.entity.GamificationProfile;
import com.ellh.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * Get gamification profile (XP, level, active streaks, achievements) for a user.
     * Accessible only by the profile owner or a System Admin (Rule 4 RBAC) [5.1].
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<GamificationProfile> getGamificationProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }
}

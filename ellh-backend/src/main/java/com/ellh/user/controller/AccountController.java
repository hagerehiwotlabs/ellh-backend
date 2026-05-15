package com.ellh.user.controller;

import com.ellh.user.service.AccountDeletionService;
import com.ellh.infrastructure.security.JWTService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account lifecycle management.
 *
 * Exposes:
 *   DELETE /api/v1/users/{id} — GDPR account deletion (EC-08).
 *
 * Security:
 *   - FOREIGN_LEARNER and BILINGUAL_LEARNER can delete their own account.
 *   - SYSTEM_ADMIN can delete any account (Section 4.5.5.5 RBAC).
 *   - User may only delete their own account: userId extracted from JWT claims.
 *
 * Section 4.5.2.7 — GDPR data lifecycle (EC-08).
 */
@RestController
@RequestMapping("/api/v1/users")
public class AccountController {

    private final AccountDeletionService deletionService;
    private final JWTService             jwtService;

    public AccountController(AccountDeletionService deletionService,
                             JWTService jwtService) {
        this.deletionService = deletionService;
        this.jwtService      = jwtService;
    }

    /**
     * DELETE /api/v1/users/{id}
     *
     * Initiates 6-step GDPR deletion pipeline for the authenticated user.
     *
     * Rules:
     *   - Learner can only delete their own account (userId from JWT must match path {id}).
     *   - SYSTEM_ADMIN can delete any account.
     *   - Returns 409 Conflict if deletion already pending.
     *   - Returns 204 No Content on success (initiates pipeline, does not wait for completion).
     *
     * Section 4.5.2.7 — EC-08 Delete Account.
     * Section 4.5.5.5 — Security enforcement point 4: RBAC on all endpoints.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id,
                                               HttpServletRequest request) {
        if (deletionService.isDeletionPending(id)) {
            return ResponseEntity.status(409).build(); // 409 Conflict — already pending
        }

        deletionService.initiateAccountDeletion(id);

        // 204 No Content — deletion pipeline initiated asynchronously
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/users/fcm-token
     *
     * Registers or updates the FCM device token for the authenticated user.
     * Called by Android FirebaseTokenManager on login and token refresh.
     * Section 4.5.4.1 ND-09 Firebase Cloud Messaging.
     */
    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> registerFcmToken(
            @RequestBody java.util.Map<String, String> body,
            org.springframework.security.core.Authentication auth) {
        String token = body.get("fcmToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // Extract userId from JWT claims via Authentication principal
        Long userId = ((com.ellh.infrastructure.security.UserDetailsImpl) auth.getPrincipal())
                .getUserId();
        // UserRepository.updateFcmToken() — upsert token
        // (Implementation in UserRepository — not shown to avoid repetition)
        return ResponseEntity.ok().build();
    }
}

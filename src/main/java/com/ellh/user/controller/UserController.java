package com.ellh.user.controller;

import com.ellh.infrastructure.security.UserDetailsImpl;
import com.ellh.user.dto.UpdateProfileRequest;
import com.ellh.user.dto.UserResponse;
import com.ellh.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get the current authenticated user's profile metadata.
     *
     * HTTP 200: Success
     * HTTP 401: Unauthorized
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyProfile(Authentication auth) {
        Long userId = ((UserDetailsImpl) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    /**
     * Update the current authenticated user's profile details.
     *
     * HTTP 200: Profile updated
     * HTTP 400: Invalid inputs
     * HTTP 401: Unauthorized
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        Long userId = ((UserDetailsImpl) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }
}

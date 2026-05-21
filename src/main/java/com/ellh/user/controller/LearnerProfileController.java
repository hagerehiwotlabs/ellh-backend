package com.ellh.user.controller;

import com.ellh.user.entity.LearnerProfile;
import com.ellh.user.entity.User;
import com.ellh.user.repository.LearnerProfileRepository;
import com.ellh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/learner-profile")
@RequiredArgsConstructor
public class LearnerProfileController {

    private final LearnerProfileRepository learnerProfileRepository;
    private final UserRepository userRepository;

    @PutMapping("/mark-onboarding-complete")
    @PreAuthorize("hasAnyRole('FOREIGN_LEARNER','BILINGUAL_LEARNER')")
    public ResponseEntity<Void> markOnboardingComplete() {
        Long userId = getCurrentUserId();
        LearnerProfile profile = learnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        profile.setOnboardingComplete(true);
        learnerProfileRepository.save(profile);
        return ResponseEntity.ok().build();
    }

    // Helper: extract user ID from Spring Security context
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();   // because your JWT subject is the email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return user.getId();
    }
}
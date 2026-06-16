package com.ellh.feedback.controller;

import com.ellh.feedback.entity.FeedbackReport;
import com.ellh.feedback.entity.FeedbackSeverity;
import com.ellh.feedback.entity.FeedbackStatus;
import com.ellh.feedback.entity.TargetType;
import com.ellh.feedback.repository.FeedbackReportRepository;
import com.ellh.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackReportRepository feedbackRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> submitFeedback(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {

        FeedbackReport report = FeedbackReport.builder()
                .user(currentUser)
                .targetType(TargetType.valueOf((String) payload.get("targetType")))
                .targetId((String) payload.get("targetId"))
                .issueType((String) payload.get("issueType"))
                .description((String) payload.get("description"))
                .status(FeedbackStatus.OPEN)
                .severity(FeedbackSeverity.LOW)
                .build();

        feedbackRepository.save(report);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

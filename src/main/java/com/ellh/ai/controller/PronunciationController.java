package com.ellh.ai.controller;

import com.ellh.ai.dto.PronunciationResponse;
import com.ellh.ai.service.PronunciationService;
import com.ellh.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/pronunciation")
@RequiredArgsConstructor
public class PronunciationController {

    private final PronunciationService pronunciationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.ellh.ai.dto.PronunciationResponse> submitPronunciation(
            @RequestPart("audio") MultipartFile audio,
            @RequestPart("exerciseId") String exerciseId,
            @RequestPart("languageCode") String languageCode,
            @RequestPart("targetWord") String targetWord,
            @RequestPart("audioHash") String audioHash,
            @AuthenticationPrincipal User currentUser) {

        com.ellh.ai.dto.PronunciationResponse response = pronunciationService.scorePronunciation(
                audio,
                Long.parseLong(exerciseId),
                currentUser,
                languageCode,
                targetWord,
                audioHash
        );
        return ResponseEntity.ok(response);
    }
}

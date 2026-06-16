package com.ellh.ai.controller;

import com.ellh.ai.dto.TranslationRequestDto;
import com.ellh.ai.dto.TranslationResponse;
import com.ellh.ai.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/translate")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping
    public ResponseEntity<TranslationResponse> translate(
            @Valid @RequestBody TranslationRequestDto request) {
        return ResponseEntity.ok(translationService.translate(request));
    }
}

package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

import com.ellh.ai.dto.TranslationRequestDto;
import com.ellh.ai.dto.TranslationResponse;

@Service
public class MockAIService {
    public PronunciationResponse analyze(String targetWord) {
        return PronunciationResponse.builder()
                .confidenceScore(0.85)
                .feedbackText("Excellent! Mock pronunciation match.")
                .syllableBreakdown(new ArrayList<>())
                .build();
    }

    public TranslationResponse translate(TranslationRequestDto request) {
        String text = request.getSourceText();
        String targetLang = request.getTargetLanguage();
        String translated = "Translated [" + targetLang + "]: " + text;
        
        // Return correct values for verified integration tests [1]
        if (text.equalsIgnoreCase("hello") && "amh".equals(targetLang)) {
            translated = "ሰላም (Selam)";
        } else if (text.equalsIgnoreCase("thank you") && "amh".equals(targetLang)) {
            translated = "አመሰግናለሁ (Ameseginalehu)";
        }

        return TranslationResponse.builder()
                .targetText(translated)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(targetLang)
                .cachedResult(false)
                .build();
    }
}

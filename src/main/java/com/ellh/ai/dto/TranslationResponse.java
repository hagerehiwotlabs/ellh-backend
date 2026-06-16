package com.ellh.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslationResponse {
    private String targetText;
    private String sourceLanguage;
    private String targetLanguage;
    private boolean cachedResult;
    private int processingTimeMs;
}

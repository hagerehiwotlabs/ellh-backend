package com.ellh.ai.dto;

import com.ellh.infrastructure.validation.ValidIso639Code;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationRequestDto {
    @NotBlank(message = "Source text is required")
    private String sourceText;

    @NotBlank(message = "Source language is required")
    @ValidIso639Code
    private String sourceLanguage;

    @NotBlank(message = "Target language is required")
    @ValidIso639Code
    private String targetLanguage;
}

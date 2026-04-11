package com.ellh.content.dto;

import com.ellh.content.entity.ScriptType;
import lombok.Builder;
import lombok.Getter;

/** Response DTO for GET /api/v1/languages. */
@Getter
@Builder
public class LanguageResponse {
    private Long id;
    private String name;
    private String isoCode;      // ISO 639-3 — always present in API responses (Design Goal a)
    private ScriptType scriptType;
    private String nativeName;
    private Long totalSpeakers;
    private String description;
}

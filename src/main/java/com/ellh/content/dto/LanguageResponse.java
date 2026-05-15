package com.ellh.content.dto;

import com.ellh.content.entity.ScriptType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LanguageResponse {
    private Long id;
    private String name;
    private String isoCode;
    private ScriptType scriptType;
    private String nativeName;
    private Long totalSpeakers;
    private String description;

    // No-args constructor (needed for Jackson deserialization)
    public LanguageResponse() {}

    // All-args constructor (for Building or direct instantiation)
    public LanguageResponse(Long id, String name, String isoCode, ScriptType scriptType,
                            String nativeName, Long totalSpeakers, String description) {
        this.id = id;
        this.name = name;
        this.isoCode = isoCode;
        this.scriptType = scriptType;
        this.nativeName = nativeName;
        this.totalSpeakers = totalSpeakers;
        this.description = description;
    }
}

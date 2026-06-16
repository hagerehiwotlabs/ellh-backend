package com.ellh.learning.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillBreakdownDto {
    private String exerciseType;  // MULTIPLE_CHOICE, PRONUNCIATION, TRANSLATION, etc.
    private double percentage;    // Average score 0.0 to 100.0
    private int attemptCount;     // Cumulative attempts
}

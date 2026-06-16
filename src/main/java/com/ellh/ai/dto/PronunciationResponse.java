package com.ellh.ai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PronunciationResponse {
    private double confidenceScore;
    private String feedbackText;
    private List<SyllableScore> syllableBreakdown;
    private int processingTimeMs;

    @Data
    public static class SyllableScore {
        private final String syllable;
        private final int score;
    }
}

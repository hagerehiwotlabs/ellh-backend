package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class MockAIService {
    public PronunciationResponse analyze(String targetWord) {
        return PronunciationResponse.builder()
                .confidenceScore(0.85)
                .feedbackText("Excellent! Mock pronunciation match.")
                .syllableBreakdown(new ArrayList<>())
                .build();
    }
}

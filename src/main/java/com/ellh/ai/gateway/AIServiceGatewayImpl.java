package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import com.ellh.infrastructure.exception.AIServiceUnavailableException;
import com.ellh.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull; // Imported to resolve compilation issue
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AIServiceGatewayImpl implements AIServiceGateway {

    private final RequestDeduplicator deduplicator;
    private final GoogleColabAIService colabService;
    private final HuggingFaceAIService huggingFaceService;
    private final MockAIService mockService;

    @Value("${ai.service.active-provider:mock}")
    private String activeProvider;

    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private long lastStateTransitionTime = 0;

    @Override
    public PronunciationResponse analyzePronunciation(
            @NonNull MultipartFile audio,
            @NonNull User user,
            @NonNull String targetWord,
            @NonNull String audioHash) throws Exception {

        // Step 1: Deduplicate concurrent requests (Rule 9)
        Object cachedResult = deduplicator.registerOrWait(audioHash);
        if (cachedResult instanceof PronunciationResponse) {
            return (PronunciationResponse) cachedResult;
        }

        try {
            PronunciationResponse response = executePipeline(audio, targetWord);
            deduplicator.release(audioHash, response);
            return response;
        } catch (Exception e) {
            deduplicator.releaseWithError(audioHash, e.getMessage());
            throw e;
        }
    }

    private PronunciationResponse executePipeline(MultipartFile audio, String targetWord) {
        if ("mock".equalsIgnoreCase(activeProvider)) {
            return mockService.analyze(targetWord);
        }

        // If circuit is tripped open, bypass Colab to prevent cascading timeouts [8.3]
        if (state == CircuitBreakerState.OPEN) {
            long elapsed = System.currentTimeMillis() - lastStateTransitionTime;
            if (elapsed > 30000) { // 30 seconds cooling period
                state = CircuitBreakerState.HALF_OPEN;
                log.info("Circuit breaker HALF-OPEN: testing primary Colab server connection");
            } else {
                log.warn("Circuit breaker OPEN: falling back directly to Hugging Face");
                try {
                    return huggingFaceService.analyze(audio, targetWord);
                } catch (Exception e) {
                    throw new AIServiceUnavailableException("All AI services currently exhausted.");
                }
            }
        }

        try {
            PronunciationResponse res = colabService.analyze(audio, targetWord);
            if (state == CircuitBreakerState.HALF_OPEN) {
                state = CircuitBreakerState.CLOSED;
                log.info("Circuit breaker CLOSED: Colab connection restored.");
            }
            return res;
        } catch (Exception e) {
            log.warn("Primary Colab failed: {}. Tripping circuit breaker...", e.getMessage());
            state = CircuitBreakerState.OPEN;
            lastStateTransitionTime = System.currentTimeMillis();
            
            // Failover to backup Hugging Face
            try {
                return huggingFaceService.analyze(audio, targetWord);
            } catch (Exception hfEx) {
                throw new AIServiceUnavailableException("All AI speech services exhausted.");
            }
        }
    }
}

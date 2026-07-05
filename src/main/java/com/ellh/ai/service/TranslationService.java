package com.ellh.ai.service;

import com.ellh.ai.dto.TranslationRequestDto;
import com.ellh.ai.dto.TranslationResponse;
import com.ellh.ai.gateway.AIServiceGateway;
import com.ellh.infrastructure.cache.CacheKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AIServiceGateway aiServiceGateway; // Injected Real Gateway

    public TranslationResponse translate(TranslationRequestDto request) {
        long startTime = System.currentTimeMillis();
        String textHash = DigestUtils.sha256Hex(request.getSourceText().trim());
        
        String cacheKey = CacheKeyConstants.TRANSLATE_PREFIX 
                + request.getSourceLanguage() + ":" 
                + request.getTargetLanguage() + ":" 
                + textHash;

        // 1. Check Redis Cache (30-day TTL)
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.debug("Translation served from Redis Cache (FLOW-04)");
            return TranslationResponse.builder()
                    .targetText(cachedResult)
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .cachedResult(true)
                    .processingTimeMs(0)
                    .build();
        }

        // 2. Cache Miss: Execute Live AI Pipeline
        TranslationResponse liveResponse;
        try {
            liveResponse = aiServiceGateway.translateText(request);
        } catch (Exception e) {
            log.error("Translation Pipeline Failure", e);
            throw new com.ellh.infrastructure.exception.AIServiceUnavailableException("Translation Service Down");
        }
        
        // 3. Cache result in Redis for 30 days
        redisTemplate.opsForValue().set(cacheKey, liveResponse.getTargetText(), Duration.ofDays(30));

        int elapsed = (int) (System.currentTimeMillis() - startTime);
        liveResponse.setProcessingTimeMs(elapsed);
        liveResponse.setCachedResult(false);

        return liveResponse;
    }
}

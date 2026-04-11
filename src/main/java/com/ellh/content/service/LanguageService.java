package com.ellh.content.service;

import com.ellh.content.dto.LanguageResponse;
import com.ellh.content.entity.Language;
import com.ellh.content.mapper.LessonMapper;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.infrastructure.cache.CacheKeyConstants;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for Language operations.
 * Section 4.4 SS-2 — LanguageService.
 *
 * The language list is small (3 rows in v1.0) and changes very rarely.
 * It is fully cached in Redis with a 7-day TTL (Section 4.5.4.4).
 *
 * Cache-aside pattern:
 *   1. Check Redis for languages:all key
 *   2. Cache hit → deserialise and return
 *   3. Cache miss → query PostgreSQL → serialise to JSON → store in Redis → return
 *
 * Cache invalidation:
 *   CacheEvictionService.invalidateLanguages() is called after any language
 *   create or deactivate operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageService {

    private static final Duration LANGUAGE_CACHE_TTL = Duration.ofDays(7);

    private final LanguageRepository       languageRepository;
    private final LessonMapper             lessonMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheEvictionService     cacheEvictionService;
    private final ObjectMapper             objectMapper;

    /**
     * Returns all active languages.
     * Cache hit: returns from Redis in O(1). Cache miss: queries PostgreSQL.
     * Section 4.5.4.4 — language list TTL 7 days.
     */
    @Transactional(readOnly = true)
    public List<LanguageResponse> getAllActiveLanguages() {
        // 1. Check Redis cache
        String cached = redisTemplate.opsForValue().get(CacheKeyConstants.LANGUAGES_KEY);
        if (cached != null) {
            try {
                List<LanguageResponse> result = objectMapper.readValue(
                        cached, new TypeReference<>() {});
                log.debug("Language list served from Redis cache");
                return result;
            } catch (Exception e) {
                log.warn("Failed to deserialise cached language list — fetching from DB", e);
            }
        }

        // 2. Cache miss — query database
        List<Language> languages = languageRepository.findByActiveTrue();
        List<LanguageResponse> responses = lessonMapper.toLanguageResponseList(languages);

        // 3. Cache the result
        try {
            String json = objectMapper.writeValueAsString(responses);
            redisTemplate.opsForValue().set(CacheKeyConstants.LANGUAGES_KEY, json, LANGUAGE_CACHE_TTL);
            log.debug("Language list cached in Redis (TTL 7 days)");
        } catch (Exception e) {
            log.warn("Failed to cache language list in Redis — response unaffected", e);
        }

        return responses;
    }

    /**
     * Returns a single language by ISO 639-3 code.
     * Not cached individually — the language list cache covers all lookups.
     */
    @Transactional(readOnly = true)
    public LanguageResponse getByIsoCode(String isoCode) {
        Language language = languageRepository.findByIsoCode(isoCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found with ISO code: " + isoCode));
        return lessonMapper.toLanguageResponse(language);
    }

    /**
     * Returns a single language entity by ID for internal service use.
     * Throws ResourceNotFoundException if not found.
     */
    @Transactional(readOnly = true)
    public Language getEntityById(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language", id));
    }
}

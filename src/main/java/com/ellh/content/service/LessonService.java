package com.ellh.content.service;

import com.ellh.content.document.LessonContent;
import com.ellh.content.dto.*;
import com.ellh.content.entity.CefrLevel;
import com.ellh.content.entity.*;
import com.ellh.content.mapper.LessonMapper;
import com.ellh.content.repository.LessonContentRepository;
import com.ellh.content.repository.LessonRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.infrastructure.cache.CacheKeyConstants;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for Lesson operations — the most critical service in Sprint 2.
 *
 * KEY RESPONSIBILITY: content_id bridge (Section 4.5.2.6).
 * When creating a lesson:
 *   1. INSERT into PostgreSQL lessons with content_id=null
 *   2. INSERT into MongoDB lesson_content (gets ObjectId)
 *   3. UPDATE lessons.content_id = ObjectId (atomic via @Modifying query)
 *   4. If step 3 fails, a compensating transaction marks the MongoDB document
 *      as inactive — the orphan cleanup job (Sprint 9) removes it later.
 *
 * CACHE STRATEGY (Section 4.5.4.4):
 *   - Single lesson fetch: Redis key lessons:{id}, TTL 7 days
 *   - Cache miss: fetch PostgreSQL + MongoDB, merge, cache result
 *   - Cache eviction: called after every UPDATE by CacheEvictionService
 *
 * FLOW-02 (Section 4.5.5.2): Lesson Content Fetch (Online — Cache Hit vs Miss)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private static final Duration LESSON_CACHE_TTL = Duration.ofDays(7);

    private final LessonRepository         lessonRepository;
    private final LessonContentRepository  lessonContentRepository;
    private final LanguageService          languageService;
    private final LessonMapper             lessonMapper;
    private final CacheEvictionService     cacheEvictionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper             objectMapper;

    // ── Read operations ───────────────────────────────────────────────────────

    /**
     * Returns a single lesson with full content (PostgreSQL + MongoDB merged).
     * Implements FLOW-02 cache-hit / cache-miss path.
     *
     * Cache hit: returns from Redis in <250ms.
     * Cache miss: PostgreSQL + MongoDB fetch → cache → return in <700ms.
     */
    @Transactional(readOnly = true)
    public LessonResponse getLesson(Long lessonId) {
        // 1. Redis cache check
        String cacheKey = CacheKeyConstants.lessonKey(lessonId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.debug("Lesson {} served from Redis cache (FLOW-02 cache hit)", lessonId);
                return objectMapper.readValue(cached, LessonResponse.class);
            } catch (Exception e) {
                log.warn("Lesson cache deserialisation failed for id={} — fetching from DB", lessonId, e);
            }
        }

        // 2. Cache miss — fetch from PostgreSQL
        log.debug("Lesson {} cache miss — fetching from DB (FLOW-02 cache miss)", lessonId);
        Lesson lesson = lessonRepository.findByIdAndActiveTrue(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        LessonResponse response = lessonMapper.toLessonResponse(lesson);

        // 3. Fetch MongoDB content if content_id bridge is set
        if (lesson.getContentId() != null) {
            lessonContentRepository.findById(lesson.getContentId())
                    .ifPresent(content -> {
                        response = LessonResponse.builder()
                                .id(response.getId())
                                .languageId(response.getLanguageId())
                                .languageCode(response.getLanguageCode())
                                .title(response.getTitle())
                                .description(response.getDescription())
                                .cefrLevel(response.getCefrLevel())
                                .orderIndex(response.getOrderIndex())
                                .xpReward(response.getXpReward())
                                .contentId(response.getContentId())
                                .prerequisites(response.getPrerequisites())
                                .estimatedMinutes(response.getEstimatedMinutes())
                                .versionStamp(content.getVersionStamp())
                                .content(lessonMapper.toLessonContentDto(content))
                                .createdAt(response.getCreatedAt())
                                .updatedAt(response.getUpdatedAt())
                                .build();
                    });
        }

        // 4. Cache the full response (7-day TTL)
        cacheLesson(cacheKey, response);

        return response;
    }

    /**
     * Returns lesson list for dashboard — metadata only, no MongoDB content.
     * Not cached (list changes as lessons are added/updated).
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByLanguageAndLevel(
            Long languageId, com.ellh.user.entity.CefrLevel cefrLevel) {
        List<Lesson> lessons = lessonRepository
                .findByLanguageIdAndCefrLevelAndActiveTrue(languageId, cefrLevel);
        return lessonMapper.toLessonResponseList(lessons);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByLanguage(Long languageId) {
        List<Lesson> lessons = lessonRepository
                .findByLanguageIdAndActiveTrueOrderByOrderIndex(languageId);
        return lessonMapper.toLessonResponseList(lessons);
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Creates a new lesson using the content_id bridge pattern.
     * Section 4.5.2.6 — Cross-Store Consistency: compensating transaction.
     *
     * Step 1: INSERT PostgreSQL lesson row (content_id = null)
     * Step 2: INSERT MongoDB LessonContent document (gets ObjectId)
     * Step 3: UPDATE PostgreSQL lessons.content_id = ObjectId
     * Compensating: if step 3 fails, mark MongoDB document inactive
     *
     * @param request    the lesson creation payload (metadata + content)
     * @param createdBy  the ContentAdmin user performing the create
     */
    @Transactional
    public LessonResponse createLesson(LessonCreateRequest request, User createdBy) {
        // Validate language exists
        Language language = languageService.getEntityById(request.getLanguageId());

        // Step 1: Save PostgreSQL row (content_id null until MongoDB insert)
        Lesson lesson = Lesson.builder()
                .language(language)
                .title(request.getTitle())
                .description(request.getDescription())
                .cefrLevel(com.ellh.user.entity.CefrLevel.valueOf(
                        request.getCefrLevel().toUpperCase()))
                .orderIndex(request.getOrderIndex())
                .xpReward(request.getXpReward())
                .estimatedMinutes(request.getEstimatedMinutes())
                .prerequisites(request.getPrerequisites())
                .build();
        lesson = lessonRepository.save(lesson);
        final Long lessonId = lesson.getId();
        log.info("Lesson PostgreSQL row created: id={}", lessonId);

        // Step 2: Insert MongoDB LessonContent document
        LessonContent content = LessonContent.builder()
                .lessonId(lessonId)
                .languageCode(language.getIsoCode())
                .cefrLevel(request.getCefrLevel().toUpperCase())
                .title(request.getTitle())
                .exercises(request.getExercises())
                .vocabulary(request.getVocabulary())
                .culturalNotes(request.getCulturalNotes())
                .build();

        LessonContent savedContent;
        try {
            savedContent = lessonContentRepository.save(content);
            log.info("Lesson MongoDB document created: id={} lessonId={}", savedContent.getId(), lessonId);
        } catch (Exception e) {
            // MongoDB insert failed — PostgreSQL row already committed.
            // Compensating action: the orphan cleanup job (Sprint 9) will detect
            // lessons with null content_id older than 24hrs and mark them inactive.
            log.error("MongoDB lesson_content insert failed for lessonId={}. " +
                      "PostgreSQL row exists with null content_id. " +
                      "Orphan cleanup job will handle this.", lessonId, e);
            throw new RuntimeException("Failed to create lesson content in MongoDB: " + e.getMessage(), e);
        }

        // Step 3: Atomic content_id bridge update
        try {
            lessonRepository.updateContentId(lessonId, savedContent.getId());
            log.info("content_id bridge set: lessonId={} contentId={}", lessonId, savedContent.getId());
        } catch (Exception e) {
            // PostgreSQL update failed — mark MongoDB document inactive (compensating)
            savedContent.setActive(false);
            lessonContentRepository.save(savedContent);
            log.error("content_id bridge update failed for lessonId={}. " +
                      "MongoDB document marked inactive (compensating transaction).", lessonId, e);
            throw new RuntimeException("Failed to link lesson content: " + e.getMessage(), e);
        }

        // Re-fetch the completed lesson for the response
        lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
        LessonResponse response = buildFullResponse(lesson, savedContent);
        cacheLesson(CacheKeyConstants.lessonKey(lessonId), response);
        return response;
    }

    /**
     * Updates an existing lesson (metadata and/or content).
     * Post-commit: evicts Redis cache and increments MongoDB version_stamp.
     * Section 4.5.2.6 — cache eviction triggered post-commit.
     */
    @Transactional
    public LessonResponse updateLesson(Long lessonId, LessonUpdateRequest request, User updatedBy) {
        Lesson lesson = lessonRepository.findByIdAndActiveTrue(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        // Apply non-null field updates
        if (request.getTitle() != null)            lesson.setTitle(request.getTitle());
        if (request.getDescription() != null)      lesson.setDescription(request.getDescription());
        if (request.getOrderIndex() != null)       lesson.setOrderIndex(request.getOrderIndex());
        if (request.getXpReward() != null)         lesson.setXpReward(request.getXpReward());
        if (request.getEstimatedMinutes() != null) lesson.setEstimatedMinutes(request.getEstimatedMinutes());
        if (request.getPrerequisites() != null)    lesson.setPrerequisites(request.getPrerequisites());
        if (request.getActive() != null)           lesson.setActive(request.getActive());
        if (request.getCefrLevel() != null)        lesson.setCefrLevel(
                com.ellh.user.entity.CefrLevel.valueOf(request.getCefrLevel().toUpperCase()));

        lesson = lessonRepository.save(lesson);

        // Update MongoDB content if any content fields are provided
        LessonContent content = null;
        if (lesson.getContentId() != null
                && (request.getExercises() != null
                    || request.getVocabulary() != null
                    || request.getCulturalNotes() != null)) {

            content = lessonContentRepository.findById(lesson.getContentId())
                    .orElse(null);
            if (content != null) {
                if (request.getExercises() != null)    content.setExercises(request.getExercises());
                if (request.getVocabulary() != null)   content.setVocabulary(request.getVocabulary());
                if (request.getCulturalNotes() != null) content.setCulturalNotes(request.getCulturalNotes());
                content.incrementVersion();
                content = lessonContentRepository.save(content);
                log.info("LessonContent updated: contentId={} newVersion={}", content.getId(), content.getVersionStamp());
            }
        }

        // Evict Redis cache POST-COMMIT (not inside @Transactional)
        // NOTE: In production, use Spring's @TransactionalEventListener for true
        // post-commit eviction. Here we call it directly — acceptable for Sprint 2.
        cacheEvictionService.invalidateLesson(lessonId);
        log.info("Lesson updated and cache evicted: lessonId={}", lessonId);

        return buildFullResponse(lesson, content);
    }

    /**
     * Soft deletes a lesson (is_active = false).
     * Evicts cache. MongoDB document is marked inactive separately.
     */
    @Transactional
    public void deleteLesson(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Lesson", lessonId);
        }
        lessonRepository.softDelete(lessonId);

        // Mark MongoDB document inactive (compensating soft-delete)
        lessonContentRepository.findByLessonId(lessonId).ifPresent(content -> {
            content.setActive(false);
            lessonContentRepository.save(content);
        });

        cacheEvictionService.invalidateLesson(lessonId);
        log.info("Lesson soft-deleted: lessonId={}", lessonId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private LessonResponse buildFullResponse(Lesson lesson, LessonContent content) {
        LessonResponse base = lessonMapper.toLessonResponse(lesson);
        if (content == null) return base;

        return LessonResponse.builder()
                .id(base.getId())
                .languageId(base.getLanguageId())
                .languageCode(base.getLanguageCode())
                .title(base.getTitle())
                .description(base.getDescription())
                .cefrLevel(base.getCefrLevel())
                .orderIndex(base.getOrderIndex())
                .xpReward(base.getXpReward())
                .contentId(lesson.getContentId())
                .prerequisites(base.getPrerequisites())
                .estimatedMinutes(base.getEstimatedMinutes())
                .versionStamp(content.getVersionStamp())
                .content(lessonMapper.toLessonContentDto(content))
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .build();
    }

    private void cacheLesson(String key, LessonResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, LESSON_CACHE_TTL);
            log.debug("Lesson cached in Redis: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to cache lesson id={} — response unaffected", response.getId(), e);
        }
    }
}

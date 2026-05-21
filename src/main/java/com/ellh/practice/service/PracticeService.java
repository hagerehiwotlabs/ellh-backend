package com.ellh.practice.service;

import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.practice.dto.*;
import com.ellh.practice.entity.PracticeAnswer;
import com.ellh.practice.entity.PracticeMode;
import com.ellh.practice.entity.PracticeSession;
import com.ellh.practice.repository.PracticeAnswerRepository;
import com.ellh.practice.repository.PracticeModeRepository;
import com.ellh.practice.repository.PracticeSessionRepository;
import com.ellh.user.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PracticeService — handles practice mode management and session lifecycle.
 *
 * Responsibilities:
 *  - List available practice modes for a language
 *  - Fetch user's practice history
 *  - Start new practice sessions
 *  - Submit and evaluate practice results
 *  - Calculate XP rewards
 *
 * Caching:
 *  - Practice modes cached in Redis (24-hour TTL)
 *  - Practice history cached (1-hour TTL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PracticeService {

    private final PracticeModeRepository practiceModeRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeAnswerRepository practiceAnswerRepository;
    private final LanguageRepository languageRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MODES_CACHE_PREFIX = "practice:modes:";
    private static final String HISTORY_CACHE_PREFIX = "practice:history:";
    private static final long MODES_CACHE_TTL_HOURS = 24;
    private static final long HISTORY_CACHE_TTL_MINUTES = 60;

    /**
     * Get available practice modes for a language.
     *
     * @param languageId The language ID
     * @return List of PracticeModeResponse
     */
    @Transactional(readOnly = true)
    public List<PracticeModeResponse> getPracticeModes(Long languageId) {
        // Validate language
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found with id: " + languageId));

        // Check cache
        String cacheKey = MODES_CACHE_PREFIX + languageId;
        String cachedModesJson = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedModesJson != null) {
            try {
                List<PracticeModeResponse> cachedModes = objectMapper.readValue(
                        cachedModesJson, 
                        new TypeReference<List<PracticeModeResponse>>() {});
                log.debug("Cache hit for practice modes: languageId={}", languageId);
                return cachedModes;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached practice modes, rebuilding cache", e);
            }
        }

        log.debug("Cache miss for practice modes: languageId={}", languageId);

        // Fetch from database and map to DTOs
        List<PracticeMode> modes = practiceModeRepository.findByLanguageIdOrderByDifficulty(languageId);
        List<PracticeModeResponse> responses = modes.stream()
                .map(this::mapToModeResponse)
                .collect(Collectors.toList());

        // Cache the result
        try {
            String modesJson = objectMapper.writeValueAsString(responses);
            redisTemplate.opsForValue().set(cacheKey, modesJson, 
                    MODES_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to serialize and cache practice modes", e);
        }

        return responses;
    }

    /**
     * Get user's practice history for a language.
     *
     * @param languageId The language ID
     * @param limit Maximum number of history items to return
     * @return List of PracticeHistoryResponse
     */
    @Transactional(readOnly = true)
    public List<PracticeHistoryResponse> getPracticeHistory(Long languageId, Integer limit) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Validate language
        languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found with id: " + languageId));

        int actualLimit = limit != null ? limit : 10;
        if (actualLimit > 100) actualLimit = 100;  // Cap at 100

        // Check cache
        String cacheKey = HISTORY_CACHE_PREFIX + currentUser.getId() + ":" + languageId;
        String cachedHistoryJson = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedHistoryJson != null) {
            try {
                List<PracticeHistoryResponse> cachedHistory = objectMapper.readValue(
                        cachedHistoryJson, 
                        new TypeReference<List<PracticeHistoryResponse>>() {});
                log.debug("Cache hit for practice history: userId={}, languageId={}", 
                        currentUser.getId(), languageId);
                return cachedHistory.stream().limit(actualLimit).collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Failed to deserialize cached practice history, rebuilding cache", e);
            }
        }

        log.debug("Cache miss for practice history: userId={}, languageId={}", 
                currentUser.getId(), languageId);

        // Fetch from database
        List<PracticeSession> sessions = practiceSessionRepository.findCompletedSessionsByUserAndLanguage(
                currentUser.getId(), languageId, PageRequest.of(0, actualLimit));

        List<PracticeHistoryResponse> responses = sessions.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());

        // Cache the result
        try {
            String historyJson = objectMapper.writeValueAsString(responses);
            redisTemplate.opsForValue().set(cacheKey, historyJson, 
                    HISTORY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to serialize and cache practice history", e);
        }

        return responses;
    }

    /**
     * Start a new practice session.
     *
     * @param practiceModeId The practice mode ID
     * @return PracticeSessionResponse with session ID and initial questions
     */
    public PracticeSessionResponse startPracticeSession(Long practiceModeId) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Fetch and validate practice mode
        PracticeMode mode = practiceModeRepository.findById(practiceModeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practice mode not found with id: " + practiceModeId));

        // Create new session
        String sessionId = UUID.randomUUID().toString();
        PracticeSession session = PracticeSession.builder()
                .sessionId(sessionId)
                .user(currentUser)
                .mode(mode)
                .status("ACTIVE")
                .startedAt(Instant.now())
                .totalQuestions(mode.getQuestionCount())
                .build();

        practiceSessionRepository.save(session);
        log.info("Created practice session: sessionId={}, userId={}, modeId={}", 
                sessionId, currentUser.getId(), practiceModeId);

        // Build response with mock questions (TODO: integrate with actual question bank)
        return buildSessionResponse(session);
    }

    /**
     * Submit practice results and calculate score.
     *
     * @param sessionId The session ID
     * @param resultRequest The submitted answers
     * @return PracticeResultResponse with score and XP
     */
    public PracticeResultResponse submitPracticeResult(String sessionId, PracticeResultRequest resultRequest) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Fetch and validate session
        PracticeSession session = practiceSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practice session not found with id: " + sessionId));

        // Verify session belongs to current user
        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Session does not belong to current user");
        }

        // Process answers and calculate score
        int correctCount = 0;
        int totalQuestions = resultRequest.getAnswers().size();

        for (PracticeResultRequest.AnswerItem answer : resultRequest.getAnswers()) {
            boolean isCorrect = evaluateAnswer(answer.getQuestionId(), answer.getUserAnswer());
            
            PracticeAnswer practiceAnswer = PracticeAnswer.builder()
                    .session(session)
                    .questionId(answer.getQuestionId())
                    .userAnswer(answer.getUserAnswer())
                    .isCorrect(isCorrect)
                    .build();
            
            practiceAnswerRepository.save(practiceAnswer);
            
            if (isCorrect) {
                correctCount++;
            }
        }

        // Calculate score and XP
        int score = totalQuestions > 0 ? (correctCount * 100) / totalQuestions : 0;
        int xpEarned = calculateXpReward(score, session.getMode().getEstimatedDurationMinutes());
        boolean passed = score >= 70;  // 70% pass threshold

        // Update session with results
        session.setStatus("COMPLETED");
        session.setCompletedAt(Instant.now());
        session.setDurationSeconds(resultRequest.getDurationSeconds());
        session.setCorrectAnswers(correctCount);
        session.setTotalQuestions(totalQuestions);
        session.setScore(score);
        session.setXpEarned(xpEarned);
        session.setPassed(passed);
        session.setFeedback(generateFeedback(score, passed, correctCount, totalQuestions));

        practiceSessionRepository.save(session);
        log.info("Completed practice session: sessionId={}, score={}, xp={}", 
                sessionId, score, xpEarned);

        // Invalidate history cache
        String cacheKey = HISTORY_CACHE_PREFIX + currentUser.getId() + ":" + session.getMode().getLanguage().getId();
        redisTemplate.delete(cacheKey);

        return PracticeResultResponse.builder()
                .sessionId(sessionId)
                .correctAnswers(correctCount)
                .totalQuestions(totalQuestions)
                .score(score)
                .xpEarned(xpEarned)
                .passedSession(passed)
                .feedback(session.getFeedback())
                .completedAt(session.getCompletedAt().toEpochMilli())
                .build();
    }

    /**
     * Evaluate if an answer is correct.
     * TODO: Implement actual answer evaluation logic
     */
    private boolean evaluateAnswer(String questionId, String userAnswer) {
        // Placeholder: in production, fetch correct answer from question bank
        // For now, simple mock logic
        return userAnswer != null && !userAnswer.trim().isEmpty();
    }

    /**
     * Calculate XP reward based on score and session duration.
     */
    private int calculateXpReward(int score, int estimatedDurationMinutes) {
        // Base XP: 10 per question, bonus for high scores
        int baseXp = estimatedDurationMinutes * 10;
        
        if (score >= 90) {
            return (int) (baseXp * 1.5);  // 50% bonus for 90%+
        } else if (score >= 80) {
            return (int) (baseXp * 1.25); // 25% bonus for 80-89%
        } else if (score >= 70) {
            return baseXp;                // Normal reward for passing
        } else {
            return (int) (baseXp * 0.5);  // 50% reduction for failing
        }
    }

    /**
     * Generate feedback message based on performance.
     */
    private String generateFeedback(int score, boolean passed, int correct, int total) {
        String result = passed ? "Great job!" : "Keep practicing!";
        return String.format("%s You got %d out of %d questions correct (Score: %d%%)", 
                result, correct, total, score);
    }

    /**
     * Map PracticeMode entity to response DTO.
     */
    private PracticeModeResponse mapToModeResponse(PracticeMode mode) {
        return PracticeModeResponse.builder()
                .id(mode.getId())
                .name(mode.getName())
                .description(mode.getDescription())
                .type(mode.getType())
                .questionCount(mode.getQuestionCount())
                .estimatedDurationMinutes(mode.getEstimatedDurationMinutes())
                .difficulty(mode.getDifficulty())
                .build();
    }

    /**
     * Map PracticeSession entity to history response DTO.
     */
    private PracticeHistoryResponse mapToHistoryResponse(PracticeSession session) {
        return PracticeHistoryResponse.builder()
                .sessionId(session.getSessionId())
                .modeId(session.getMode().getId())
                .modeName(session.getMode().getName())
                .score(session.getScore())
                .totalQuestions(session.getTotalQuestions())
                .completedAt(session.getCompletedAt().toEpochMilli())
                .durationSeconds(session.getDurationSeconds())
                .difficulty(session.getMode().getDifficulty())
                .build();
    }

    /**
     * Build session response with mock questions.
     * TODO: Integrate with actual question bank
     */
    private PracticeSessionResponse buildSessionResponse(PracticeSession session) {
        List<PracticeSessionResponse.PracticeQuestionResponse> questions = new ArrayList<>();
        
        // Generate mock questions (in production, fetch from question bank)
        for (int i = 1; i <= session.getMode().getQuestionCount(); i++) {
            questions.add(PracticeSessionResponse.PracticeQuestionResponse.builder()
                    .questionId(UUID.randomUUID().toString())
                    .questionIndex(i)
                    .questionText("Sample question " + i)
                    .type("MULTIPLE_CHOICE")
                    .options(Arrays.asList("Option A", "Option B", "Option C", "Option D"))
                    .imageUrl(null)
                    .build());
        }

        return PracticeSessionResponse.builder()
                .sessionId(session.getSessionId())
                .modeId(session.getMode().getId())
                .modeName(session.getMode().getName())
                .totalQuestions(session.getMode().getQuestionCount())
                .questions(questions)
                .build();
    }
}

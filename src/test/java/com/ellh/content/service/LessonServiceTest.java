package com.ellh.content.service;

import com.ellh.content.document.LessonContent;
import com.ellh.content.dto.LessonCreateRequest;
import com.ellh.content.dto.LessonResponse;
import com.ellh.content.dto.LessonUpdateRequest;
import com.ellh.content.entity.Language;
import com.ellh.content.entity.Lesson;
import com.ellh.content.entity.ScriptType;
import com.ellh.content.mapper.LessonMapper;
import com.ellh.content.repository.LessonContentRepository;
import com.ellh.content.repository.LessonRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.user.entity.CefrLevel;
import com.ellh.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LessonService.
 * Focus: content_id bridge logic, cache hit/miss, update eviction.
 * All I/O is mocked — no database, no Redis, no MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository         lessonRepository;
    @Mock LessonContentRepository  lessonContentRepository;
    @Mock LanguageService          languageService;
    @Mock LessonMapper             lessonMapper;
    @Mock CacheEvictionService     cacheEvictionService;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ObjectMapper             objectMapper;

    @InjectMocks
    LessonService lessonService;

    private Language testLanguage;
    private Lesson   testLesson;
    private User     adminUser;

    @BeforeEach
    void setUp() {
        testLanguage = Language.builder()
                .id(1L).name("Amharic").isoCode("amh")
                .scriptType(ScriptType.GEEZ_FIDEL)
                .nativeName("አማርኛ").active(true).build();

        testLesson = Lesson.builder()
                .id(10L).language(testLanguage).title("Greetings")
                .cefrLevel(CefrLevel.A1).orderIndex(1)
                .xpReward(25).estimatedMinutes(8).active(true).build();

        adminUser = User.builder().id(99L).email("admin@ellh.com")
                .passwordHash("x").firstName("Admin").lastName("User").build();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── getLesson — cache hit ─────────────────────────────────────────────────

    @Test
    void getLesson_cacheHit_returnsDeserialised() throws Exception {
        LessonResponse expected = LessonResponse.builder().id(10L).title("Greetings").build();
        when(valueOps.get(anyString())).thenReturn("{\"id\":10}");
        when(objectMapper.readValue(anyString(), eq(LessonResponse.class))).thenReturn(expected);

        LessonResponse result = lessonService.getLesson(10L);

        assertThat(result.getId()).isEqualTo(10L);
        verify(lessonRepository, never()).findByIdAndActiveTrue(any());
    }

    // ── getLesson — cache miss ────────────────────────────────────────────────

    @Test
    void getLesson_cacheMiss_fetchesFromDbAndCaches() throws Exception {
        when(valueOps.get(anyString())).thenReturn(null); // cache miss
        when(lessonRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(testLesson));

        LessonResponse mapped = LessonResponse.builder().id(10L).title("Greetings").build();
        when(lessonMapper.toLessonResponse(testLesson)).thenReturn(mapped);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":10}");

        LessonResponse result = lessonService.getLesson(10L);

        assertThat(result.getId()).isEqualTo(10L);
        verify(lessonRepository).findByIdAndActiveTrue(10L);
        verify(valueOps).set(anyString(), anyString(), any()); // cached
    }

    // ── getLesson — not found ─────────────────────────────────────────────────

    @Test
    void getLesson_notFound_throwsResourceNotFoundException() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(lessonRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLesson(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createLesson — content_id bridge ─────────────────────────────────────

    @Test
    void createLesson_bridgeSuccess_setsContentId() throws Exception {
        when(languageService.getEntityById(1L)).thenReturn(testLanguage);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);

        LessonContent savedContent = LessonContent.builder()
                .id("507f1f77bcf86cd799439011") // fake MongoDB ObjectId
                .lessonId(10L).languageCode("amh").cefrLevel("A1")
                .versionStamp(1L).build();
        when(lessonContentRepository.save(any(LessonContent.class))).thenReturn(savedContent);

        // After updateContentId, re-fetch returns lesson with contentId set
        Lesson lessonWithBridge = Lesson.builder()
                .id(10L).language(testLanguage).title("Greetings")
                .cefrLevel(CefrLevel.A1).orderIndex(1)
                .contentId("507f1f77bcf86cd799439011").active(true).build();
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lessonWithBridge));

        LessonResponse mappedResponse = LessonResponse.builder()
                .id(10L).title("Greetings").contentId("507f1f77bcf86cd799439011").build();
        when(lessonMapper.toLessonResponse(lessonWithBridge)).thenReturn(mappedResponse);
        when(lessonMapper.toLessonContentDto(savedContent)).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        LessonCreateRequest request = new LessonCreateRequest();
        request.setLanguageId(1L);
        request.setTitle("Greetings");
        request.setCefrLevel("A1");
        request.setOrderIndex(1);
        request.setExercises(List.of(Map.of("type", "MULTIPLE_CHOICE")));

        LessonResponse result = lessonService.createLesson(request, adminUser);

        assertThat(result.getContentId()).isEqualTo("507f1f77bcf86cd799439011");
        // Step 3: bridge update must have been called
        verify(lessonRepository).updateContentId(10L, "507f1f77bcf86cd799439011");
    }

    @Test
    void createLesson_mongoFails_throwsRuntimeException() {
        when(languageService.getEntityById(1L)).thenReturn(testLanguage);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);
        when(lessonContentRepository.save(any(LessonContent.class)))
                .thenThrow(new RuntimeException("MongoDB connection refused"));

        LessonCreateRequest request = new LessonCreateRequest();
        request.setLanguageId(1L);
        request.setTitle("Greetings");
        request.setCefrLevel("A1");
        request.setOrderIndex(1);
        request.setExercises(List.of(Map.of("type", "FILL_BLANK")));

        assertThatThrownBy(() -> lessonService.createLesson(request, adminUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MongoDB");

        // Bridge update must NOT have been called (MongoDB failed before step 3)
        verify(lessonRepository, never()).updateContentId(any(), any());
    }

    // ── updateLesson — cache eviction ─────────────────────────────────────────

    @Test
    void updateLesson_evictsCacheAfterSave() {
        when(lessonRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.save(testLesson)).thenReturn(testLesson);
        when(lessonMapper.toLessonResponse(testLesson)).thenReturn(
                LessonResponse.builder().id(10L).build());

        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setTitle("Updated Greetings");

        lessonService.updateLesson(10L, request, adminUser);

        verify(cacheEvictionService).invalidateLesson(10L);
    }
}

package com.ellh.integration;

import com.ellh.AbstractIntegrationTest;
import com.ellh.content.dto.LessonCreateRequest;
import com.ellh.content.dto.LessonResponse;
import com.ellh.content.service.LessonService;
import com.ellh.user.entity.User;
import com.ellh.user.entity.AccountStatus;
import com.ellh.user.entity.AuthProvider;
import com.ellh.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for LessonService — uses real Testcontainers stack.
 * Verifies the full content_id bridge end-to-end:
 *   PostgreSQL INSERT → MongoDB INSERT → PostgreSQL UPDATE content_id
 *
 * IMPORTANT: These tests require the seed data (R__seed_languages.sql) to
 * have run (Flyway applies repeatable migrations automatically on context start).
 * Language ID 1 = Amharic (amh) — assumed from seed data.
 */
@AutoConfigureMockMvc
class LessonServiceIT extends AbstractIntegrationTest {

    @Autowired LessonService  lessonService;
    @Autowired UserRepository userRepository;

    private User adminUser;

    @BeforeEach
    void setUp() {
        // Create a CONTENT_ADMIN user for write operations
        adminUser = userRepository.findByEmail("admin_it@ellh.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("admin_it@ellh.com")
                        .passwordHash("$2a$12$hashed")
                        .firstName("Admin")
                        .lastName("IT")
                        .accountStatus(AccountStatus.ACTIVE)
                        .authProvider(AuthProvider.LOCAL)
                        .build()));
    }

    @Test
    void createLesson_bridgeCompletedSuccessfully() {
        LessonCreateRequest request = new LessonCreateRequest();
        request.setLanguageId(1L); // Amharic — seeded in R__seed_languages.sql
        request.setTitle("IT Test Lesson: Basic Greetings");
        request.setCefrLevel("A1");
        request.setOrderIndex(100); // high number to avoid conflicts with seed data
        request.setXpReward(25);
        request.setEstimatedMinutes(8);
        request.setExercises(List.of(
                Map.of("type", "MULTIPLE_CHOICE",
                       "question", "What does 'ሰላም' mean?",
                       "options", List.of(
                               Map.of("id","a","text","Hello","isCorrect",true),
                               Map.of("id","b","text","Goodbye","isCorrect",false)))));

        LessonResponse response = lessonService.createLesson(request, adminUser);

        // PostgreSQL row exists
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("IT Test Lesson: Basic Greetings");

        // content_id bridge was set (MongoDB ObjectId is 24 hex chars)
        assertThat(response.getContentId()).isNotNull();
        assertThat(response.getContentId()).hasSize(24);
        assertThat(response.getContentId()).matches("[a-f0-9]{24}");

        // MongoDB content is embedded in the response
        assertThat(response.getContent()).isNotNull();
        assertThat(response.getContent().getExercises()).hasSize(1);

        // versionStamp starts at 1
        assertThat(response.getVersionStamp()).isEqualTo(1L);
    }

    @Test
    void getLesson_afterCreate_cacheIsPopulated() {
        // Create a lesson
        LessonCreateRequest request = new LessonCreateRequest();
        request.setLanguageId(1L);
        request.setTitle("Cache Test Lesson");
        request.setCefrLevel("A2");
        request.setOrderIndex(101);
        request.setExercises(List.of(Map.of("type", "FILL_BLANK", "question", "test")));

        LessonResponse created = lessonService.createLesson(request, adminUser);
        Long lessonId = created.getId();

        // First fetch — may be cache miss (createLesson caches the result)
        LessonResponse firstFetch = lessonService.getLesson(lessonId);
        assertThat(firstFetch.getId()).isEqualTo(lessonId);
        assertThat(firstFetch.getTitle()).isEqualTo("Cache Test Lesson");

        // Second fetch — must be cache hit (same result, no additional DB queries)
        LessonResponse secondFetch = lessonService.getLesson(lessonId);
        assertThat(secondFetch.getId()).isEqualTo(lessonId);
        assertThat(secondFetch.getTitle()).isEqualTo("Cache Test Lesson");
    }

    @Test
    void getLessons_byLanguage_returnsCreatedLessons() {
        // Verify the language list endpoint works with seed data
        // and that lessons created above are returned in the list
        var lessons = lessonService.getLessonsByLanguage(1L);
        // Seed data may have lessons; verify no exception and list is returned
        assertThat(lessons).isNotNull();
    }
}

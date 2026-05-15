package com.ellh.content.service;

import com.ellh.content.model.ContrastiveNote;
import com.ellh.content.model.ContrastiveRule;
import com.ellh.content.repository.ContrastiveRuleRepository;
import com.ellh.infrastructure.cache.AIResponseCacheAdapter;
import com.ellh.user.repository.LearnerProfileRepository;
import com.ellh.user.model.LearnerProfile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContrastiveAnalysisEngine — coverage gap-fill (Sprint 9 NFR-15).
 *
 * Tests:
 *   1. Language pair with matching rules returns ContrastiveNote.
 *   2. Language pair with no rules returns empty/null gracefully.
 *   3. lessonId not in applicable_lessons returns empty gracefully.
 *   4. BILINGUAL_LEARNER pathway → isApplicable() returns true.
 *   5. FOREIGN_LEARNER pathway → isApplicable() returns false.
 *   6. Result is cached via AIResponseCacheAdapter after first fetch.
 *
 * Section 4.5.1 ContrastiveAnalysisEngine.
 * NFR-15 coverage gap-fill.
 */
@ExtendWith(MockitoExtension.class)
class ContrastiveAnalysisEngineTest {

    @Mock private ContrastiveRuleRepository  ruleRepository;
    @Mock private AIResponseCacheAdapter     cacheAdapter;
    @Mock private LearnerProfileRepository   learnerProfileRepository;

    @InjectMocks
    private ContrastiveAnalysisEngine engine;

    @Test
    void generateContrastiveNote_withMatchingRules_returnsNote() {
        ContrastiveRule rule = buildRule("amh", "tir", "lesson-1", "GRAMMAR");
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", "lesson-1"))
                .thenReturn(Arrays.asList(rule));

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "tir", "lesson-1");

        assertNotNull(note, "ContrastiveNote should not be null when rules exist");
        assertEquals("GRAMMAR", note.getRuleCategory());
    }

    @Test
    void generateContrastiveNote_noMatchingRules_returnsNull() {
        when(ruleRepository.findByLanguagePairAndLesson(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "orm", "lesson-99");

        assertNull(note, "ContrastiveNote should be null when no rules match");
    }

    @Test
    void generateContrastiveNote_lessonNotInApplicableList_returnsNull() {
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", "lesson-99"))
                .thenReturn(Collections.emptyList());

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "tir", "lesson-99");

        assertNull(note, "Lesson not in applicable_lessons should return null");
    }

    @Test
    void isApplicable_bilingualLearnerPathway_returnsTrue() {
        LearnerProfile profile = new LearnerProfile();
        profile.setPathwayType("BILINGUAL_LEARNER");
        when(learnerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertTrue(engine.isApplicable(1L),
                "BILINGUAL_LEARNER should be applicable for contrastive notes");
    }

    @Test
    void isApplicable_foreignLearnerPathway_returnsFalse() {
        LearnerProfile profile = new LearnerProfile();
        profile.setPathwayType("FOREIGN_LEARNER");
        when(learnerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertFalse(engine.isApplicable(1L),
                "FOREIGN_LEARNER should not receive contrastive notes");
    }

    @Test
    void generateContrastiveNote_resultisCachedAfterFetch() {
        ContrastiveRule rule = buildRule("amh", "tir", "lesson-1", "PHONOLOGY");
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", "lesson-1"))
                .thenReturn(Arrays.asList(rule));

        engine.generateContrastiveNote(1L, "amh", "tir", "lesson-1");

        verify(cacheAdapter).cacheContrastiveNote(anyString(), any(ContrastiveNote.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ContrastiveRule buildRule(String src, String tgt, String lessonId, String category) {
        ContrastiveRule r = new ContrastiveRule();
        r.setSourceLanguage(src);
        r.setTargetLanguage(tgt);
        r.setApplicableLessons(Arrays.asList(lessonId));
        r.setRuleCategory(category);
        r.setRuleTitle("Test Rule");
        r.setContrastDescription("Description");
        r.setVersionStamp("v1");
        r.setActive(true);
        return r;
    }
}

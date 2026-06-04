package com.ellh.content.service;

import com.ellh.learning.document.ContrastiveRule;
import com.ellh.learning.dto.ContrastiveNote;
import com.ellh.learning.repository.ContrastiveRuleRepository;
import com.ellh.learning.service.ContrastiveAnalysisEngine;
import com.ellh.infrastructure.cache.AIResponseCacheAdapter;
import com.ellh.user.repository.LearnerProfileRepository;
import com.ellh.user.entity.LearnerProfile;
import com.ellh.user.entity.PathwayType;

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

@ExtendWith(MockitoExtension.class)
class ContrastiveAnalysisEngineTest {

    @Mock private ContrastiveRuleRepository  ruleRepository;
    @Mock private AIResponseCacheAdapter     cacheAdapter;
    @Mock private LearnerProfileRepository   learnerProfileRepository;

    @InjectMocks
    private ContrastiveAnalysisEngine engine;

    @Test
    void generateContrastiveNote_withMatchingRules_returnsNote() {
        ContrastiveRule rule = buildRule("amh", "tir", 1L, "GRAMMAR");
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", 1L))
                .thenReturn(Arrays.asList(rule));

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "tir", 1L);

        assertNotNull(note, "ContrastiveNote should not be null when rules exist");
        assertEquals("GRAMMAR", note.getRuleCategory());
    }

    @Test
    void generateContrastiveNote_noMatchingRules_returnsNull() {
        when(ruleRepository.findByLanguagePairAndLesson(anyString(), anyString(), anyLong()))
                .thenReturn(Collections.emptyList());

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "orm", 99L);

        assertNull(note, "ContrastiveNote should be null when no rules match");
    }

    @Test
    void generateContrastiveNote_lessonNotInApplicableList_returnsNull() {
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", 99L))
                .thenReturn(Collections.emptyList());

        ContrastiveNote note = engine.generateContrastiveNote(1L, "amh", "tir", 99L);

        assertNull(note, "Lesson not in applicable_lessons should return null");
    }

    @Test
    void isApplicable_bilingualLearnerPathway_returnsTrue() {
        LearnerProfile profile = new LearnerProfile();
        profile.setPathwayType(PathwayType.BILINGUAL_LEARNER);
        when(learnerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertTrue(engine.isApplicable(1L),
                "BILINGUAL_LEARNER should be applicable for contrastive notes");
    }

    @Test
    void isApplicable_foreignLearnerPathway_returnsFalse() {
        LearnerProfile profile = new LearnerProfile();
        profile.setPathwayType(PathwayType.FOREIGN_LEARNER);
        when(learnerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertFalse(engine.isApplicable(1L),
                "FOREIGN_LEARNER should not receive contrastive notes");
    }

    @Test
    void generateContrastiveNote_resultIsCachedAfterFetch() {
        ContrastiveRule rule = buildRule("amh", "tir", 1L, "PHONOLOGY");
        when(ruleRepository.findByLanguagePairAndLesson("amh", "tir", 1L))
                .thenReturn(Arrays.asList(rule));

        engine.generateContrastiveNote(1L, "amh", "tir", 1L);

        verify(cacheAdapter).cacheContrastiveNote(anyString(), any(ContrastiveNote.class));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ContrastiveRule buildRule(String src, String tgt, Long lessonId, String category) {
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

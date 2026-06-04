package com.ellh.learning.service;

import com.ellh.infrastructure.cache.AIResponseCacheAdapter;
import com.ellh.learning.document.ContrastiveRule;
import com.ellh.learning.dto.ContrastiveNote;
import com.ellh.learning.repository.ContrastiveRuleRepository;
import com.ellh.user.entity.LearnerProfile;
import com.ellh.user.repository.LearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContrastiveAnalysisEngine {

    private final ContrastiveRuleRepository ruleRepository;
    private final AIResponseCacheAdapter cacheAdapter;
    private final LearnerProfileRepository learnerProfileRepository;

    /**
     * Generates a contrastive note for a given user, language pair, and lesson.
     * @param lessonId  numeric lesson ID (Long) as stored in the database.
     * @return ContrastiveNote or null if no matching rules are found.
     */
    public ContrastiveNote generateContrastiveNote(Long userId, String sourceLang,
                                                    String targetLang, Long lessonId) {
        List<ContrastiveRule> rules = ruleRepository
                .findByLanguagePairAndLesson(sourceLang, targetLang, lessonId);
        if (rules.isEmpty()) {
            return null;
        }
        ContrastiveRule rule = rules.get(0);
        ContrastiveNote note = new ContrastiveNote();
        note.setRuleCategory(rule.getRuleCategory());
        note.setRuleTitle(rule.getRuleTitle());
        note.setContrastDescription(rule.getContrastDescription());
        note.setSourceLanguage(rule.getSourceLanguage());
        note.setTargetLanguage(rule.getTargetLanguage());
        note.setVersionStamp(rule.getVersionStamp());

        String cacheKey = sourceLang + ":" + targetLang + ":" + lessonId;
        cacheAdapter.cacheContrastiveNote(cacheKey, note);
        return note;
    }

    /**
     * Checks if contrastive analysis is applicable for the given user.
     * Applicable only for BILINGUAL_LEARNER pathway.
     */
    public boolean isApplicable(Long userId) {
        Optional<LearnerProfile> profile = learnerProfileRepository.findByUserId(userId);
        return profile.isPresent() && "BILINGUAL_LEARNER".equals(profile.get().getPathwayType());
    }
}
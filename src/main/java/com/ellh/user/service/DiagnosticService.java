package com.ellh.user.service;

import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.learning.entity.LearnerLanguage;
import com.ellh.learning.repository.LearnerLanguageRepository;
import com.ellh.user.entity.CefrLevel;
import com.ellh.user.entity.DiagnosticAssessment;
import com.ellh.user.entity.LearnerProfile;
import com.ellh.user.entity.User;
import com.ellh.user.repository.DiagnosticAssessmentRepository;
import com.ellh.user.repository.LearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiagnosticService {

    private final DiagnosticAssessmentRepository diagnosticRepository;
    private final LearnerProfileRepository profileRepository;
    private final LanguageRepository languageRepository;
    private final LearnerLanguageRepository learnerLanguageRepository;

    @Transactional
    public DiagnosticAssessment submitAssessment(
            User user, 
            Map<String, String> responses, 
            Map<String, Boolean> knowledgeFlags) {

        // 1. Evaluate responses and calculate CEFR placement [1.8]
        DiagnosticAssessment.AssessmentResult result = DiagnosticAssessment.evaluate(responses, knowledgeFlags);

        DiagnosticAssessment assessment = DiagnosticAssessment.builder()
                .user(user)
                .responses(responses)
                .assignedPathway(result.pathwayType())
                .assignedCefrLevel(result.cefrLevel())
                .totalScore(result.score())
                .languageKnowledgeFlags(knowledgeFlags)
                .completedAt(Instant.now())
                .build();

        diagnosticRepository.save(assessment);

        // 2. Update LearnerProfile
        LearnerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("LearnerProfile", user.getId()));

        profile.setPathwayType(result.pathwayType());
        profile.setCurrentCefrLevel(result.cefrLevel());
        profile.setDiagnosticScore(result.score());
        profile.setOnboardingComplete(true);
        profileRepository.save(profile);

        // 3. Register the target learning language to start their path [1.9]
        if (profile.getTargetLanguageId() != null) {
            Language targetLang = languageRepository.findById(profile.getTargetLanguageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Language", profile.getTargetLanguageId()));

            LearnerLanguage learnerLanguage = LearnerLanguage.builder()
                    .user(user)
                    .language(targetLang)
                    .cefrLevel(result.cefrLevel())
                    .isActive(true)
                    .build();
            learnerLanguageRepository.save(learnerLanguage);
        }

        return assessment;
    }
}

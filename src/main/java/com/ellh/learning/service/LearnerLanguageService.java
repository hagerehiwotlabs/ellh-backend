package com.ellh.learning.service;

import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.learning.entity.LearnerLanguage;
import com.ellh.learning.repository.LearnerLanguageRepository;
import com.ellh.user.entity.CefrLevel;
import com.ellh.user.entity.User;
import com.ellh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LearnerLanguageService {

    private final LearnerLanguageRepository learnerLanguageRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;

    /**
     * Saves and registers a set of languages the learner wants to learn.
     * Aligns with the V12__create_learner_languages.sql schema.
     */
    public void saveLearnerLanguages(Long userId, Set<Long> languageIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        for (Long langId : languageIds) {
            Language language = languageRepository.findById(langId)
                    .orElseThrow(() -> new ResourceNotFoundException("Language", langId));

            // Verify if already registered to prevent duplicate key constraint violations [2]
            boolean alreadyRegistered = false;
            List<LearnerLanguage> existing = learnerLanguageRepository.findAll();
            for (LearnerLanguage ll : existing) {
                if (ll.getUser().getId().equals(userId) && ll.getLanguage().getId().equals(langId)) {
                    alreadyRegistered = true;
                    break;
                }
            }

            if (!alreadyRegistered) {
                LearnerLanguage learnerLanguage = LearnerLanguage.builder()
                        .user(user)
                        .language(language)
                        .cefrLevel(CefrLevel.A1) // Default starting level
                        .isActive(true)
                        .build();
                learnerLanguageRepository.save(learnerLanguage);
                log.info("Registered languageId={} for userId={}", langId, userId);
            }
        }
    }
}

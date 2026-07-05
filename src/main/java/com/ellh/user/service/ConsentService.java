package com.ellh.user.service;

import com.ellh.user.entity.User;
import com.ellh.user.entity.UserConsent;
import com.ellh.user.repository.UserConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository consentRepository;

    /**
     * Records immutable, legally binding consent logs at the exact moment of registration.
     */
    @Transactional
    public void recordRegistrationConsents(User user, boolean privacy, boolean data, boolean audio, String ipAddress) {
        if (privacy) recordSingleConsent(user, "PRIVACY_POLICY", ipAddress);
        if (data) recordSingleConsent(user, "DATA_COLLECTION", ipAddress);
        if (audio) recordSingleConsent(user, "AUDIO_RECORDING", ipAddress);
        
        log.info("GDPR consents legally recorded for User ID: {} from IP: {}", user.getId(), ipAddress);
    }

    private void recordSingleConsent(User user, String type, String ipAddress) {
        UserConsent consent = UserConsent.builder()
                .user(user)
                .consentType(type)
                .policyVersion("v1.0")
                .ipAddress(ipAddress)
                .build();
        consentRepository.save(consent);
    }
}

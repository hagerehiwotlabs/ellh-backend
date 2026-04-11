package com.ellh.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Firebase Admin SDK initialisation.
 * Section 4.5.1 — Infrastructure Domain (NotificationService depends on this).
 * Section 4.5.4.1 — ND-09 Firebase Cloud Messaging.
 *
 * The Firebase service account JSON is stored as a base64-encoded environment
 * variable (FIREBASE_SERVICE_ACCOUNT_B64) — never committed to git.
 *
 * Encoding command (Phase 0 guide Section 1.6 Step 2):
 *   base64 -i firebase-service-account.json | tr -d '\n'
 *
 * Initialisation is skipped in test profile (no env var set) so integration
 * tests do not fail when Firebase credentials are absent.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-b64:}")
    private String serviceAccountB64;

    @PostConstruct
    public void initFirebase() {
        // Skip if already initialised (Spring context refresh / hot reload)
        if (!FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp already initialised — skipping");
            return;
        }

        if (!StringUtils.hasText(serviceAccountB64)) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_B64 not set. " +
                     "Firebase push notifications will be unavailable. " +
                     "This is expected in test and local-dev profiles.");
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountB64.trim());
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(decoded));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialised successfully");

        } catch (Exception e) {
            // Log as ERROR but do not crash the application — FCM is non-critical
            // for core learning features. NotificationService handles the null case.
            log.error("Failed to initialise Firebase Admin SDK: {}", e.getMessage());
        }
    }
}

package com.ellh.content.repository;

import java.time.LocalDateTime;

/**
 * Custom repository methods not supported by query derivation.
 */
public interface ContentUpdateLogDocumentRepositoryCustom {

    /**
     * Record a GDPR purge event in the content_update_logs collection.
     *
     * @param deletedAttempts number of pronunciation/translation attempts deleted
     * @param deletedUsers    number of user records fully deleted
     * @param timestamp       when the purge was executed
     */
    void logGdprPurge(int deletedAttempts, int deletedUsers, LocalDateTime timestamp);
}

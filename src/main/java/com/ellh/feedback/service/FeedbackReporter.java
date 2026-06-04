package com.ellh.feedback.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Central reporting service for non‑critical events (orphans, cache alerts, etc.).
 * Used by scheduled maintenance jobs.
 */
@Service
@Slf4j
public class FeedbackReporter {

    /**
     * Report content orphans found during cleanup.
     * @param count   number of orphan documents
     * @param details human‑readable summary of what was removed
     */
    public void reportContentOrphans(int count, String details) {
        log.warn("Content orphans detected: {} documents. Details: {}", count, details);
        // Future improvement: create a FeedbackReport entry in the database
    }

    /**
     * Report Redis cache capacity alerts.
     * @param currentSize  current number of entries
     * @param maxSize      maximum allowed entries
     * @param percentUsed  percentage of cache used
     */
    public void reportCacheAlert(int currentSize, int maxSize, double percentUsed) {
        log.warn("Redis cache alert: {}/{} entries ({}% used)", currentSize, maxSize, percentUsed);
    }
}

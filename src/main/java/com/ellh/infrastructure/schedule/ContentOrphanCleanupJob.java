package com.ellh.infrastructure.schedule;

import com.ellh.content.repository.LessonContentRepository;
import com.ellh.content.repository.LessonRepository;
import com.ellh.feedback.service.FeedbackReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Weekly scheduled job to clean up orphaned MongoDB lesson_content documents.
 *
 * An orphaned document is one in the lesson_content MongoDB collection that has
 * no corresponding row in the PostgreSQL lessons table (i.e., lessons.content_id
 * does not reference the MongoDB ObjectId).
 *
 * Orphans accumulate when:
 *   - A Spring Boot crash occurs between MongoDB insert and PostgreSQL insert
 *     during lesson creation (known risk from Section 4.4 Trade-off b).
 *   - Content is deleted from PostgreSQL but MongoDB cleanup fails.
 *
 * The job logs orphan ObjectIds and alerts via FeedbackReporter (severity=MEDIUM)
 * rather than deleting immediately — a human reviewer should confirm before purge.
 * Deletion is logged to content_update_logs for audit.
 *
 * This job was explicitly planned in Sprint 2: "schedule a weekly cleanup job
 * in Sprint 9" (Section 4.4 Sprint 2 Key Risks).
 *
 * Section 4.4 Sprint 2 (key risk mitigation), Trade-off b, Section 4.5.2.6.
 */
@Component
public class ContentOrphanCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ContentOrphanCleanupJob.class);

    private final LessonRepository        lessonRepository;
    private final LessonContentRepository lessonContentRepository;
    private final FeedbackReporter        feedbackReporter;

    public ContentOrphanCleanupJob(LessonRepository lessonRepository,
                                   LessonContentRepository lessonContentRepository,
                                   FeedbackReporter feedbackReporter) {
        this.lessonRepository        = lessonRepository;
        this.lessonContentRepository = lessonContentRepository;
        this.feedbackReporter        = feedbackReporter;
    }

    /**
     * Runs every Sunday at 04:00 UTC.
     *
     * Finds MongoDB lesson_content documents whose _id is not referenced by
     * any row in PostgreSQL lessons.content_id.
     *
     * Reports orphans via FeedbackReporter (MEDIUM severity).
     * Does NOT auto-delete — human review required before purge.
     * A follow-up job (future v2.0) will auto-delete after 7-day confirmation window.
     */
    @Scheduled(cron = "0 0 4 * * SUN", zone = "UTC")
    public void findOrphanedLessonContent() {
        log.info("ContentOrphanCleanupJob: starting weekly orphan scan at {}", LocalDateTime.now());

        // Step 1: Get all content_ids from PostgreSQL lessons table
        Set<String> activeContentIds = lessonRepository.findAllContentIds()
                .stream()
                .collect(Collectors.toSet());

        // Step 2: Get all _id values from MongoDB lesson_content collection
        List<String> allMongoIds = lessonContentRepository.findAllIds();

        // Step 3: Find orphans (MongoDB IDs not in PostgreSQL content_ids)
        List<String> orphanIds = allMongoIds.stream()
                .filter(id -> !activeContentIds.contains(id))
                .collect(Collectors.toList());

        if (orphanIds.isEmpty()) {
            log.info("ContentOrphanCleanupJob: no orphaned documents found — system clean");
            return;
        }

        // Step 4: Alert via FeedbackReporter (MEDIUM severity)
        String orphanList = String.join(", ", orphanIds.subList(0,
                Math.min(orphanIds.size(), 20))); // truncate at 20 for log readability
        log.warn("ContentOrphanCleanupJob: found {} orphaned lesson_content documents: {}{}",
                orphanIds.size(), orphanList,
                orphanIds.size() > 20 ? " (truncated)" : "");

        feedbackReporter.reportContentOrphans(orphanIds.size(), orphanList);

        log.info("ContentOrphanCleanupJob: orphan scan complete — {} documents reported",
                orphanIds.size());
    }
}

package com.ellh.feedback.repository;

import com.ellh.feedback.document.AIServiceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for the ai_service_logs collection.
 * Section 4.5.2.4 — ai_service_logs collection (90-day TTL via index).
 *
 * IMPORTANT: The TTL index on created_at automatically deletes documents
 * older than 90 days — no manual cleanup required for this collection.
 */
@Repository
public interface AIServiceLogRepository extends MongoRepository<AIServiceLog, String> {

    Page<AIServiceLog> findByUserId(Long userId, Pageable pageable);

    List<AIServiceLog> findByProviderAndStatusAndCreatedAtAfter(
            String provider, String status, Instant after);

    /** Used by FeedbackReporter to check AI failure rate over a time window. */
    long countByProviderAndStatusAndCreatedAtAfter(
            String provider, String status, Instant after);
}

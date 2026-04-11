package com.ellh.feedback.repository;

import com.ellh.feedback.entity.FeedbackReport;
import com.ellh.feedback.entity.FeedbackSeverity;
import com.ellh.feedback.entity.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Section 4.5.2.3 — idx_feedback_status_severity (composite B-tree).
 * Admin dashboard queries filter by status + severity simultaneously.
 */
@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {

    /** Admin dashboard: open reports ordered by severity then submission date. */
    Page<FeedbackReport> findByStatus(FeedbackStatus status, Pageable pageable);

    Page<FeedbackReport> findByStatusAndSeverity(
            FeedbackStatus status, FeedbackSeverity severity, Pageable pageable);

    List<FeedbackReport> findByUserId(Long userId);

    /** Update status and resolution notes — called by ContentAdmin. */
    @Modifying
    @Query("UPDATE FeedbackReport f SET f.status = :status, " +
           "f.resolutionNotes = :notes, f.resolvedAt = CURRENT_TIMESTAMP " +
           "WHERE f.id = :id")
    void resolveReport(
            @Param("id") Long id,
            @Param("status") FeedbackStatus status,
            @Param("notes") String notes);
}

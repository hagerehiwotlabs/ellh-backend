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

@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {

    Page<FeedbackReport> findByStatus(FeedbackStatus status, Pageable pageable);

    Page<FeedbackReport> findByStatusAndSeverity(
            FeedbackStatus status, FeedbackSeverity severity, Pageable pageable);

    List<FeedbackReport> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE FeedbackReport f SET f.status = :status, " +
           "f.resolutionNotes = :notes, f.resolvedAt = CURRENT_TIMESTAMP " +
           "WHERE f.id = :id")
    void resolveReport(
            @Param("id") Long id,
            @Param("status") FeedbackStatus status,
            @Param("notes") String notes);

    /** Anonymise all reports submitted by a user (set user reference to null). */
    @Modifying
    @Query("UPDATE FeedbackReport fr SET fr.user = null WHERE fr.user.id = :userId")
    void anonymiseByUserId(@Param("userId") Long userId);
}

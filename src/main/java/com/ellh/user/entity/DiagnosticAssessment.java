package com.ellh.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Records onboarding diagnostic assessment results.
 * Section 4.5.1 — User Domain; Section 4.5.2.2 — diagnostic_assessments table.
 *
 * evaluateAndAssignPathway() is the core scoring method: it totals the responses,
 * derives a CefrLevel and PathwayType, then updates LearnerProfile.
 *
 * responses and language_knowledge_flags are JSONB — flexible structure
 * without a rigid schema per Trade-off b rationale.
 */
@Entity
@Table(name = "diagnostic_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UNIQUE: one assessment per user. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * JSONB: serialised question-answer pairs from the assessment flow.
     * Structure: Map<String questionId, String answerValue>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responses", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> responses;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_pathway", nullable = false, length = 20)
    private PathwayType assignedPathway;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_cefr_level", nullable = false, length = 5)
    private CefrLevel assignedCefrLevel;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    /**
     * JSONB: per-language familiarity indicators used by bilingual routing.
     * Structure: Map<String isoCode, Boolean hasFamiliarity>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_knowledge_flags", columnDefinition = "jsonb")
    private Map<String, Boolean> languageKnowledgeFlags;

    @Column(name = "completed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant completedAt = Instant.now();

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Scoring thresholds (Section 4.5.1 — DiagnosticAssessment.evaluateAndAssignPathway).
     * Score 0–39  → A1 FOREIGN_LEARNER
     * Score 40–69 → A2 FOREIGN_LEARNER
     * Score 70–100 → B1 FOREIGN_LEARNER (or BILINGUAL_LEARNER if flags present)
     *
     * If languageKnowledgeFlags contains any true value → BILINGUAL_LEARNER.
     */
    public static AssessmentResult evaluate(
            Map<String, String> responses,
            Map<String, Boolean> languageKnowledgeFlags) {

        // Each correct answer scores 10 points; 10 questions → max 100
        long correctCount = responses.values().stream()
                .filter("correct"::equalsIgnoreCase)
                .count();
        int score = (int) (correctCount * 10);

        CefrLevel cefr;
        if (score >= 70) cefr = CefrLevel.B1;
        else if (score >= 40) cefr = CefrLevel.A2;
        else cefr = CefrLevel.A1;

        boolean isBilingual = languageKnowledgeFlags != null
                && languageKnowledgeFlags.values().stream().anyMatch(Boolean.TRUE::equals);

        PathwayType pathway = isBilingual
                ? PathwayType.BILINGUAL_LEARNER
                : PathwayType.FOREIGN_LEARNER;

        return new AssessmentResult(score, cefr, pathway);
    }

    /** Immutable result record returned by evaluate(). */
    public record AssessmentResult(int score, CefrLevel cefrLevel, PathwayType pathwayType) {}
}

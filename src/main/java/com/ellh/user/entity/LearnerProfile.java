package com.ellh.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Learner-specific configuration: native language, target language,
 * pathway type, CEFR placement, and daily learning goal.
 * Section 4.5.1 — User Domain; Section 4.5.2.2 — learner_profiles table.
 *
 * 1:1 composition with User — only FOREIGN_LEARNER and BILINGUAL_LEARNER
 * users have a LearnerProfile. ContentAdmin and SystemAdmin do not.
 */
@Entity
@Table(name = "learner_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → users.id — UNIQUE enforces the 1:1 relationship. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * FK → languages.id — the learner's primary known language.
     * Nullable: foreign learners with no Ethiopian language background
     * will not have a native language set.
     */
    @Column(name = "native_language_id")
    private Long nativeLanguageId;

    /** FK → languages.id — the language being studied. NOT NULL. */
    @Column(name = "target_language_id", nullable = false)
    private Long targetLanguageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pathway_type", nullable = false, length = 20)
    private PathwayType pathwayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_cefr_level", nullable = false, length = 5)
    @Builder.Default
    private CefrLevel currentCefrLevel = CefrLevel.A1;

    @Column(name = "onboarding_complete", nullable = false)
    @Builder.Default
    private boolean onboardingComplete = false;

    @Column(name = "daily_goal_minutes", nullable = false)
    @Builder.Default
    private int dailyGoalMinutes = 10;

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private boolean notificationsEnabled = true;

    /** Aggregate score from DiagnosticAssessment (0–100). Null until assessed. */
    @Column(name = "diagnostic_score")
    private Integer diagnosticScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

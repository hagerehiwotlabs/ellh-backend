package com.ellh.user.entity;

/**
 * Learner pathway determined by DiagnosticAssessment.
 * Section 4.5.1 — LearnerProfile.pathwayType.
 * Trade-off k: ContrastiveAnalysisEngine only activates for BILINGUAL_LEARNER.
 */
public enum PathwayType {
    FOREIGN_LEARNER,
    BILINGUAL_LEARNER
}

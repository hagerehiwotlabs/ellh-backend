package com.ellh.practice.entity;

import com.ellh.content.entity.Language;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * PracticeMode entity — represents available practice exercise types.
 *
 * Stores metadata about practice modes (vocabulary drills, listening exercises, etc.)
 * that learners can participate in. Each mode belongs to a language and has
 * difficulty levels.
 */
@Entity
@Table(name = "practice_modes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String type;  // VOCABULARY, LISTENING, SPEAKING, GRAMMAR, WRITING

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(nullable = false, length = 20)
    private String difficulty;  // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

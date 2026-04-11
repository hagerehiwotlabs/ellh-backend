package com.ellh.content.entity;

import com.ellh.user.entity.CefrLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * A single structured learning unit within the ELLH curriculum.
 * Section 4.5.1 — Content Domain; Section 4.5.2.2 — lessons table.
 *
 * Lesson metadata lives in PostgreSQL (title, level, order, XP).
 * Full lesson content lives in MongoDB lesson_content collection.
 * The content_id column is the cross-store bridge: lessons.content_id
 * stores the MongoDB ObjectId as a 24-char hex string.
 * Section 4.5.2.6 — Cross-Store Consistency: content_id bridge pattern.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false, length = 5)
    private CefrLevel cefrLevel;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private int xpReward = 25;

    /**
     * Cross-store bridge to MongoDB lesson_content collection.
     * VARCHAR(24) stores a MongoDB ObjectId hex string.
     * NULL until the MongoDB document is inserted (set atomically in LessonService).
     * Section 4.5.2.6 — content_id bridge.
     */
    @Column(name = "content_id", length = 24)
    private String contentId;

    /**
     * JSONB: List of prerequisite lesson IDs that must be COMPLETED
     * before this lesson unlocks. Null means no prerequisites.
     * Example: [1, 2, 3]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prerequisites", columnDefinition = "jsonb")
    private List<Long> prerequisites;

    @Column(name = "estimated_minutes", nullable = false)
    @Builder.Default
    private int estimatedMinutes = 8;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

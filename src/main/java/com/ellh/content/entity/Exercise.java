package com.ellh.content.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A single interactive task within a lesson.
 * Section 4.5.1 — Content Domain; Section 4.5.2.2 — exercises table.
 *
 * The exercise_type determines which UI component Android renders
 * and whether AI features are needed (PRONUNCIATION, TRANSLATION require SS-5).
 *
 * options is JSONB: [{id: string, text: string, isCorrect: boolean}]
 * Used only for MULTIPLE_CHOICE type; null for all other types.
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    /**
     * JSONB: [{id: string, text: string, isCorrect: boolean}]
     * Present only for MULTIPLE_CHOICE; null for other exercise types.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<Map<String, Object>> options;

    @Column(name = "hint_text", columnDefinition = "TEXT")
    private String hintText;

    /** Cloud storage URL for LISTENING exercise audio. */
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    /** Cloud storage URL for image-based exercise visual. */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "points", nullable = false)
    @Builder.Default
    private int points = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

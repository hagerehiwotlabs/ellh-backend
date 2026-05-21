package com.ellh.practice.entity;

import com.ellh.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * PracticeAnswer entity — stores individual question answers within a session.
 *
 * Each answer record:
 *  - Links to a practice session
 *  - Records the question ID
 *  - Stores user's answer
 *  - Tracks correctness (backend-verified)
 */
@Entity
@Table(name = "practice_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Column(name = "question_id", nullable = false, length = 36)
    private String questionId;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

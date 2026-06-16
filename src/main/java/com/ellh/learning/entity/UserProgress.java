package com.ellh.learning.entity;

import com.ellh.user.entity.User;
import com.ellh.content.entity.Lesson;
import com.ellh.content.entity.Exercise;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id", "exercise_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise; // Null for lesson-level progress

    @Column(nullable = false, length = 20)
    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED, MASTERED

    private Integer score;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
}

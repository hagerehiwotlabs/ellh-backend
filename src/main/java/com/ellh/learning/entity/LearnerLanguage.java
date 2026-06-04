package com.ellh.learning.entity;

import com.ellh.content.entity.Language;
import com.ellh.user.entity.User;
import com.ellh.user.entity.CefrLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "learner_languages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "language_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "language"})
public class LearnerLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    /**
     * Current CEFR level.  Defaults to 'A1' as in the migration.
     * NOTE: The DB CHECK constraint only allows 'A1','A2','B1'.
     * If your CefrLevel enum contains more values, you may need to adjust the CHECK.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false, length = 5)
    @Builder.Default
    private CefrLevel cefrLevel = CefrLevel.A1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "learning_start_date", nullable = false, updatable = false)
    @Builder.Default
    private Instant learningStartDate = Instant.now();

    @Column(name = "total_xp_earned", nullable = false)
    @Builder.Default
    private int totalXpEarned = 0;

    @Column(name = "lessons_completed", nullable = false)
    @Builder.Default
    private int lessonsCompleted = 0;

    @Column(name = "mastery_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal masteryPercent = BigDecimal.ZERO;

    @Column(name = "last_switch_date")
    private Instant lastSwitchDate;

    // The migration uses NOW() for learning_start_date, but JPA will set it via @PrePersist.
    @PrePersist
    protected void onCreate() {
        if (learningStartDate == null) {
            learningStartDate = Instant.now();
        }
    }
}

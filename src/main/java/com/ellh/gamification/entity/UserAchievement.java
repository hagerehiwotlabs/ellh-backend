package com.ellh.gamification.entity;

import com.ellh.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_achievements", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant awardedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress_data", columnDefinition = "jsonb")
    private Map<String, Object> progressData;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean completed = true;
}

package com.ellh.ai.entity;

import com.ellh.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "translation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "target_text", nullable = false, columnDefinition = "TEXT")
    private String targetText;

    @Column(name = "source_language", nullable = false, length = 3)
    private String sourceLanguage;

    @Column(name = "target_language", nullable = false, length = 3)
    private String targetLanguage;

    @Column(name = "cached_result", nullable = false)
    private boolean cachedResult;

    @Column(name = "processing_time_ms", nullable = false)
    private int processingTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

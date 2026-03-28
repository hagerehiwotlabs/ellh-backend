package com.ellh.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.net.InetAddress;
import java.time.Instant;

/**
 * Records privacy consent granted by a user at registration and on policy updates.
 * Section 4.5.1 — User Domain; Section 4.5.2.2 — user_consent table.
 * Section 4.5.2.7 — retained for 7 years even after account deletion (legal hold).
 *
 * UNIQUE constraint on (user_id, consent_type, policy_version) ensures one
 * record per consent type per policy version.
 */
@Entity
@Table(name = "user_consent",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "consent_type", "policy_version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Values: PRIVACY_POLICY | DATA_COLLECTION | AUDIO_RECORDING | RESEARCH_USE */
    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Column(name = "granted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    /** NULL while consent is active. Set when user revokes. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** IP address at consent grant time — required for legal audit trail. */
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    public boolean isActive() { return revokedAt == null; }
}

package com.ellh.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Base account entity — STI root for the users table.
 * Section 4.5.1 — User Domain, Section 4.5.2.1 — STI strategy.
 *
 * Single Table Inheritance with user_type discriminator column.
 * Subtypes: ForeignLearner, BilingualLearner, ContentAdmin, SystemAdmin
 * are distinguished by UserType enum — no separate tables required.
 *
 * Implements UserDetails so Spring Security can load principals directly
 * from the users table without a separate UserDetailsService lookup.
 *
 * Trade-off j — STI chosen over JTI because auth queries span all user types.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * STI discriminator — mapped as enum but stored as VARCHAR(30).
     * @Column(insertable=false, updatable=false) because the discriminator
     * is managed by JPA, not by application code.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", insertable = false, updatable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "last_active")
    private Instant lastActive;

    // ── UserDetails contract ──────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // userType may be null before JPA sets it from the discriminator column;
        // fall back to a safe default so Spring Security never NPEs on load.
        String role = (userType != null) ? userType.name() : "FOREIGN_LEARNER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        // Account is locked after 5 failed login attempts (Section 4.5.1)
        return failedLoginAttempts < 5 && accountStatus != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE
                || accountStatus == AccountStatus.PENDING_VERIFICATION;
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /** Called on each failed login attempt. Locks account at 5. */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /** Called on successful login — resets counter and updates last active. */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastActive = Instant.now();
    }
}

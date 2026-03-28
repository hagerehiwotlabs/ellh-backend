package com.ellh.user.repository;

import com.ellh.user.entity.User;
import com.ellh.user.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the users table.
 * Section 4.5.2.3 — idx_users_email (UNIQUE B-tree) makes findByEmail fast.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Primary lookup for authentication — indexed on email. */
    Optional<User> findByEmail(String email);

    /** Registration duplicate check. */
    boolean existsByEmail(String email);

    /** GDPR Step 1 — mark account inactive without deleting (grace period). */
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    void updateAccountStatus(@Param("id") Long id,
                              @Param("status") AccountStatus status);

    /** Increment failed login attempts atomically. */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(@Param("id") Long id);

    /** Reset failed attempts on successful login. */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :id")
    void resetFailedAttempts(@Param("id") Long id);
}

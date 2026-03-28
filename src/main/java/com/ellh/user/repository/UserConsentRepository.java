package com.ellh.user.repository;

import com.ellh.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    List<UserConsent> findByUserId(Long userId);

    /** GDPR: find all active consents for a user. */
    List<UserConsent> findByUserIdAndRevokedAtIsNull(Long userId);

    /** GDPR Step 3: anonymise — set user_id reference to null handled via cascade;
     *  here we mark all consents with a deletion timestamp for the 7-year hold. */
    @Modifying
    @Query("UPDATE UserConsent c SET c.revokedAt = :ts WHERE c.user.id = :userId AND c.revokedAt IS NULL")
    void revokeAllForUser(@Param("userId") Long userId, @Param("ts") Instant ts);
}

package com.ellh.user.repository;

import com.ellh.user.entity.User;
import com.ellh.user.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    void updateAccountStatus(@Param("id") Long id, @Param("status") AccountStatus status);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :id")
    void resetFailedAttempts(@Param("id") Long id);

    // ── GDPR / Account deletion ────────────────────────────────────────────────
    // WARNING: The following methods reference JPA entities that may not exist yet.
    // If a referenced entity is missing, compilation will fail.
    // You can comment out the offending methods until the entity classes are created.

    // @Modifying
    //  @Query("DELETE FROM PronunciationAttempt pa WHERE pa.user.id = :userId AND pa.retentionDate < :cutoff")
    // int deleteExpiredPronunciationAttempts(@Param("cutoff") LocalDateTime cutoff);

    // @Query("SELECT u.id FROM User u WHERE u.accountStatus = 'PENDING_DELETION' AND u.retentionDate < :now")
    // List<Long> findUsersReadyForFullDeletion(@Param("now") LocalDateTime now);

    // @Modifying
    // @Query("DELETE FROM TranslationRequest tr WHERE tr.user.id = :userId")
    // void deleteTranslationRequestsByUserId(@Param("userId") Long userId);

    // @Modifying
    // @Query("DELETE FROM UserProgress up WHERE up.user.id = :userId")
    // void deleteUserProgressByUserId(@Param("userId") Long userId);

    // @Modifying
    // @Query("DELETE FROM SyncEvent se WHERE se.user.id = :userId")
    // void deleteSyncQueueByUserId(@Param("userId") Long userId);

    // @Modifying
    // @Query("DELETE FROM UserAchievement ua WHERE ua.user.id = :userId")
    // void deleteUserAchievementsByUserId(@Param("userId") Long userId);

    // @Modifying
    // @Query("DELETE FROM GamificationProfile gp WHERE gp.user.id = :userId")
    // void deleteGamificationProfileByUserId(@Param("userId") Long userId);

@Modifying
@Query("DELETE FROM LearnerLanguage ll WHERE ll.user.id = :userId")
void deleteLearnerLanguagesByUserId(@Param("userId") Long userId);

@Modifying
@Query("DELETE FROM LearnerProfile lp WHERE lp.user.id = :userId")
void deleteLearnerProfilesByUserId(@Param("userId") Long userId);

@Modifying
@Query("DELETE FROM DiagnosticAssessment da WHERE da.user.id = :userId")
void deleteDiagnosticAssessmentsByUserId(@Param("userId") Long userId);

@Modifying
@Query("DELETE FROM User u WHERE u.id = :userId")
void deleteUserById(@Param("userId") Long userId);

    //@Modifying
    //@Query("UPDATE User u SET u.accountStatus = 'INACTIVE' WHERE u.id = :userId")
  //  void setAccountStatusInactive(@Param("userId") Long userId);

    //@Modifying
    //@Query("UPDATE PronunciationAttempt pa SET pa.retentionDate = :retentionDate WHERE pa.user.id = :userId")
    //void markPronunciationAttemptsForDeletion(@Param("userId") Long userId, @Param("retentionDate") LocalDateTime retentionDate);

    // @Modifying
    // @Query("UPDATE User u SET u.retentionDate = :retentionDate WHERE u.id = :userId")
    // void setRetentionDateForFullDeletion(@Param("userId") Long userId, @Param("retentionDate") LocalDateTime retentionDate);
/*
    @Modifying
    @Query("UPDATE User u SET u.fcmToken = NULL WHERE u.id = :userId")
    void clearFcmToken(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.accountStatus = 'PENDING_DELETION'")
    boolean isDeletionPending(@Param("userId") Long userId);*/

    @Modifying
    @Query("DELETE FROM UserConsent uc WHERE uc.user.id = :userId")
    void deleteUserConsentByUserId(@Param("userId") Long userId);
}

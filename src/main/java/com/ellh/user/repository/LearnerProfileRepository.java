package com.ellh.user.repository;

import com.ellh.user.entity.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Section 4.5.2.3 — idx_learner_profiles_user_id (UNIQUE B-tree FK index).
 * findByUserId is called on every authenticated API request to load the profile.
 */
@Repository
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

    Optional<LearnerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}

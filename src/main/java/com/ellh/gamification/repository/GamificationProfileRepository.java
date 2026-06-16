package com.ellh.gamification.repository;

import com.ellh.gamification.entity.GamificationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, Long> {
    Optional<GamificationProfile> findByUserId(Long userId);
}

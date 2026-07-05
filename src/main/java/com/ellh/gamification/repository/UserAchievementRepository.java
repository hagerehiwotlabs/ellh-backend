package com.ellh.gamification.repository;

import com.ellh.gamification.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserId(Long userId);
    
    // Check to prevent double-awarding
    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
}

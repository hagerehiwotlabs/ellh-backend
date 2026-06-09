package com.ellh.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeStatsResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("language_id")
    private Long languageId;

    @JsonProperty("total_xp")
    private Integer totalXp;

    @JsonProperty("current_daily_xp")
    private Integer currentDailyXp;

    @JsonProperty("daily_goal_xp")
    private Integer dailyGoalXp;

    @JsonProperty("current_streak")
    private Integer currentStreak;

    @JsonProperty("longest_streak")
    private Integer longestStreak;

    @JsonProperty("lessons_completed")
    private Integer lessonsCompleted;

    @JsonProperty("exercises_completed")
    private Integer exercisesCompleted;

    @JsonProperty("current_level")
    private Integer currentLevel;

    @JsonProperty("level_progress")
    private Integer levelProgress;  
}

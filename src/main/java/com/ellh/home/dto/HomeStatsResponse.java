package com.ellh.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Home/Dashboard statistics response.
 * GET /api/v1/home/stats?languageId={id}
 *
 * Contains aggregated user engagement metrics for the home screen.
 */
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

    @JsonProperty("daily_xp")
    private Integer dailyXp;

    @JsonProperty("current_streak")
    private Integer currentStreak;

    @JsonProperty("longest_streak")
    private Integer longestStreak;

    @JsonProperty("lessons_completed")
    private Integer lessonsCompleted;

    @JsonProperty("exercises_completed")
    private Integer exercisesCompleted;

    @JsonProperty("current_level")
    private String currentLevel;

    @JsonProperty("level_progress")
    private Integer levelProgress;  // 0-100 percentage to next level
}

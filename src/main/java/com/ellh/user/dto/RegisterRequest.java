package com.ellh.user.dto;

import com.ellh.user.entity.UserType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "User type is required")
    private UserType userType;

    // ── ONBOARDING FIELDS (Transmitted after Path Recommendation) ──
    
    private String pathwayType;       // "FOREIGN_LEARNER" or "BILINGUAL_LEARNER"
    private String currentCefrLevel;   // "A1", "A2", "B1"
    private Integer dailyGoalMinutes;
    private Long nativeLanguageId;     // The language they speak (Bilingual only)
    private Long targetLanguageId;     // The language they want to learn
}

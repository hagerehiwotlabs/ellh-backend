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

    // ── GDPR CONSENTS ──
    @AssertTrue(message = "Privacy Policy consent is mandatory")
    private boolean consentPrivacyPolicy;
    
    @AssertTrue(message = "Data Collection consent is mandatory")
    private boolean consentDataCollection;
    
    @AssertTrue(message = "Audio Recording consent is mandatory")
    private boolean consentAudioRecording;

    // ── ONBOARDING FIELDS ──
    private String pathwayType;       
    private String currentCefrLevel;   
    private Integer dailyGoalMinutes;
    private Long nativeLanguageId;     
    private Long targetLanguageId;     
}

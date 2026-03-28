package com.ellh.user.dto;

import com.ellh.user.entity.UserType;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for POST /api/v1/auth/register.
 * All fields are validated via Jakarta Bean Validation before reaching AuthService.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotNull(message = "User type is required")
    private UserType userType;
}

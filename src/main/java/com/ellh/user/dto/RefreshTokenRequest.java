package com.ellh.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/v1/auth/refresh. */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

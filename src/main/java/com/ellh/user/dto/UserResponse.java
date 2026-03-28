package com.ellh.user.dto;

import com.ellh.user.entity.AccountStatus;
import com.ellh.user.entity.UserType;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

/** User profile data returned by GET /api/v1/users/me. Never includes passwordHash. */
@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserType userType;
    private AccountStatus accountStatus;
    private boolean emailVerified;
    private String profileImageUrl;
    private Instant createdAt;
    private Instant lastActive;
}

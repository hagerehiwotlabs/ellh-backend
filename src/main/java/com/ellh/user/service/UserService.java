package com.ellh.user.service;

import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.user.dto.UpdateProfileRequest;
import com.ellh.user.dto.UserResponse;
import com.ellh.user.entity.User;
import com.ellh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves the profile metadata for the given user.
     * Maps the User entity to a secure UserResponse DTO.
     */
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .lastActive(user.getLastActive())
                .build();
    }

    /**
     * Updates profile details and saves them back to PostgreSQL.
     */
    @Transactional
    public UserResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        userRepository.save(user);

        return getUserProfile(userId);
    }
}

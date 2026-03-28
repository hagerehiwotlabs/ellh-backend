package com.ellh.infrastructure.security;

import com.ellh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a User principal from the database for Spring Security.
 * JWTAuthFilter calls loadUserByUsername(userId) — where the "username"
 * is actually the String representation of User.id extracted from the JWT subject.
 *
 * Using ID as the subject (not email) means renamed email addresses do not
 * invalidate existing tokens.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            Long id = Long.parseLong(userId);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with id: " + userId));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid user id in JWT subject: " + userId);
        }
    }
}

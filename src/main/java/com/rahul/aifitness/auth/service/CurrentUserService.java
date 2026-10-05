package com.rahul.aifitness.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.user.entity.User;
import com.rahul.aifitness.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException(
                    "Authenticated JWT is required"
            );
        }

        Jwt jwt = jwtAuthenticationToken.getToken();

        Number userId = jwt.getClaim("userId");

        if (userId == null) {
            throw new IllegalStateException(
                    "JWT does not contain userId claim"
            );
        }

        return userId.longValue();
    }

    public String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException(
                    "Authenticated JWT is required"
            );
        }

        return jwtAuthenticationToken.getToken().getSubject();
    }
    public User getCurrentUser() {

        Long userId = getCurrentUserId();

        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}
package com.rahul.aifitness.auth.service;

import com.rahul.aifitness.auth.dto.request.LoginRequest;
import com.rahul.aifitness.auth.dto.request.RegisterRequest;
import com.rahul.aifitness.auth.dto.response.AuthResponse;
import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.user.entity.User;
import com.rahul.aifitness.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(
            RegisterRequest request
    ) {
        log.info(
                "Registering new user. username={}, email={}",
                request.username(),
                request.email()
        );

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        validateRegistrationUniqueness(
                username,
                email
        );

        User user = User.builder()
                .username(username)
                .email(email)
                .firstName(request.firstName().trim())
                .lastName(
                        request.lastName() == null
                                ? null
                                : request.lastName().trim()
                )
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                savedUser
        );

        log.info(
                "User registered successfully. userId={}, username={}",
                savedUser.getId(),
                savedUser.getUsername()
        );

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {
        log.info(
                "User login attempt. identifier={}",
                request.usernameOrEmail()
        );

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.usernameOrEmail().trim(),
                                request.password()
                        )
                );

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Authenticated user not found"
                        )
                );

        String accessToken = jwtService.generateAccessToken(
                user
        );

        log.info(
                "User logged in successfully. userId={}, username={}",
                user.getId(),
                user.getUsername()
        );

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    private void validateRegistrationUniqueness(
            String username,
            String email
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException(
                    "Username already exists: " + username
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email already exists: " + email
            );
        }
    }
}
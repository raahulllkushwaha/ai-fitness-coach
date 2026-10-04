package com.rahul.aifitness.user.service;

import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.user.dto.request.CreateUserRequest;
import com.rahul.aifitness.user.dto.request.UpdateUserRequest;
import com.rahul.aifitness.user.dto.response.UserResponse;
import com.rahul.aifitness.user.entity.User;
import com.rahul.aifitness.user.mapper.UserMapper;
import com.rahul.aifitness.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request){
        log.info("Creating new User, username={}, email={}", request.username(), request.email());

        validateUniqueness(request);
        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully. userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long userId) {

        log.debug("Fetching user. userId={}", userId);
        User user = findUserById(userId);

        return userMapper.toResponse(user);
    }
    private User findUserById(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id '" + userId + "' was not found")
                );
    }

    private void validateUniqueness(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already in use");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already in use");
        }
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request){
        log.info("Updating user. userId={}, username={}, email={}", userId, request.username(), request.email());
        User user = findUserById(userId);

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        validateUpdateUniqueness(userId, username, email);

        user.updateProfile(username, email, request.firstName().trim(), request.lastName() == null ? null : request.lastName().trim());

        log.info("User updated successfully. userId={}", userId);

        return userMapper.toResponse(user);
    }
    private void validateUpdateUniqueness(Long userId, String username, String email) {

        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new DuplicateResourceException(
                    "Username '" + username + "' is already in use"
            );
        }

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new DuplicateResourceException(
                    "Email '" + email + "' is already in use"
            );
        }
    }

    @Transactional
    public void deleteUser(Long userId) {

        log.info("Deleting user. userId={}", userId);
        User user = findUserById(userId);
        userRepository.delete(user);

        log.info("User deleted successfully. userId={}", userId);
    }
}

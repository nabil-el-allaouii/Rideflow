package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.user.ProfileUpdateRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.UserProfileService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public UserProfileServiceImpl(
        UserRepository userRepository,
        AuthenticatedUserService authenticatedUserService
    ) {
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return toResponse(findCurrentUser());
    }

    @Override
    public UserResponse updateCurrentProfile(ProfileUpdateRequest request) {
        User user = findCurrentUser();
        String normalizedEmail = normalizeEmail(request.email());

        if (!user.email.equals(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException("An account with this email already exists.");
        }

        user.fullName = request.fullName().trim();
        user.email = normalizedEmail;
        user.phoneNumber = normalizeOptional(request.phoneNumber());
        user.preferredPaymentMethod = request.paymentMethod();

        return toResponse(userRepository.save(user));
    }

    private User findCurrentUser() {
        Long userId = authenticatedUserService.getCurrentUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.id,
            user.email,
            user.fullName,
            user.phoneNumber,
            user.preferredPaymentMethod,
            user.role,
            user.status,
            user.createdAt,
            user.lastLoginAt,
            user.updatedAt
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

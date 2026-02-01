package com.rideflow.demo.api.dto.user;

import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    PaymentMethod paymentMethod,
    UserRole role,
    UserStatus status,
    Instant createdAt,
    Instant lastLoginAt,
    Instant updatedAt
) {
}

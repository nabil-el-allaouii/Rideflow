package com.rideflow.demo.api.dto.auth;

import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;

public record AuthUserResponse(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    PaymentMethod paymentMethod,
    UserRole role,
    UserStatus status
) {
}

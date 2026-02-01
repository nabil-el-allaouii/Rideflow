package com.rideflow.demo.api.dto.user;

import com.rideflow.demo.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
    @NotNull(message = "User status is required.")
    UserStatus status
) {
}

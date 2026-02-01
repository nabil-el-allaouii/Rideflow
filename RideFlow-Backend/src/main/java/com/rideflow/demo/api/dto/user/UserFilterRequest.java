package com.rideflow.demo.api.dto.user;

import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import java.time.Instant;

public record UserFilterRequest(
    String query,
    UserRole role,
    UserStatus status,
    Instant fromDate,
    Instant toDate,
    Integer page,
    Integer size
) {
}

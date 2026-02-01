package com.rideflow.demo.api.dto.scooter;

import com.rideflow.demo.domain.enums.ScooterStatus;
import jakarta.validation.constraints.NotNull;

public record ScooterStatusUpdateRequest(
    @NotNull(message = "Scooter status is required.")
    ScooterStatus status
) {
}

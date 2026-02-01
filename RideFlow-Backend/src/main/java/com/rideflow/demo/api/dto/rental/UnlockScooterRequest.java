package com.rideflow.demo.api.dto.rental;

import jakarta.validation.constraints.NotNull;

public record UnlockScooterRequest(
    @NotNull(message = "Scooter id is required.")
    Long scooterId
) {
}

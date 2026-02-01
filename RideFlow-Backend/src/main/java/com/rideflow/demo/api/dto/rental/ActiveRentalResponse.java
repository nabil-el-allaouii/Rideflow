package com.rideflow.demo.api.dto.rental;

import com.rideflow.demo.domain.enums.RentalStatus;
import java.time.Instant;

public record ActiveRentalResponse(
    Long rentalId,
    Long scooterId,
    String scooterCode,
    String scooterModel,
    RentalStatus status,
    Instant createdAt,
    Instant startTime
) {
}

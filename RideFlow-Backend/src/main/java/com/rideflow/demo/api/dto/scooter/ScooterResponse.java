package com.rideflow.demo.api.dto.scooter;

import com.rideflow.demo.domain.enums.ScooterStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ScooterResponse(
    Long id,
    String publicCode,
    String model,
    Integer batteryPercentage,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    ScooterStatus status,
    BigDecimal kilometersTraveled,
    String maintenanceNotes,
    Instant lastActivityAt,
    Double distanceKm,
    boolean unlockable,
    String unlockBlockedReason
) {
}

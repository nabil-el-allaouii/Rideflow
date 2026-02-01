package com.rideflow.demo.api.dto.rental;

import com.rideflow.demo.domain.enums.RentalStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RentalResponse(
    Long id,
    Long userId,
    Long scooterId,
    String scooterCode,
    String scooterModel,
    Instant startTime,
    Instant endTime,
    RentalStatus status,
    Integer batteryAtStart,
    Integer batteryAtEnd,
    BigDecimal distanceTraveled,
    Integer durationMinutes,
    BigDecimal unlockFeeApplied,
    BigDecimal ratePerMinuteApplied,
    BigDecimal totalCost,
    Boolean receiptAvailable,
    Instant createdAt,
    Instant updatedAt
) {
}

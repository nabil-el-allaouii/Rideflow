package com.rideflow.demo.api.dto.scooter;

import com.rideflow.demo.domain.enums.ScooterStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record ScooterFilterRequest(
    String query,
    ScooterStatus status,
    @Min(value = 0, message = "Minimum battery must be between 0 and 100.")
    @Max(value = 100, message = "Minimum battery must be between 0 and 100.")
    Integer minBattery,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal radiusKm,
    Integer page,
    Integer size
) {
}

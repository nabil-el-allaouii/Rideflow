package com.rideflow.demo.api.dto.scooter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ScooterUpdateRequest(
    @NotBlank(message = "Model is required.")
    @Size(max = 100, message = "Model must not exceed 100 characters.")
    String model,
    @NotNull(message = "Battery percentage is required.")
    @Min(value = 0, message = "Battery percentage must be between 0 and 100.")
    @Max(value = 100, message = "Battery percentage must be between 0 and 100.")
    Integer batteryPercentage,
    BigDecimal latitude,
    BigDecimal longitude,
    @Size(max = 500, message = "Address must not exceed 500 characters.")
    String address,
    BigDecimal kilometersTraveled,
    @Size(max = 2000, message = "Maintenance notes must not exceed 2000 characters.")
    String maintenanceNotes
) {
}

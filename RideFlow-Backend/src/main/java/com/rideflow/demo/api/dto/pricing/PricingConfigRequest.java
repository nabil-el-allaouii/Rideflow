package com.rideflow.demo.api.dto.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PricingConfigRequest(
    @NotNull(message = "Unlock fee is required.")
    @DecimalMin(value = "0.00", inclusive = true, message = "Unlock fee must be at least 0.")
    BigDecimal unlockFee,
    @NotNull(message = "Rate per minute is required.")
    @DecimalMin(value = "0.00", inclusive = true, message = "Rate per minute must be at least 0.")
    BigDecimal ratePerMinute,
    @NotNull(message = "Battery consumption rate is required.")
    @DecimalMin(value = "0.01", inclusive = true, message = "Battery consumption rate must be greater than 0.")
    BigDecimal batteryConsumptionRate,
    @NotBlank(message = "Currency is required.")
    String currency
) {
}

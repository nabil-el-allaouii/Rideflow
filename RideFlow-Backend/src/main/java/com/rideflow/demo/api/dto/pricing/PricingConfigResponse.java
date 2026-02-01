package com.rideflow.demo.api.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingConfigResponse(
    Long id,
    BigDecimal unlockFee,
    BigDecimal ratePerMinute,
    BigDecimal batteryConsumptionRate,
    String currency,
    Instant effectiveFrom,
    Boolean active
) {
}

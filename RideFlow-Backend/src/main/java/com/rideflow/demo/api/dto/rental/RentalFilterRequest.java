package com.rideflow.demo.api.dto.rental;

import com.rideflow.demo.domain.enums.RentalStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RentalFilterRequest(
    Long userId,
    Long scooterId,
    String query,
    RentalStatus status,
    Instant fromDate,
    Instant toDate,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    Integer page,
    Integer size
) {
}

package com.rideflow.demo.api.dto.payment;

import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFilterRequest(
    String query,
    Long userId,
    Long rentalId,
    PaymentType type,
    PaymentStatus status,
    Instant fromDate,
    Instant toDate,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    Integer page,
    Integer size
) {
}

package com.rideflow.demo.api.dto.receipt;

import com.rideflow.demo.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptResponse(
    Long id,
    String receiptCode,
    Long rentalId,
    String scooterCode,
    String scooterModel,
    String userFullName,
    String userEmail,
    Instant startTime,
    Instant endTime,
    Integer durationMinutes,
    String durationLabel,
    BigDecimal unlockFee,
    BigDecimal ratePerMinute,
    BigDecimal usageCost,
    BigDecimal totalCost,
    Integer batteryAtStart,
    Integer batteryAtEnd,
    Integer batteryConsumed,
    BigDecimal distanceTraveled,
    PaymentStatus unlockPaymentStatus,
    String unlockPaymentReference,
    PaymentStatus finalPaymentStatus,
    String finalPaymentReference,
    String finalPaymentFailureReason,
    Instant generatedAt
) {
}

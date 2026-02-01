package com.rideflow.demo.api.dto.payment;

import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.enums.RentalStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    Long id,
    Long rentalId,
    Long userId,
    String userFullName,
    String userEmail,
    Long scooterId,
    String scooterCode,
    String scooterModel,
    RentalStatus rentalStatus,
    PaymentType type,
    BigDecimal amount,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    String transactionReference,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {
}

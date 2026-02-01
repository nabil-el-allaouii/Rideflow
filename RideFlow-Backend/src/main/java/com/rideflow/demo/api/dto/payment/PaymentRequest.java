package com.rideflow.demo.api.dto.payment;

import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentType;
import java.math.BigDecimal;

public record PaymentRequest(
    Long rentalId,
    PaymentType type,
    BigDecimal amount,
    PaymentMethod paymentMethod
) {
}

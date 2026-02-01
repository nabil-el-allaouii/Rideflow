package com.rideflow.demo.api.dto.payment;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
    BigDecimal totalAmount,
    Long totalCount,
    Double successRate
) {
}

package com.rideflow.demo.api.dto.payment;

import jakarta.validation.constraints.NotNull;

public record FinalPaymentRequest(
    @NotNull Long rentalId
) {
}

package com.rideflow.demo.api.dto.rental;

import jakarta.validation.constraints.Size;

public record ForceEndRentalRequest(
    @Size(max = 500, message = "Force-end reason must not exceed 500 characters.")
    String reason
) {
}

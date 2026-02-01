package com.rideflow.demo.api.dto.admin;

import java.time.LocalDate;

public record RentalReportPointResponse(
    LocalDate date,
    Long rentalCount
) {
}

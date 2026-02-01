package com.rideflow.demo.api.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueReportPointResponse(
    LocalDate date,
    BigDecimal revenue
) {
}

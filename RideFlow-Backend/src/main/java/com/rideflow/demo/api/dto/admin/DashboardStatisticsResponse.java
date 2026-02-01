package com.rideflow.demo.api.dto.admin;

import java.math.BigDecimal;

public record DashboardStatisticsResponse(
    Long totalScooters,
    Long availableScooters,
    Long reservedScooters,
    Long inUseScooters,
    Long lockedScooters,
    Long disabledScooters,
    Long activeRentals,
    BigDecimal totalRevenue,
    BigDecimal todayRevenue,
    Long lowBatteryScooters,
    Long scootersInMaintenance,
    Long totalUsers,
    Long activeUsers
) {
}

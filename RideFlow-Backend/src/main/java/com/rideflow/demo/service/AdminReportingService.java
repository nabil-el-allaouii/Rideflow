package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.admin.DashboardStatisticsResponse;
import com.rideflow.demo.api.dto.admin.RentalReportPointResponse;
import com.rideflow.demo.api.dto.admin.RevenueReportPointResponse;
import java.time.LocalDate;
import java.util.List;

public interface AdminReportingService {
    DashboardStatisticsResponse getDashboardStatistics();
    List<RentalReportPointResponse> getRentalReport(LocalDate fromDate, LocalDate toDate);
    List<RevenueReportPointResponse> getRevenueReport(LocalDate fromDate, LocalDate toDate);
}

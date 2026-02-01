package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.admin.DashboardStatisticsResponse;
import com.rideflow.demo.api.dto.admin.RentalReportPointResponse;
import com.rideflow.demo.api.dto.admin.RevenueReportPointResponse;
import com.rideflow.demo.service.AdminDashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<DashboardStatisticsResponse> statistics() {
        return ResponseEntity.ok(adminDashboardService.getStatistics());
    }

    @GetMapping("/reports/rentals")
    public ResponseEntity<List<RentalReportPointResponse>> rentalsReport(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(adminDashboardService.getRentalsReport(fromDate, toDate));
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<List<RevenueReportPointResponse>> revenueReport(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(adminDashboardService.getRevenueReport(fromDate, toDate));
    }
}

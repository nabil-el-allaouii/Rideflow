package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScooterRepository scooterRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    void getStatisticsAggregatesLiveCountsAndRevenue() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        when(userRepository.findAll()).thenReturn(List.of(
            user(1L, UserRole.CUSTOMER, UserStatus.ACTIVE),
            user(2L, UserRole.CUSTOMER, UserStatus.SUSPENDED),
            user(3L, UserRole.ADMIN, UserStatus.ACTIVE)
        ));
        when(scooterRepository.findAll()).thenReturn(List.of(
            scooter(1L, ScooterStatus.AVAILABLE, 90),
            scooter(2L, ScooterStatus.RESERVED, 80),
            scooter(3L, ScooterStatus.IN_USE, 70),
            scooter(4L, ScooterStatus.MAINTENANCE, 60),
            scooter(5L, ScooterStatus.LOCKED, 10),
            scooter(6L, ScooterStatus.DISABLED, 5)
        ));
        when(rentalRepository.findAll()).thenReturn(List.of(
            rental(1L, RentalStatus.ACTIVE, Instant.now()),
            rental(2L, RentalStatus.COMPLETED, Instant.now())
        ));
        when(paymentRepository.findAll()).thenReturn(List.of(
            payment(1L, PaymentStatus.SUCCEEDED, new BigDecimal("5.50"), today.atStartOfDay().toInstant(ZoneOffset.UTC)),
            payment(2L, PaymentStatus.SUCCEEDED, new BigDecimal("3.25"), today.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)),
            payment(3L, PaymentStatus.FAILED, new BigDecimal("9.99"), today.atStartOfDay().toInstant(ZoneOffset.UTC))
        ));

        var response = adminDashboardService.getStatistics();

        assertThat(response.totalScooters()).isEqualTo(6);
        assertThat(response.availableScooters()).isEqualTo(1);
        assertThat(response.reservedScooters()).isEqualTo(1);
        assertThat(response.inUseScooters()).isEqualTo(1);
        assertThat(response.lockedScooters()).isEqualTo(1);
        assertThat(response.disabledScooters()).isEqualTo(1);
        assertThat(response.scootersInMaintenance()).isEqualTo(1);
        assertThat(response.lowBatteryScooters()).isEqualTo(2);
        assertThat(response.totalUsers()).isEqualTo(2);
        assertThat(response.activeUsers()).isEqualTo(1);
        assertThat(response.activeRentals()).isEqualTo(1);
        assertThat(response.totalRevenue()).isEqualByComparingTo("8.75");
        assertThat(response.todayRevenue()).isEqualByComparingTo("5.50");
    }

    @Test
    void getRentalsReportReturnsDenseDateSeries() {
        LocalDate from = LocalDate.of(2026, 3, 20);
        LocalDate to = LocalDate.of(2026, 3, 22);

        when(rentalRepository.findAll()).thenReturn(List.of(
            rental(1L, RentalStatus.COMPLETED, from.atStartOfDay().toInstant(ZoneOffset.UTC)),
            rental(2L, RentalStatus.CANCELLED, from.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC))
        ));

        var report = adminDashboardService.getRentalsReport(from, to);

        assertThat(report).hasSize(3);
        assertThat(report.get(0).rentalCount()).isEqualTo(1);
        assertThat(report.get(1).rentalCount()).isEqualTo(0);
        assertThat(report.get(2).rentalCount()).isEqualTo(1);
    }

    @Test
    void getRevenueReportRejectsInvalidDateRange() {
        assertThatThrownBy(() ->
            adminDashboardService.getRevenueReport(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 24))
        )
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("The end date must be on or after the start date.");
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.id = id;
        user.role = role;
        user.status = status;
        return user;
    }

    private Scooter scooter(Long id, ScooterStatus status, int battery) {
        Scooter scooter = new Scooter();
        scooter.id = id;
        scooter.status = status;
        scooter.batteryPercentage = battery;
        return scooter;
    }

    private Rental rental(Long id, RentalStatus status, Instant createdAt) {
        Rental rental = new Rental();
        rental.id = id;
        rental.status = status;
        rental.createdAt = createdAt;
        return rental;
    }

    private Payment payment(Long id, PaymentStatus status, BigDecimal amount, Instant createdAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.status = status;
        payment.amount = amount;
        payment.createdAt = createdAt;
        return payment;
    }
}

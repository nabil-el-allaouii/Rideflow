package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.admin.DashboardStatisticsResponse;
import com.rideflow.demo.api.dto.admin.RentalReportPointResponse;
import com.rideflow.demo.api.dto.admin.RevenueReportPointResponse;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentStatus;
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
import com.rideflow.demo.service.AdminDashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int LOW_BATTERY_THRESHOLD = 15;

    private final UserRepository userRepository;
    private final ScooterRepository scooterRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardServiceImpl(
        UserRepository userRepository,
        ScooterRepository scooterRepository,
        RentalRepository rentalRepository,
        PaymentRepository paymentRepository
    ) {
        this.userRepository = userRepository;
        this.scooterRepository = scooterRepository;
        this.rentalRepository = rentalRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public DashboardStatisticsResponse getStatistics() {
        List<User> users = userRepository.findAll();
        List<Scooter> scooters = scooterRepository.findAll();
        List<Rental> rentals = rentalRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        long totalScooters = scooters.size();
        long availableScooters = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.AVAILABLE).count();
        long reservedScooters = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.RESERVED).count();
        long inUseScooters = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.IN_USE).count();
        long lockedScooters = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.LOCKED).count();
        long disabledScooters = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.DISABLED).count();
        long scootersInMaintenance = scooters.stream().filter(scooter -> scooter.status == ScooterStatus.MAINTENANCE).count();
        long lowBatteryScooters = scooters.stream()
            .filter(scooter -> scooter.batteryPercentage != null && scooter.batteryPercentage < LOW_BATTERY_THRESHOLD)
            .count();

        long activeRentals = rentals.stream().filter(rental -> rental.status == com.rideflow.demo.domain.enums.RentalStatus.ACTIVE).count();
        long totalUsers = users.stream().filter(user -> user.role == UserRole.CUSTOMER).count();
        long activeUsers = users.stream()
            .filter(user -> user.role == UserRole.CUSTOMER && user.status == UserStatus.ACTIVE)
            .count();

        BigDecimal totalRevenue = payments.stream()
            .filter(payment -> payment.status == PaymentStatus.SUCCEEDED)
            .map(payment -> payment.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayRevenue = payments.stream()
            .filter(payment -> payment.status == PaymentStatus.SUCCEEDED)
            .filter(payment -> payment.createdAt != null && payment.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isEqual(today))
            .map(payment -> payment.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardStatisticsResponse(
            totalScooters,
            availableScooters,
            reservedScooters,
            inUseScooters,
            lockedScooters,
            disabledScooters,
            activeRentals,
            totalRevenue,
            todayRevenue,
            lowBatteryScooters,
            scootersInMaintenance,
            totalUsers,
            activeUsers
        );
    }

    @Override
    public List<RentalReportPointResponse> getRentalsReport(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        Map<LocalDate, Long> counts = rentalRepository.findAll().stream()
            .filter(rental -> rental.createdAt != null)
            .collect(Collectors.groupingBy(
                rental -> rental.createdAt.atZone(ZoneOffset.UTC).toLocalDate(),
                Collectors.counting()
            ));

        return enumerateDates(fromDate, toDate).stream()
            .map(date -> new RentalReportPointResponse(date, counts.getOrDefault(date, 0L)))
            .toList();
    }

    @Override
    public List<RevenueReportPointResponse> getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        Map<LocalDate, BigDecimal> revenueByDate = paymentRepository.findAll().stream()
            .filter(payment -> payment.status == PaymentStatus.SUCCEEDED)
            .filter(payment -> payment.createdAt != null)
            .collect(Collectors.groupingBy(
                payment -> payment.createdAt.atZone(ZoneOffset.UTC).toLocalDate(),
                Collectors.mapping(
                    payment -> payment.amount,
                    Collectors.reducing(BigDecimal.ZERO, Function.identity(), BigDecimal::add)
                )
            ));

        return enumerateDates(fromDate, toDate).stream()
            .map(date -> new RevenueReportPointResponse(date, revenueByDate.getOrDefault(date, BigDecimal.ZERO)))
            .toList();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessRuleException("Both from and to dates are required.");
        }

        if (toDate.isBefore(fromDate)) {
            throw new BusinessRuleException("The end date must be on or after the start date.");
        }
    }

    private List<LocalDate> enumerateDates(LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        dates.sort(Comparator.naturalOrder());
        return dates;
    }
}

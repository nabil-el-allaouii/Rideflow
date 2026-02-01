package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.rental.RentalFilterRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.PricingConfig;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.PricingConfigRepository;
import com.rideflow.demo.domain.repository.ReceiptRepository;
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AuditLogWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private ScooterRepository scooterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private RentalServiceImpl rentalService;

    @BeforeEach
    void setUp() {
        lenient().when(rentalRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());
        lenient().when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of());
    }

    @Test
    void startRideTransitionsPendingRentalToActive() {
        User user = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.RESERVED, 95);
        Rental rental = rental(10L, RentalStatus.PENDING, scooter, user);
        Payment unlockPayment = unlockPayment(rental, user, PaymentStatus.SUCCEEDED);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(rentalRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(rental));
        when(scooterRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(scooter));
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(10L, PaymentType.UNLOCK_FEE))
            .thenReturn(Optional.of(unlockPayment));
        when(scooterRepository.saveAndFlush(any(Scooter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = rentalService.startRide(10L);

        assertThat(response.status()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(scooter.status).isEqualTo(ScooterStatus.IN_USE);
        assertThat(rental.startTime).isNotNull();
    }

    @Test
    void cancelRefundsUnlockPaymentAndReleasesScooter() {
        User user = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.RESERVED, 90);
        Rental rental = rental(10L, RentalStatus.PENDING, scooter, user);
        Payment unlockPayment = unlockPayment(rental, user, PaymentStatus.SUCCEEDED);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(rentalRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(rental));
        when(scooterRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(scooter));
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(10L, PaymentType.UNLOCK_FEE))
            .thenReturn(Optional.of(unlockPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scooterRepository.saveAndFlush(any(Scooter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = rentalService.cancel(10L);

        assertThat(response.status()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(unlockPayment.status).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(scooter.status).isEqualTo(ScooterStatus.AVAILABLE);
    }

    @Test
    void endRideCalculatesRoundedDurationCostAndBatteryConsumption() {
        User user = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.IN_USE, 100);
        Rental rental = rental(10L, RentalStatus.ACTIVE, scooter, user);
        rental.startTime = Instant.now().minusSeconds(61);
        rental.batteryAtStart = 100;
        PricingConfig pricing = pricing();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(rentalRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(rental));
        when(scooterRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(scooter));
        when(pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()).thenReturn(Optional.of(pricing));
        when(receiptRepository.findByRentalId(10L)).thenReturn(Optional.empty());
        when(receiptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scooterRepository.updateRideCompletionState(anyLong(), any(), any(), any(), any())).thenReturn(1);
        when(rentalRepository.findScooterIdByRentalId(10L)).thenReturn(Optional.of(5L));

        var response = rentalService.endRide(10L);

        assertThat(response.status()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(response.durationMinutes()).isEqualTo(2);
        assertThat(response.totalCost()).isEqualByComparingTo("1.30");
        assertThat(response.batteryAtEnd()).isEqualTo(99);
        assertThat(response.distanceTraveled()).isEqualByComparingTo("0.70");
        verify(scooterRepository).updateRideCompletionState(anyLong(), any(), any(), any(), any());
    }

    @Test
    void unlockRequiresConfiguredPaymentMethod() {
        User user = customer(1L);
        user.preferredPaymentMethod = null;
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 90);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> rentalService.unlock(new com.rideflow.demo.api.dto.rental.UnlockScooterRequest(5L)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Configure a payment method in your profile before making payments.");
    }

    @Test
    void findMyActiveRentalReturnsMappedResponse() {
        User user = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.RESERVED, 95);
        Rental rental = rental(10L, RentalStatus.PENDING, scooter, user);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(rentalRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(eq(1L), any())).thenReturn(Optional.of(rental));

        var response = rentalService.findMyActiveRental();

        assertThat(response).isPresent();
        assertThat(response.get().rentalId()).isEqualTo(10L);
        assertThat(response.get().status()).isEqualTo(RentalStatus.PENDING);
        assertThat(response.get().scooterCode()).isEqualTo("RF-5");
    }

    @Test
    void findMyRentalsAppliesFiltersAndPagination() {
        User user = customer(1L);
        Scooter scooterAlpha = scooter(5L, ScooterStatus.AVAILABLE, 90);
        scooterAlpha.publicCode = "RF-ALPHA";
        Rental completed = rental(10L, RentalStatus.COMPLETED, scooterAlpha, user);
        completed.totalCost = new BigDecimal("5.50");
        completed.createdAt = Instant.parse("2026-03-25T08:00:00Z");

        Scooter scooterBeta = scooter(6L, ScooterStatus.AVAILABLE, 91);
        scooterBeta.publicCode = "RF-BETA";
        Rental cancelled = rental(11L, RentalStatus.CANCELLED, scooterBeta, user);
        cancelled.totalCost = BigDecimal.ZERO;
        cancelled.createdAt = Instant.parse("2026-03-24T08:00:00Z");

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(rentalRepository.findByUserId(1L)).thenReturn(List.of(cancelled, completed));
        when(receiptRepository.findByRentalId(anyLong())).thenReturn(Optional.empty());

        var page = rentalService.findMyRentals(
            new RentalFilterRequest(
                null,
                null,
                "alpha",
                RentalStatus.COMPLETED,
                Instant.parse("2026-03-25T00:00:00Z"),
                Instant.parse("2026-03-25T23:59:59Z"),
                new BigDecimal("5.00"),
                new BigDecimal("6.00"),
                0,
                10
            )
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(10L);
        assertThat(page.content().get(0).scooterCode()).isEqualTo("RF-ALPHA");
    }

    @Test
    void findAllSupportsAdminFilters() {
        User customer = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 90);
        Rental completed = rental(10L, RentalStatus.COMPLETED, scooter, customer);
        completed.totalCost = new BigDecimal("7.25");

        User other = customer(2L);
        other.email = "other@example.com";
        Rental active = rental(11L, RentalStatus.ACTIVE, scooter(6L, ScooterStatus.IN_USE, 80), other);
        active.totalCost = new BigDecimal("1.20");

        when(rentalRepository.findAll()).thenReturn(List.of(active, completed));
        when(receiptRepository.findByRentalId(anyLong())).thenReturn(Optional.empty());

        var page = rentalService.findAll(new RentalFilterRequest(1L, null, null, RentalStatus.COMPLETED, null, null, null, null, 0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).userId()).isEqualTo(1L);
        assertThat(page.content().get(0).status()).isEqualTo(RentalStatus.COMPLETED);
    }

    @Test
    void exportAllAsCsvIncludesReceiptAvailability() {
        User user = customer(1L);
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 90);
        Rental rental = rental(10L, RentalStatus.COMPLETED, scooter, user);
        rental.totalCost = new BigDecimal("3.00");

        when(rentalRepository.findAll()).thenReturn(List.of(rental));
        when(receiptRepository.findByRentalId(10L)).thenReturn(Optional.of(new com.rideflow.demo.domain.model.Receipt()));

        byte[] csv = rentalService.exportAllAsCsv(new RentalFilterRequest(null, null, null, null, null, null, null, null, 0, 10));
        String content = new String(csv);

        assertThat(content).contains("rental_id,user_id,user_email");
        assertThat(content).contains("\"10\"");
        assertThat(content).contains("\"true\"");
    }

    private User customer(Long id) {
        User user = new User();
        user.id = id;
        user.email = "customer@example.com";
        user.fullName = "Customer";
        user.preferredPaymentMethod = PaymentMethod.CREDIT_CARD;
        return user;
    }

    private Scooter scooter(Long id, ScooterStatus status, int battery) {
        Scooter scooter = new Scooter();
        scooter.id = id;
        scooter.publicCode = "RF-" + id;
        scooter.model = "Model " + id;
        scooter.status = status;
        scooter.batteryPercentage = battery;
        scooter.kilometersTraveled = BigDecimal.ZERO;
        return scooter;
    }

    private Rental rental(Long id, RentalStatus status, Scooter scooter, User user) {
        Rental rental = new Rental();
        rental.id = id;
        rental.status = status;
        rental.scooter = scooter;
        rental.user = user;
        rental.createdAt = Instant.now();
        rental.unlockFeeApplied = new BigDecimal("1.00");
        rental.ratePerMinuteApplied = new BigDecimal("0.15");
        rental.batteryAtStart = scooter.batteryPercentage;
        return rental;
    }

    private Payment unlockPayment(Rental rental, User user, PaymentStatus status) {
        Payment payment = new Payment();
        payment.id = 44L;
        payment.rental = rental;
        payment.user = user;
        payment.type = PaymentType.UNLOCK_FEE;
        payment.status = status;
        payment.amount = new BigDecimal("1.00");
        payment.paymentMethod = user.preferredPaymentMethod;
        return payment;
    }

    private PricingConfig pricing() {
        PricingConfig pricing = new PricingConfig();
        pricing.unlockFee = new BigDecimal("1.00");
        pricing.ratePerMinute = new BigDecimal("0.15");
        pricing.batteryConsumptionRate = new BigDecimal("0.50");
        pricing.currency = "USD";
        pricing.active = true;
        pricing.effectiveFrom = Instant.now();
        return pricing;
    }
}

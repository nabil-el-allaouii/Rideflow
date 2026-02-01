package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.payment.FinalPaymentRequest;
import com.rideflow.demo.api.dto.payment.PaymentFilterRequest;
import com.rideflow.demo.api.dto.payment.UnlockFeePaymentRequest;
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
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AuditLogWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private ScooterRepository scooterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void payUnlockFeeSucceedsAndReservesScooter() {
        User user = customer(1L, PaymentMethod.CREDIT_CARD);
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 90);
        PricingConfig pricing = pricing();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(scooterRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(scooter));
        when(rentalRepository.existsByScooterIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(rentalRepository.existsByUserIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()).thenReturn(Optional.of(pricing));
        when(rentalRepository.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> {
            Rental rental = invocation.getArgument(0);
            if (rental.id == null) {
                rental.id = 11L;
            }
            rental.createdAt = Instant.now();
            return rental;
        });
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.id == null) {
                payment.id = 21L;
            }
            payment.createdAt = Instant.now();
            return payment;
        });
        when(scooterRepository.saveAndFlush(any(Scooter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ThreadLocalRandom random = org.mockito.Mockito.mock(ThreadLocalRandom.class);
        when(random.nextLong(1000, 2001)).thenReturn(0L);
        when(random.nextInt(100)).thenReturn(0);

        try (MockedStatic<ThreadLocalRandom> randomMock = mockStatic(ThreadLocalRandom.class)) {
            randomMock.when(ThreadLocalRandom::current).thenReturn(random);

            var response = paymentService.payUnlockFee(new UnlockFeePaymentRequest(5L));

            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(response.type()).isEqualTo(PaymentType.UNLOCK_FEE);
            assertThat(response.amount()).isEqualByComparingTo("1.00");
            assertThat(scooter.status).isEqualTo(ScooterStatus.RESERVED);
        }
    }

    @Test
    void payUnlockFeeRejectsScooterWithLowBattery() {
        User user = customer(1L, PaymentMethod.CREDIT_CARD);
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 10);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(scooterRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(scooter));

        assertThatThrownBy(() -> paymentService.payUnlockFee(new UnlockFeePaymentRequest(5L)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Scooter battery must be at least 15%.");

        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void payFinalAmountRejectsRentalThatHasNotEnded() {
        User user = customer(1L, PaymentMethod.DEBIT_CARD);
        Rental rental = rental(7L, RentalStatus.ACTIVE, scooter(5L, ScooterStatus.IN_USE, 90), user);
        rental.totalCost = new BigDecimal("3.00");

        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.findByIdAndUserIdForUpdate(7L, 1L)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() -> paymentService.payFinalAmount(new FinalPaymentRequest(7L)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Final payment can only be processed after the ride has ended.");
    }

    @Test
    void findAllAppliesQueryFilterAndPagination() {
        User user = customer(1L, PaymentMethod.WALLET);
        Scooter scooter = scooter(9L, ScooterStatus.AVAILABLE, 85);
        scooter.publicCode = "RF-101";
        scooter.model = "Ninebot";

        Rental rental = rental(15L, RentalStatus.COMPLETED, scooter, user);

        Payment matched = payment(1L, user, rental, PaymentType.FINAL_PAYMENT, PaymentStatus.SUCCEEDED, new BigDecimal("4.50"));
        matched.transactionReference = "TX-MATCH-001";
        matched.failureReason = null;

        Payment unmatched = payment(2L, user, rental, PaymentType.UNLOCK_FEE, PaymentStatus.FAILED, new BigDecimal("1.00"));
        unmatched.transactionReference = "TX-OTHER-999";
        unmatched.failureReason = "Card rejected";

        when(paymentRepository.findAll()).thenReturn(List.of(matched, unmatched));

        var page = paymentService.findAll(new PaymentFilterRequest(
            "match",
            null,
            null,
            null,
            PaymentStatus.SUCCEEDED,
            null,
            null,
            null,
            null,
            0,
            10
        ));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).transactionReference()).isEqualTo("TX-MATCH-001");
    }

    private User customer(Long id, PaymentMethod paymentMethod) {
        User user = new User();
        user.id = id;
        user.fullName = "Test Customer";
        user.email = "customer@example.com";
        user.preferredPaymentMethod = paymentMethod;
        return user;
    }

    private Scooter scooter(Long id, ScooterStatus status, int battery) {
        Scooter scooter = new Scooter();
        scooter.id = id;
        scooter.status = status;
        scooter.batteryPercentage = battery;
        scooter.publicCode = "RF-" + id;
        scooter.model = "Scooter " + id;
        return scooter;
    }

    private Rental rental(Long id, RentalStatus status, Scooter scooter, User user) {
        Rental rental = new Rental();
        rental.id = id;
        rental.status = status;
        rental.scooter = scooter;
        rental.user = user;
        rental.unlockFeeApplied = new BigDecimal("1.00");
        rental.ratePerMinuteApplied = new BigDecimal("0.15");
        rental.createdAt = Instant.now();
        return rental;
    }

    private PricingConfig pricing() {
        PricingConfig pricing = new PricingConfig();
        pricing.id = 3L;
        pricing.unlockFee = new BigDecimal("1.00");
        pricing.ratePerMinute = new BigDecimal("0.15");
        pricing.batteryConsumptionRate = new BigDecimal("0.50");
        pricing.currency = "USD";
        pricing.active = true;
        pricing.effectiveFrom = Instant.now();
        return pricing;
    }

    private Payment payment(
        Long id,
        User user,
        Rental rental,
        PaymentType type,
        PaymentStatus status,
        BigDecimal amount
    ) {
        Payment payment = new Payment();
        payment.id = id;
        payment.user = user;
        payment.rental = rental;
        payment.type = type;
        payment.status = status;
        payment.amount = amount;
        payment.paymentMethod = user.preferredPaymentMethod;
        payment.createdAt = Instant.now();
        payment.updatedAt = Instant.now();
        return payment;
    }
}

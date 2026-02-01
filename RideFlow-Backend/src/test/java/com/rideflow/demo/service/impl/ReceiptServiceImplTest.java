package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.Receipt;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.ReceiptRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    @Test
    void findByRentalIdReturnsReceiptForOwningCustomer() {
        Receipt receipt = receipt(12L, 3L);
        when(receiptRepository.findByRentalId(12L)).thenReturn(Optional.of(receipt));
        when(authenticatedUserService.isAdmin()).thenReturn(false);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(3L);
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(12L, PaymentType.UNLOCK_FEE))
            .thenReturn(Optional.of(payment(receipt.rental, PaymentType.UNLOCK_FEE, PaymentStatus.SUCCEEDED, "UNLOCK-1")));
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(12L, PaymentType.FINAL_PAYMENT))
            .thenReturn(Optional.of(payment(receipt.rental, PaymentType.FINAL_PAYMENT, PaymentStatus.SUCCEEDED, "FINAL-1")));

        var response = receiptService.findByRentalId(12L);

        assertThat(response.rentalId()).isEqualTo(12L);
        assertThat(response.unlockPaymentReference()).isEqualTo("UNLOCK-1");
        assertThat(response.finalPaymentReference()).isEqualTo("FINAL-1");
        assertThat(response.batteryConsumed()).isEqualTo(2);
    }

    @Test
    void findByRentalIdRejectsAccessToAnotherUsersReceipt() {
        Receipt receipt = receipt(12L, 3L);
        when(receiptRepository.findByRentalId(12L)).thenReturn(Optional.of(receipt));
        when(authenticatedUserService.isAdmin()).thenReturn(false);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(99L);

        assertThatThrownBy(() -> receiptService.findByRentalId(12L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("You can only access your own receipts.");
    }

    @Test
    void generateHtmlAndPdfIncludeReceiptIdentity() {
        Receipt receipt = receipt(12L, 3L);
        when(receiptRepository.findByRentalId(12L)).thenReturn(Optional.of(receipt));
        when(authenticatedUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(12L, PaymentType.UNLOCK_FEE))
            .thenReturn(Optional.of(payment(receipt.rental, PaymentType.UNLOCK_FEE, PaymentStatus.REFUNDED, "UNLOCK-1")));
        when(paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(12L, PaymentType.FINAL_PAYMENT))
            .thenReturn(Optional.empty());

        String html = receiptService.generateHtmlByRentalId(12L);
        byte[] pdf = receiptService.generatePdfByRentalId(12L);

        assertThat(html).contains("RideFlow Receipt");
        assertThat(html).contains("RCPT-12");
        assertThat(pdf).isNotEmpty();
    }

    private Receipt receipt(Long rentalId, Long userId) {
        User user = new User();
        user.id = userId;
        user.email = "customer@example.com";
        user.fullName = "Customer";
        user.role = UserRole.CUSTOMER;
        user.status = UserStatus.ACTIVE;
        user.preferredPaymentMethod = PaymentMethod.CREDIT_CARD;

        Scooter scooter = new Scooter();
        scooter.id = 7L;
        scooter.publicCode = "RF-007";
        scooter.model = "Urban";

        Rental rental = new Rental();
        rental.id = rentalId;
        rental.user = user;
        rental.scooter = scooter;
        rental.status = RentalStatus.COMPLETED;
        rental.startTime = Instant.parse("2026-03-25T08:00:00Z");
        rental.endTime = Instant.parse("2026-03-25T08:10:00Z");
        rental.durationMinutes = 10;
        rental.unlockFeeApplied = new BigDecimal("1.00");
        rental.ratePerMinuteApplied = new BigDecimal("0.15");
        rental.distanceTraveled = new BigDecimal("3.50");
        rental.batteryAtStart = 80;
        rental.batteryAtEnd = 78;
        rental.totalCost = new BigDecimal("2.50");

        Receipt receipt = new Receipt();
        receipt.id = 100L;
        receipt.receiptCode = "RCPT-" + rentalId;
        receipt.rental = rental;
        receipt.generatedAt = Instant.parse("2026-03-25T08:10:05Z");
        receipt.unlockFeeCharged = new BigDecimal("1.00");
        receipt.usageCost = new BigDecimal("1.50");
        receipt.totalCost = new BigDecimal("2.50");
        return receipt;
    }

    private Payment payment(Rental rental, PaymentType type, PaymentStatus status, String reference) {
        Payment payment = new Payment();
        payment.rental = rental;
        payment.user = rental.user;
        payment.type = type;
        payment.status = status;
        payment.amount = type == PaymentType.UNLOCK_FEE ? new BigDecimal("1.00") : new BigDecimal("1.50");
        payment.transactionReference = reference;
        payment.paymentMethod = PaymentMethod.CREDIT_CARD;
        return payment;
    }
}

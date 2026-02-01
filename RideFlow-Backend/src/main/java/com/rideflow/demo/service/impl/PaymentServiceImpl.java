package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.payment.FinalPaymentRequest;
import com.rideflow.demo.api.dto.payment.PaymentFilterRequest;
import com.rideflow.demo.api.dto.payment.PaymentResponse;
import com.rideflow.demo.api.dto.payment.UnlockFeePaymentRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
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
import com.rideflow.demo.service.PaymentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MINIMUM_UNLOCK_BATTERY = 15;
    private static final BigDecimal DEFAULT_UNLOCK_FEE = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_RATE_PER_MINUTE = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_BATTERY_CONSUMPTION_RATE = new BigDecimal("0.50");
    private static final Set<RentalStatus> ACTIVE_OR_PENDING_STATUSES = EnumSet.of(
        RentalStatus.PENDING,
        RentalStatus.ACTIVE
    );

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final ScooterRepository scooterRepository;
    private final UserRepository userRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogWriter auditLogWriter;

    @Value("${app.simulation.payment-success-rate:95}")
    private int paymentSuccessRate = 95;

    @Value("${app.simulation.payment-min-delay-ms:1000}")
    private long paymentMinDelayMs = 1000;

    @Value("${app.simulation.payment-max-delay-ms:2000}")
    private long paymentMaxDelayMs = 2000;

    public PaymentServiceImpl(
        PaymentRepository paymentRepository,
        RentalRepository rentalRepository,
        ScooterRepository scooterRepository,
        UserRepository userRepository,
        PricingConfigRepository pricingConfigRepository,
        AuthenticatedUserService authenticatedUserService,
        AuditLogWriter auditLogWriter
    ) {
        this.paymentRepository = paymentRepository;
        this.rentalRepository = rentalRepository;
        this.scooterRepository = scooterRepository;
        this.userRepository = userRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public PaymentResponse payUnlockFee(UnlockFeePaymentRequest request) {
        Long currentUserId = authenticatedUserService.getCurrentUserId();
        User user = userRepository.findByIdForUpdate(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        PaymentMethod configuredPaymentMethod = resolveConfiguredPaymentMethod(user);
        Scooter scooter = scooterRepository.findByIdForUpdate(request.scooterId())
            .orElseThrow(() -> new ResourceNotFoundException("Scooter not found."));

        validateUnlockEligibility(user.id, scooter);
        PricingConfig pricing = getOrCreateActivePricing();

        Rental rental = new Rental();
        rental.user = user;
        rental.scooter = scooter;
        rental.status = RentalStatus.PENDING;
        rental.batteryAtStart = scooter.batteryPercentage;
        rental.unlockFeeApplied = pricing.unlockFee.setScale(2, RoundingMode.HALF_UP);
        rental.ratePerMinuteApplied = pricing.ratePerMinute.setScale(2, RoundingMode.HALF_UP);
        rental = rentalRepository.saveAndFlush(rental);

        Payment payment = new Payment();
        payment.rental = rental;
        payment.user = user;
        payment.type = PaymentType.UNLOCK_FEE;
        payment.amount = rental.unlockFeeApplied;
        payment.paymentMethod = configuredPaymentMethod;
        payment.status = PaymentStatus.PENDING;
        payment.transactionReference = generateTransactionReference();
        payment = paymentRepository.saveAndFlush(payment);
        auditLogWriter.logSuccess(
            AuditActionType.PAYMENT_INITIATED,
            AuditEntityType.PAYMENT,
            payment.id,
            paymentPayload(payment)
        );

        payment.status = PaymentStatus.PROCESSING;
        payment.failureReason = null;
        payment.transactionReference = generateTransactionReference();
        paymentRepository.saveAndFlush(payment);

        simulateProcessingDelay();

        if (isSuccessfulAttempt()) {
            payment.status = PaymentStatus.SUCCEEDED;
            scooter.status = ScooterStatus.RESERVED;
            scooter.lastActivityAt = Instant.now();
            scooterRepository.saveAndFlush(scooter);
            paymentRepository.saveAndFlush(payment);
            auditLogWriter.logSuccess(
                AuditActionType.PAYMENT_SUCCEEDED,
                AuditEntityType.PAYMENT,
                payment.id,
                paymentPayload(payment)
            );
            auditLogWriter.logSuccess(
                AuditActionType.RENTAL_UNLOCK,
                AuditEntityType.RENTAL,
                rental.id,
                payload(
                    "scooterId", scooter.id,
                    "unlockFee", payment.amount,
                    "paymentId", payment.id
                )
            );
            return toResponse(payment);
        }

        payment.status = PaymentStatus.FAILED;
        payment.failureReason = "Simulated payment failure. Please retry.";
        rental.status = RentalStatus.CANCELLED;
        rental.endTime = Instant.now();
        rental.totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        scooter.status = resolveAvailableScooterStatus(scooter.batteryPercentage == null ? 0 : scooter.batteryPercentage);
        scooter.lastActivityAt = Instant.now();

        rentalRepository.saveAndFlush(rental);
        scooterRepository.saveAndFlush(scooter);
        paymentRepository.saveAndFlush(payment);
        auditLogWriter.logFailure(
            AuditActionType.PAYMENT_FAILED,
            AuditEntityType.PAYMENT,
            payment.id,
            paymentPayload(payment)
        );

        return toResponse(payment);
    }

    @Override
    public PaymentResponse payFinalAmount(FinalPaymentRequest request) {
        Long currentUserId = authenticatedUserService.getCurrentUserId();
        User user = userRepository.findByIdForUpdate(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        PaymentMethod configuredPaymentMethod = resolveConfiguredPaymentMethod(user);
        Rental rental = rentalRepository.findByIdAndUserIdForUpdate(request.rentalId(), currentUserId)
            .orElseThrow(() -> new BusinessRuleException("You can only access your own payments."));

        if (rental.status != RentalStatus.COMPLETED && rental.status != RentalStatus.FORCE_ENDED) {
            throw new BusinessRuleException("Final payment can only be processed after the ride has ended.");
        }

        if (rental.totalCost == null) {
            throw new BusinessRuleException("Rental total has not been calculated yet.");
        }

        BigDecimal finalAmount = rental.totalCost
            .subtract(rental.unlockFeeApplied == null ? BigDecimal.ZERO : rental.unlockFeeApplied)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        Payment payment = paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(rental.id, PaymentType.FINAL_PAYMENT)
            .orElseGet(() -> {
                Payment newPayment = new Payment();
                newPayment.rental = rental;
                newPayment.user = user;
                newPayment.type = PaymentType.FINAL_PAYMENT;
                newPayment.amount = finalAmount;
                newPayment.status = PaymentStatus.PENDING;
                newPayment.paymentMethod = configuredPaymentMethod;
                newPayment.transactionReference = generateTransactionReference();
                return newPayment;
            });

        if (payment.id != null && payment.status == PaymentStatus.SUCCEEDED) {
            return toResponse(payment);
        }

        payment.amount = finalAmount;
        payment.paymentMethod = configuredPaymentMethod;
        payment.status = PaymentStatus.PROCESSING;
        payment.failureReason = null;
        payment.transactionReference = generateTransactionReference();
        payment = paymentRepository.saveAndFlush(payment);
        auditLogWriter.logSuccess(
            AuditActionType.PAYMENT_INITIATED,
            AuditEntityType.PAYMENT,
            payment.id,
            paymentPayload(payment)
        );

        simulateProcessingDelay();

        if (isSuccessfulAttempt()) {
            payment.status = PaymentStatus.SUCCEEDED;
            payment = paymentRepository.saveAndFlush(payment);
            auditLogWriter.logSuccess(
                AuditActionType.PAYMENT_SUCCEEDED,
                AuditEntityType.PAYMENT,
                payment.id,
                paymentPayload(payment)
            );
        } else {
            payment.status = PaymentStatus.FAILED;
            payment.failureReason = "Simulated payment failure. Please retry.";
            payment = paymentRepository.saveAndFlush(payment);
            auditLogWriter.logFailure(
                AuditActionType.PAYMENT_FAILED,
                AuditEntityType.PAYMENT,
                payment.id,
                paymentPayload(payment)
            );
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> findMyPayments(PaymentFilterRequest filter) {
        Long currentUserId = authenticatedUserService.getCurrentUserId();
        List<PaymentResponse> payments = paymentRepository.findByUserId(currentUserId).stream()
            .sorted(Comparator.comparing((Payment payment) -> payment.createdAt).reversed())
            .filter(payment -> matchesFilter(payment, filter))
            .map(this::toResponse)
            .toList();

        return paginate(payments, filter.page(), filter.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> findAll(PaymentFilterRequest filter) {
        List<PaymentResponse> payments = paymentRepository.findAll().stream()
            .sorted(Comparator.comparing((Payment payment) -> payment.createdAt).reversed())
            .filter(payment -> matchesFilter(payment, filter))
            .map(this::toResponse)
            .toList();

        return paginate(payments, filter.page(), filter.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
    }

    private void validateUnlockEligibility(Long userId, Scooter scooter) {
        if (scooter.status != ScooterStatus.AVAILABLE) {
            throw new BusinessRuleException("Scooter is not available.");
        }

        if (scooter.batteryPercentage < MINIMUM_UNLOCK_BATTERY) {
            throw new BusinessRuleException("Scooter battery must be at least 15%.");
        }

        if (rentalRepository.existsByScooterIdAndStatusIn(scooter.id, ACTIVE_OR_PENDING_STATUSES)) {
            throw new BusinessRuleException("Scooter already has an active or pending rental.");
        }

        if (rentalRepository.existsByUserIdAndStatusIn(userId, ACTIVE_OR_PENDING_STATUSES)) {
            throw new BusinessRuleException("You already have an active or pending rental.");
        }
    }

    private PricingConfig getOrCreateActivePricing() {
        return pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
            .orElseGet(() -> {
                PricingConfig pricingConfig = new PricingConfig();
                pricingConfig.unlockFee = DEFAULT_UNLOCK_FEE;
                pricingConfig.ratePerMinute = DEFAULT_RATE_PER_MINUTE;
                pricingConfig.batteryConsumptionRate = DEFAULT_BATTERY_CONSUMPTION_RATE;
                pricingConfig.currency = "USD";
                pricingConfig.effectiveFrom = Instant.now();
                pricingConfig.active = true;
                return pricingConfigRepository.save(pricingConfig);
            });
    }

    private ScooterStatus resolveAvailableScooterStatus(int batteryPercentage) {
        return batteryPercentage >= MINIMUM_UNLOCK_BATTERY
            ? ScooterStatus.AVAILABLE
            : ScooterStatus.LOCKED;
    }

    private PaymentMethod resolveConfiguredPaymentMethod(User user) {
        if (user.preferredPaymentMethod == null) {
            throw new BusinessRuleException(
                "Configure a payment method in your profile before making payments."
            );
        }

        return user.preferredPaymentMethod;
    }

    private String generateTransactionReference() {
        return "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private boolean isSuccessfulAttempt() {
        int boundedSuccessRate = Math.max(0, Math.min(100, paymentSuccessRate));
        return ThreadLocalRandom.current().nextInt(100) < boundedSuccessRate;
    }

    private void simulateProcessingDelay() {
        long minDelay = Math.max(0, paymentMinDelayMs);
        long maxDelay = Math.max(minDelay, paymentMaxDelayMs);
        long delayMs = minDelay == maxDelay
            ? minDelay
            : ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment processing was interrupted.", exception);
        }
    }

    private boolean matchesFilter(Payment payment, PaymentFilterRequest filter) {
        if (filter == null) {
            return true;
        }

        if (filter.query() != null && !filter.query().isBlank()) {
            String query = filter.query().trim().toLowerCase();
            boolean matches = containsIgnoreCase(payment.transactionReference, query)
                || containsIgnoreCase(payment.failureReason, query)
                || containsIgnoreCase(payment.user == null ? null : payment.user.fullName, query)
                || containsIgnoreCase(payment.user == null ? null : payment.user.email, query)
                || containsIgnoreCase(payment.rental == null || payment.rental.scooter == null ? null : payment.rental.scooter.publicCode, query)
                || containsIgnoreCase(payment.rental == null || payment.rental.scooter == null ? null : payment.rental.scooter.model, query);
            if (!matches) {
                return false;
            }
        }

        if (filter.userId() != null && !filter.userId().equals(payment.user.id)) {
            return false;
        }

        if (filter.rentalId() != null && !filter.rentalId().equals(payment.rental.id)) {
            return false;
        }

        if (filter.type() != null && filter.type() != payment.type) {
            return false;
        }

        if (filter.status() != null && filter.status() != payment.status) {
            return false;
        }

        if (filter.fromDate() != null && payment.createdAt.isBefore(filter.fromDate())) {
            return false;
        }

        if (filter.toDate() != null && payment.createdAt.isAfter(filter.toDate())) {
            return false;
        }

        if (filter.minAmount() != null && payment.amount.compareTo(filter.minAmount()) < 0) {
            return false;
        }

        if (filter.maxAmount() != null && payment.amount.compareTo(filter.maxAmount()) > 0) {
            return false;
        }

        return true;
    }

    private PageResponse<PaymentResponse> paginate(List<PaymentResponse> responses, Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size <= 0
            ? DEFAULT_SIZE
            : Math.min(size, MAX_PAGE_SIZE);

        int fromIndex = Math.min(resolvedPage * resolvedSize, responses.size());
        int toIndex = Math.min(fromIndex + resolvedSize, responses.size());
        int totalPages = responses.isEmpty()
            ? 0
            : (int) Math.ceil((double) responses.size() / resolvedSize);

        return new PageResponse<>(
            responses.subList(fromIndex, toIndex),
            resolvedPage,
            resolvedSize,
            (long) responses.size(),
            totalPages
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        Rental rental = payment.rental;
        User user = payment.user;
        Scooter scooter = rental == null ? null : rental.scooter;

        return new PaymentResponse(
            payment.id,
            rental == null ? null : rental.id,
            user == null ? null : user.id,
            user == null ? null : user.fullName,
            user == null ? null : user.email,
            scooter == null ? null : scooter.id,
            scooter == null ? null : scooter.publicCode,
            scooter == null ? null : scooter.model,
            rental == null ? null : rental.status,
            payment.type,
            payment.amount,
            payment.status,
            payment.paymentMethod,
            payment.transactionReference,
            payment.failureReason,
            payment.createdAt,
            payment.updatedAt
        );
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private Map<String, Object> paymentPayload(Payment payment) {
        return payload(
            "rentalId", payment.rental == null ? null : payment.rental.id,
            "type", payment.type,
            "amount", payment.amount,
            "status", payment.status,
            "paymentMethod", payment.paymentMethod,
            "transactionReference", payment.transactionReference,
            "failureReason", payment.failureReason
        );
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            payload.put(String.valueOf(values[index]), values[index + 1]);
        }
        return payload;
    }
}

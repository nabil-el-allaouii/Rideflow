package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.rental.ActiveRentalResponse;
import com.rideflow.demo.api.dto.rental.ForceEndRentalRequest;
import com.rideflow.demo.api.dto.rental.RentalFilterRequest;
import com.rideflow.demo.api.dto.rental.RentalResponse;
import com.rideflow.demo.api.dto.rental.UnlockScooterRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.PricingConfig;
import com.rideflow.demo.domain.model.Receipt;
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
import com.rideflow.demo.service.RentalService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RentalServiceImpl implements RentalService {
    private static final Duration RESERVATION_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration MAX_RIDE_DURATION = Duration.ofHours(24);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MINIMUM_UNLOCK_BATTERY = 15;
    private static final BigDecimal DEFAULT_UNLOCK_FEE = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_RATE_PER_MINUTE = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_BATTERY_CONSUMPTION_RATE = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_DISTANCE_PER_MINUTE_KM = new BigDecimal("0.35");
    private static final Set<RentalStatus> ACTIVE_OR_PENDING_STATUSES = EnumSet.of(
        RentalStatus.PENDING,
        RentalStatus.ACTIVE
    );
    private static final Set<RentalStatus> CLOSED_RENTAL_STATUSES = EnumSet.of(
        RentalStatus.COMPLETED,
        RentalStatus.FORCE_ENDED
    );
    private static final Set<ScooterStatus> STALE_OPEN_SCOOTER_STATUSES = EnumSet.of(
        ScooterStatus.RESERVED,
        ScooterStatus.IN_USE
    );

    private final RentalRepository rentalRepository;
    private final ScooterRepository scooterRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final ReceiptRepository receiptRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogWriter auditLogWriter;

    public RentalServiceImpl(
        RentalRepository rentalRepository,
        ScooterRepository scooterRepository,
        UserRepository userRepository,
        PaymentRepository paymentRepository,
        PricingConfigRepository pricingConfigRepository,
        ReceiptRepository receiptRepository,
        AuthenticatedUserService authenticatedUserService,
        AuditLogWriter auditLogWriter
    ) {
        this.rentalRepository = rentalRepository;
        this.scooterRepository = scooterRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.receiptRepository = receiptRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public RentalResponse unlock(UnlockScooterRequest request) {
        cancelExpiredPendingRentalsInternal();

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

        Rental savedRental = rentalRepository.save(rental);

        Payment unlockPayment = createPayment(
            savedRental,
            user,
            PaymentType.UNLOCK_FEE,
            rental.unlockFeeApplied,
            configuredPaymentMethod,
            PaymentStatus.SUCCEEDED,
            null
        );
        paymentRepository.save(unlockPayment);

        scooter.status = ScooterStatus.RESERVED;
        scooter.lastActivityAt = Instant.now();
        scooterRepository.save(scooter);

        auditLogWriter.logSuccess(
            AuditActionType.RENTAL_UNLOCK,
            AuditEntityType.RENTAL,
            savedRental.id,
            payload(
                "scooterId", scooter.id,
                "paymentId", unlockPayment.id,
                "unlockFee", unlockPayment.amount
            )
        );
        auditLogWriter.logSuccess(
            AuditActionType.PAYMENT_SUCCEEDED,
            AuditEntityType.PAYMENT,
            unlockPayment.id,
            paymentPayload(unlockPayment)
        );

        return toResponse(savedRental);
    }

    @Override
    public RentalResponse startRide(Long rentalId) {
        cancelExpiredPendingRentalsInternal();

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        Rental rental = rentalRepository.findByIdAndUserIdForUpdate(rentalId, currentUserId)
            .orElseThrow(() -> new BusinessRuleException("You can only access your own rentals."));

        Scooter scooter = getRentalScooterOrThrow(rental);

        if (rental.status == RentalStatus.ACTIVE) {
            return toResponse(rental);
        }

        if (rental.status != RentalStatus.PENDING) {
            throw new BusinessRuleException("Only pending rentals can be started.");
        }

        Payment unlockPayment = paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(rental.id, PaymentType.UNLOCK_FEE)
            .orElseThrow(() -> new BusinessRuleException("Unlock fee payment was not found for this rental."));
        if (unlockPayment.status != PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleException("Unlock fee must be paid successfully before starting the ride.");
        }

        if (isReservationExpired(rental)) {
            cancelPendingRental(
                rental,
                scooter,
                "Reservation expired before ride start.",
                null,
                AuditActorRole.SYSTEM
            );
            throw new BusinessRuleException("Reservation expired. Unlock the scooter again.");
        }

        rental.startTime = Instant.now();
        rental.status = RentalStatus.ACTIVE;

        scooter.status = ScooterStatus.IN_USE;
        scooter.lastActivityAt = Instant.now();

        scooterRepository.saveAndFlush(scooter);
        rentalRepository.saveAndFlush(rental);
        auditLogWriter.logSuccess(
            AuditActionType.RENTAL_START,
            AuditEntityType.RENTAL,
            rental.id,
            payload("scooterId", scooter.id, "startedAt", rental.startTime)
        );

        return toResponse(rental);
    }

    @Override
    public RentalResponse cancel(Long rentalId) {
        cancelExpiredPendingRentalsInternal();

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        Rental rental = rentalRepository.findByIdAndUserIdForUpdate(rentalId, currentUserId)
            .orElseThrow(() -> new BusinessRuleException("You can only access your own rentals."));

        Scooter scooter = getRentalScooterOrThrow(rental);

        if (rental.status == RentalStatus.CANCELLED) {
            return toResponse(rental);
        }

        if (rental.status != RentalStatus.PENDING) {
            throw new BusinessRuleException("Only pending rentals can be cancelled.");
        }

        cancelPendingRental(
            rental,
            scooter,
            "Ride cancelled by user before start.",
            rental.user,
            resolveAuditActorRole(rental.user)
        );
        return toResponse(rental);
    }

    @Override
    public RentalResponse endRide(Long rentalId) {
        cancelExpiredPendingRentalsInternal();
        forceEndOverdueActiveRentalsInternal();

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        Rental rental = rentalRepository.findByIdAndUserIdForUpdate(rentalId, currentUserId)
            .orElseThrow(() -> new BusinessRuleException("You can only access your own rentals."));

        Scooter scooter = getRentalScooterOrThrow(rental);

        if (rental.status == RentalStatus.COMPLETED || rental.status == RentalStatus.FORCE_ENDED) {
            reconcileClosedRentalScooter(rental, scooter);
            return toResponse(rental);
        }

        if (rental.status != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Only active rentals can be ended.");
        }

        RentalResponse response = finalizeRental(rental, scooter, RentalStatus.COMPLETED, null);
        auditLogWriter.logSuccess(
            AuditActionType.RENTAL_END,
            AuditEntityType.RENTAL,
            rental.id,
            payload(
                "scooterId", scooter.id,
                "durationMinutes", rental.durationMinutes,
                "batteryAtEnd", rental.batteryAtEnd,
                "totalCost", rental.totalCost
            )
        );
        return response;
    }

    @Override
    public Optional<ActiveRentalResponse> findMyActiveRental() {
        cancelExpiredPendingRentalsInternal();
        forceEndOverdueActiveRentalsInternal();

        Long currentUserId = authenticatedUserService.getCurrentUserId();

        return rentalRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                currentUserId,
                ACTIVE_OR_PENDING_STATUSES
            )
            .map(this::toActiveRentalResponse);
    }

    @Override
    public PageResponse<RentalResponse> findMyRentals(RentalFilterRequest filter) {
        cancelExpiredPendingRentalsInternal();
        forceEndOverdueActiveRentalsInternal();

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        List<RentalResponse> rentals = rentalRepository.findByUserId(currentUserId).stream()
            .sorted(Comparator.comparing((Rental rental) -> rental.createdAt).reversed())
            .filter(rental -> matchesFilter(rental, filter))
            .map(this::toResponse)
            .toList();

        return paginate(rentals, filter.page(), filter.size());
    }

    @Override
    public PageResponse<RentalResponse> findAll(RentalFilterRequest filter) {
        cancelExpiredPendingRentalsInternal();
        forceEndOverdueActiveRentalsInternal();

        List<RentalResponse> rentals = rentalRepository.findAll().stream()
            .sorted(Comparator.comparing((Rental rental) -> rental.createdAt).reversed())
            .filter(rental -> matchesFilter(rental, filter))
            .map(this::toResponse)
            .toList();

        return paginate(rentals, filter.page(), filter.size());
    }

    @Override
    public byte[] exportAllAsCsv(RentalFilterRequest filter) {
        List<Rental> rentals = rentalRepository.findAll().stream()
            .sorted(Comparator.comparing((Rental rental) -> rental.createdAt).reversed())
            .filter(rental -> matchesFilter(rental, filter))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("rental_id,user_id,user_email,scooter_id,scooter_code,scooter_model,status,start_time,end_time,duration_minutes,distance_traveled,unlock_fee,rate_per_minute,total_cost,receipt_available\n");

        rentals.forEach(rental -> csv.append(csvValue(rental.id))
            .append(',').append(csvValue(rental.user.id))
            .append(',').append(csvValue(rental.user.email))
            .append(',').append(csvValue(rental.scooter.id))
            .append(',').append(csvValue(rental.scooter.publicCode))
            .append(',').append(csvValue(rental.scooter.model))
            .append(',').append(csvValue(rental.status))
            .append(',').append(csvValue(rental.startTime))
            .append(',').append(csvValue(rental.endTime))
            .append(',').append(csvValue(rental.durationMinutes))
            .append(',').append(csvValue(rental.distanceTraveled))
            .append(',').append(csvValue(rental.unlockFeeApplied))
            .append(',').append(csvValue(rental.ratePerMinuteApplied))
            .append(',').append(csvValue(rental.totalCost))
            .append(',').append(csvValue(receiptRepository.findByRentalId(rental.id).isPresent()))
            .append('\n'));

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public RentalResponse forceEnd(Long rentalId, ForceEndRentalRequest request) {
        cancelExpiredPendingRentalsInternal();

        Rental rental = rentalRepository.findByIdForUpdate(rentalId)
            .orElseThrow(() -> new ResourceNotFoundException("Rental not found."));

        Scooter scooter = getRentalScooterOrThrow(rental);

        if (rental.status == RentalStatus.FORCE_ENDED || rental.status == RentalStatus.COMPLETED) {
            return toResponse(rental);
        }

        if (rental.status != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Only active rentals can be force-ended.");
        }

        RentalResponse response = finalizeRental(rental, scooter, RentalStatus.FORCE_ENDED, normalizeReason(request.reason()));
        auditLogWriter.logSuccess(
            AuditActionType.RENTAL_FORCE_END,
            AuditEntityType.RENTAL,
            rental.id,
            payload(
                "scooterId", scooter.id,
                "reason", normalizeReason(request.reason()),
                "durationMinutes", rental.durationMinutes,
                "batteryAtEnd", rental.batteryAtEnd
            )
        );
        return response;
    }

    @Scheduled(fixedDelay = 5000)
    public void cancelExpiredPendingRentals() {
        cancelExpiredPendingRentalsInternal();
    }

    @Scheduled(fixedDelay = 60000)
    public void forceEndOverdueActiveRentals() {
        forceEndOverdueActiveRentalsInternal();
    }

    @Scheduled(fixedDelay = 30000)
    public void reconcileStaleScooterStatuses() {
        reconcileStaleScooterStatusesInternal();
    }

    private void cancelExpiredPendingRentalsInternal() {
        Instant threshold = Instant.now().minus(RESERVATION_TIMEOUT);
        List<Rental> expiredPendingRentals = rentalRepository.findByStatusAndCreatedAtBefore(
            RentalStatus.PENDING,
            threshold
        );

        expiredPendingRentals.forEach(rental -> {
            Scooter scooter = scooterRepository.findByIdForUpdate(rental.scooter.id).orElse(null);
            if (scooter != null) {
                cancelPendingRental(rental, scooter, "Reservation timed out.", null, AuditActorRole.SYSTEM);
            }
        });
    }

    private void forceEndOverdueActiveRentalsInternal() {
        Instant threshold = Instant.now().minus(MAX_RIDE_DURATION);
        rentalRepository.findByStatus(RentalStatus.ACTIVE).stream()
            .filter(rental -> rental.startTime != null && rental.startTime.isBefore(threshold))
            .forEach(rental -> {
                Rental lockedRental = rentalRepository.findByIdForUpdate(rental.id).orElse(null);
                if (lockedRental == null || lockedRental.status != RentalStatus.ACTIVE) {
                    return;
                }

                Scooter scooter = scooterRepository.findByIdForUpdate(lockedRental.scooter.id).orElse(null);
                if (scooter != null) {
                    finalizeRental(
                        lockedRental,
                        scooter,
                        RentalStatus.FORCE_ENDED,
                        "Maximum ride duration exceeded."
                    );
                    auditLogWriter.logSystemSuccess(
                        AuditActionType.RENTAL_FORCE_END,
                        AuditEntityType.RENTAL,
                        lockedRental.id,
                        payload(
                            "scooterId", scooter.id,
                            "reason", "Maximum ride duration exceeded.",
                            "durationMinutes", lockedRental.durationMinutes,
                            "batteryAtEnd", lockedRental.batteryAtEnd
                        )
                    );
                }
            });
    }

    private void reconcileStaleScooterStatusesInternal() {
        scooterRepository.findByStatusIn(STALE_OPEN_SCOOTER_STATUSES).forEach(scooter -> {
            if (rentalRepository.existsByScooterIdAndStatusIn(scooter.id, ACTIVE_OR_PENDING_STATUSES)) {
                return;
            }

            Scooter lockedScooter = scooterRepository.findByIdForUpdate(scooter.id).orElse(null);
            if (lockedScooter == null) {
                return;
            }

            if (rentalRepository.existsByScooterIdAndStatusIn(lockedScooter.id, ACTIVE_OR_PENDING_STATUSES)) {
                return;
            }

            Integer reconciledBattery = rentalRepository
                .findFirstByScooterIdAndStatusInOrderByEndTimeDesc(lockedScooter.id, CLOSED_RENTAL_STATUSES)
                .map(rental -> rental.batteryAtEnd)
                .orElse(lockedScooter.batteryPercentage);

            ScooterStatus reconciledStatus = resolveAvailableScooterStatus(
                reconciledBattery == null ? 0 : reconciledBattery
            );
            boolean batteryChanged = reconciledBattery != null
                && !reconciledBattery.equals(lockedScooter.batteryPercentage);

            if (lockedScooter.status != reconciledStatus || batteryChanged) {
                lockedScooter.status = reconciledStatus;
                lockedScooter.batteryPercentage = reconciledBattery;
                lockedScooter.lastActivityAt = Instant.now();
                scooterRepository.saveAndFlush(lockedScooter);
            }
        });
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

    private RentalResponse finalizeRental(
        Rental rental,
        Scooter scooter,
        RentalStatus finalStatus,
        String reason
    ) {
        Instant endTime = Instant.now();
        int durationMinutes = calculateDurationMinutes(rental.startTime, endTime);
        PricingConfig pricing = getOrCreateActivePricing();
        BigDecimal usageCost = rental.ratePerMinuteApplied
            .multiply(BigDecimal.valueOf(durationMinutes))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = rental.unlockFeeApplied.add(usageCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal batteryConsumptionRate = resolveBatteryConsumptionRate(pricing);
        int batteryConsumed = batteryConsumptionRate
            .multiply(BigDecimal.valueOf(durationMinutes))
            .setScale(0, RoundingMode.CEILING)
            .intValue();
        int batteryAtEnd = Math.max(0, rental.batteryAtStart - batteryConsumed);
        BigDecimal distanceTraveled = DEFAULT_DISTANCE_PER_MINUTE_KM
            .multiply(BigDecimal.valueOf(durationMinutes))
            .setScale(2, RoundingMode.HALF_UP);

        rental.endTime = endTime;
        rental.durationMinutes = durationMinutes;
        rental.batteryAtEnd = batteryAtEnd;
        rental.distanceTraveled = distanceTraveled;
        rental.totalCost = totalCost;
        rental.status = finalStatus;

        createOrUpdateReceipt(rental, usageCost, totalCost);

        applyRideCompletionToScooter(scooter, batteryAtEnd, distanceTraveled);

        rentalRepository.saveAndFlush(rental);
        persistRideCompletionToScooter(rental, scooter, distanceTraveled);

        return toResponse(rental);
    }

    private void cancelPendingRental(
        Rental rental,
        Scooter scooter,
        String reason,
        User actorUser,
        AuditActorRole actorRole
    ) {
        if (rental.status != RentalStatus.PENDING) {
            return;
        }

        rental.status = RentalStatus.CANCELLED;
        rental.endTime = Instant.now();
        rental.totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(rental.id, PaymentType.UNLOCK_FEE)
            .ifPresent(payment -> {
                payment.status = PaymentStatus.REFUNDED;
                payment.failureReason = reason;
                paymentRepository.save(payment);
                auditLogWriter.logSuccess(
                    actorUser,
                    actorRole,
                    AuditActionType.PAYMENT_REFUNDED,
                    AuditEntityType.PAYMENT,
                    payment.id,
                    paymentPayload(payment)
                );
            });

        scooter.status = resolveAvailableScooterStatus(scooter.batteryPercentage == null ? 0 : scooter.batteryPercentage);
        scooter.lastActivityAt = Instant.now();

        scooterRepository.saveAndFlush(scooter);
        rentalRepository.saveAndFlush(rental);
        auditLogWriter.logSuccess(
            actorUser,
            actorRole,
            AuditActionType.RENTAL_CANCEL,
            AuditEntityType.RENTAL,
            rental.id,
            payload("scooterId", scooter.id, "reason", reason, "status", rental.status)
        );
    }

    private void createOrUpdateReceipt(Rental rental, BigDecimal usageCost, BigDecimal totalCost) {
        Receipt receipt = receiptRepository.findByRentalId(rental.id).orElseGet(Receipt::new);
        if (receipt.id == null) {
            receipt.receiptCode = "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            receipt.rental = rental;
        }

        receipt.generatedAt = Instant.now();
        receipt.unlockFeeCharged = rental.unlockFeeApplied;
        receipt.usageCost = usageCost;
        receipt.totalCost = totalCost;
        receiptRepository.save(receipt);
    }

    private Payment createPayment(
        Rental rental,
        User user,
        PaymentType type,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String failureReason
    ) {
        Payment payment = new Payment();
        payment.rental = rental;
        payment.user = user;
        payment.type = type;
        payment.amount = amount.setScale(2, RoundingMode.HALF_UP);
        payment.status = status;
        payment.paymentMethod = paymentMethod;
        payment.transactionReference = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        payment.failureReason = failureReason;
        return payment;
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

    private PaymentMethod resolveConfiguredPaymentMethod(User user) {
        if (user.preferredPaymentMethod == null) {
            throw new BusinessRuleException(
                "Configure a payment method in your profile before making payments."
            );
        }

        return user.preferredPaymentMethod;
    }

    private void reconcileClosedRentalScooter(Rental rental, Scooter scooter) {
        Integer reconciledBattery = rental.batteryAtEnd != null
            ? rental.batteryAtEnd
            : scooter.batteryPercentage;
        ScooterStatus targetStatus = resolveAvailableScooterStatus(
            reconciledBattery == null ? 0 : reconciledBattery
        );
        boolean batteryChanged = reconciledBattery != null
            && !reconciledBattery.equals(scooter.batteryPercentage);

        if (scooter.status != targetStatus || batteryChanged) {
            scooter.status = targetStatus;
            scooter.batteryPercentage = reconciledBattery;
            scooter.lastActivityAt = Instant.now();
            scooterRepository.saveAndFlush(scooter);
        }
    }

    private void applyRideCompletionToScooter(Scooter scooter, int batteryAtEnd, BigDecimal distanceTraveled) {
        scooter.status = resolveAvailableScooterStatus(batteryAtEnd);
        scooter.batteryPercentage = batteryAtEnd;
        scooter.kilometersTraveled = (scooter.kilometersTraveled == null
            ? BigDecimal.ZERO
            : scooter.kilometersTraveled)
            .add(distanceTraveled)
            .setScale(2, RoundingMode.HALF_UP);
        scooter.lastActivityAt = Instant.now();
    }

    private void persistRideCompletionToScooter(Rental rental, Scooter scooter, BigDecimal distanceTraveled) {
        if (rental.id == null || scooter.id == null) {
            scooterRepository.saveAndFlush(scooter);
            return;
        }

        Long scooterId = rentalRepository.findScooterIdByRentalId(rental.id).orElse(scooter.id);
        int updatedRows = scooterRepository.updateRideCompletionState(
            scooterId,
            scooter.status.name(),
            scooter.batteryPercentage,
            scooter.kilometersTraveled,
            scooter.lastActivityAt
        );

        if (updatedRows == 0) {
            scooterRepository.saveAndFlush(scooter);
        }
    }

    private BigDecimal resolveBatteryConsumptionRate(PricingConfig pricing) {
        if (pricing.batteryConsumptionRate == null || pricing.batteryConsumptionRate.signum() <= 0) {
            return DEFAULT_BATTERY_CONSUMPTION_RATE;
        }
        return pricing.batteryConsumptionRate;
    }

    private ScooterStatus resolveAvailableScooterStatus(int batteryPercentage) {
        return batteryPercentage >= MINIMUM_UNLOCK_BATTERY
            ? ScooterStatus.AVAILABLE
            : ScooterStatus.LOCKED;
    }

    private Scooter getRentalScooterOrThrow(Rental rental) {
        if (rental.scooter == null) {
            throw new ResourceNotFoundException("Scooter not found.");
        }

        if (rental.scooter.id == null) {
            return rental.scooter;
        }

        return scooterRepository.findByIdForUpdate(rental.scooter.id)
            .orElse(rental.scooter);
    }

    private boolean isReservationExpired(Rental rental) {
        return rental.createdAt != null
            && rental.createdAt.plus(RESERVATION_TIMEOUT).isBefore(Instant.now());
    }

    private int calculateDurationMinutes(Instant startTime, Instant endTime) {
        if (startTime == null) {
            return 1;
        }

        long seconds = Math.max(1, Duration.between(startTime, endTime).getSeconds());
        return (int) Math.max(1, (long) Math.ceil(seconds / 60.0d));
    }

    private boolean matchesFilter(Rental rental, RentalFilterRequest filter) {
        if (filter == null) {
            return true;
        }

        if (filter.userId() != null && !filter.userId().equals(rental.user.id)) {
            return false;
        }

        if (filter.scooterId() != null && !filter.scooterId().equals(rental.scooter.id)) {
            return false;
        }

        if (filter.query() != null && !filter.query().isBlank()) {
            String query = filter.query().trim().toLowerCase();
            boolean matchesQuery = rental.scooter.publicCode.toLowerCase().contains(query)
                || rental.scooter.model.toLowerCase().contains(query)
                || rental.user.email.toLowerCase().contains(query)
                || rental.user.fullName.toLowerCase().contains(query);
            if (!matchesQuery) {
                return false;
            }
        }

        if (filter.status() != null && filter.status() != rental.status) {
            return false;
        }

        if (filter.fromDate() != null && rental.createdAt.isBefore(filter.fromDate())) {
            return false;
        }

        if (filter.toDate() != null && rental.createdAt.isAfter(filter.toDate())) {
            return false;
        }

        if (filter.minAmount() != null) {
            BigDecimal totalCost = rental.totalCost == null ? BigDecimal.ZERO : rental.totalCost;
            if (totalCost.compareTo(filter.minAmount()) < 0) {
                return false;
            }
        }

        if (filter.maxAmount() != null) {
            BigDecimal totalCost = rental.totalCost == null ? BigDecimal.ZERO : rental.totalCost;
            if (totalCost.compareTo(filter.maxAmount()) > 0) {
                return false;
            }
        }

        return true;
    }

    private PageResponse<RentalResponse> paginate(List<RentalResponse> rentals, Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size <= 0
            ? DEFAULT_SIZE
            : Math.min(size, MAX_PAGE_SIZE);

        int fromIndex = Math.min(resolvedPage * resolvedSize, rentals.size());
        int toIndex = Math.min(fromIndex + resolvedSize, rentals.size());
        int totalPages = rentals.isEmpty() ? 0 : (int) Math.ceil((double) rentals.size() / resolvedSize);

        return new PageResponse<>(
            rentals.subList(fromIndex, toIndex),
            resolvedPage,
            resolvedSize,
            (long) rentals.size(),
            totalPages
        );
    }

    private RentalResponse toResponse(Rental rental) {
        return new RentalResponse(
            rental.id,
            rental.user.id,
            rental.scooter.id,
            rental.scooter.publicCode,
            rental.scooter.model,
            rental.startTime,
            rental.endTime,
            rental.status,
            rental.batteryAtStart,
            rental.batteryAtEnd,
            rental.distanceTraveled,
            rental.durationMinutes,
            rental.unlockFeeApplied,
            rental.ratePerMinuteApplied,
            rental.totalCost,
            receiptRepository.findByRentalId(rental.id).isPresent(),
            rental.createdAt,
            rental.updatedAt
        );
    }

    private ActiveRentalResponse toActiveRentalResponse(Rental rental) {
        return new ActiveRentalResponse(
            rental.id,
            rental.scooter.id,
            rental.scooter.publicCode,
            rental.scooter.model,
            rental.status,
            rental.createdAt,
            rental.startTime
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private AuditActorRole resolveAuditActorRole(User user) {
        if (user == null || user.role == null) {
            return AuditActorRole.SYSTEM;
        }
        return user.role == UserRole.ADMIN ? AuditActorRole.ADMIN : AuditActorRole.CUSTOMER;
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

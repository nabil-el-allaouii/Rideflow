package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.scooter.ScooterCreateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterFilterRequest;
import com.rideflow.demo.api.dto.scooter.ScooterResponse;
import com.rideflow.demo.api.dto.scooter.ScooterStatusUpdateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterUpdateRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AuditLogWriter;
import com.rideflow.demo.service.ScooterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScooterServiceImpl implements ScooterService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MINIMUM_UNLOCK_BATTERY = 15;
    private static final Set<RentalStatus> ACTIVE_RENTAL_STATUSES = EnumSet.of(
        RentalStatus.PENDING,
        RentalStatus.ACTIVE
    );

    private final ScooterRepository scooterRepository;
    private final RentalRepository rentalRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogWriter auditLogWriter;

    public ScooterServiceImpl(
        ScooterRepository scooterRepository,
        RentalRepository rentalRepository,
        AuthenticatedUserService authenticatedUserService,
        AuditLogWriter auditLogWriter
    ) {
        this.scooterRepository = scooterRepository;
        this.rentalRepository = rentalRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public PageResponse<ScooterResponse> findAll(ScooterFilterRequest filter) {
        List<ScooterResponse> responses = applyFilters(scooterRepository.findAll(), filter, false);
        return paginate(responses, filter.page(), filter.size());
    }

    @Override
    public PageResponse<ScooterResponse> findAvailable(ScooterFilterRequest filter) {
        List<ScooterResponse> responses = applyFilters(scooterRepository.findAll(), filter, true);
        return paginate(responses, filter.page(), filter.size());
    }

    @Override
    public ScooterResponse findById(Long scooterId) {
        return toResponse(findScooterOrThrow(scooterId), null);
    }

    @Override
    public ScooterResponse create(ScooterCreateRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        validateKilometers(request.kilometersTraveled());

        String publicCode = normalizePublicCode(request.publicCode());
        if (scooterRepository.existsByPublicCode(publicCode)) {
            throw new BusinessRuleException("A scooter with this public code already exists.");
        }

        Scooter scooter = new Scooter();
        scooter.publicCode = publicCode;
        scooter.model = request.model().trim();
        scooter.batteryPercentage = request.batteryPercentage();
        scooter.latitude = request.latitude();
        scooter.longitude = request.longitude();
        scooter.address = normalizeOptional(request.address());
        scooter.status = request.batteryPercentage() >= MINIMUM_UNLOCK_BATTERY
            ? ScooterStatus.AVAILABLE
            : ScooterStatus.LOCKED;
        scooter.kilometersTraveled = request.kilometersTraveled() == null
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : request.kilometersTraveled().setScale(2, RoundingMode.HALF_UP);
        scooter.maintenanceNotes = normalizeOptional(request.maintenanceNotes());
        scooter.lastActivityAt = Instant.now();

        Scooter savedScooter = scooterRepository.save(scooter);
        auditLogWriter.logSuccess(
            AuditActionType.SCOOTER_CREATE,
            AuditEntityType.SCOOTER,
            savedScooter.id,
            scooterPayload(savedScooter)
        );
        return toResponse(savedScooter, null);
    }

    @Override
    public ScooterResponse update(Long scooterId, ScooterUpdateRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        validateKilometers(request.kilometersTraveled());

        Scooter scooter = findScooterOrThrow(scooterId);
        scooter.model = request.model().trim();
        scooter.batteryPercentage = request.batteryPercentage();
        scooter.latitude = request.latitude();
        scooter.longitude = request.longitude();
        scooter.address = normalizeOptional(request.address());
        scooter.kilometersTraveled = request.kilometersTraveled() == null
            ? null
            : request.kilometersTraveled().setScale(2, RoundingMode.HALF_UP);
        scooter.maintenanceNotes = normalizeOptional(request.maintenanceNotes());
        scooter.lastActivityAt = Instant.now();

        if (scooter.status == ScooterStatus.AVAILABLE && scooter.batteryPercentage < MINIMUM_UNLOCK_BATTERY) {
            scooter.status = ScooterStatus.LOCKED;
        } else if (scooter.status == ScooterStatus.LOCKED
            && scooter.batteryPercentage >= MINIMUM_UNLOCK_BATTERY) {
            scooter.status = ScooterStatus.AVAILABLE;
        }

        Scooter savedScooter = scooterRepository.save(scooter);
        auditLogWriter.logSuccess(
            AuditActionType.SCOOTER_UPDATE,
            AuditEntityType.SCOOTER,
            savedScooter.id,
            scooterPayload(savedScooter)
        );
        return toResponse(savedScooter, null);
    }

    @Override
    public ScooterResponse updateStatus(Long scooterId, ScooterStatusUpdateRequest request) {
        Scooter scooter = findScooterOrThrow(scooterId);
        ScooterStatus targetStatus = request.status();

        validateStatusTransition(scooter, targetStatus);

        if (targetStatus == ScooterStatus.AVAILABLE
            && scooter.batteryPercentage < MINIMUM_UNLOCK_BATTERY) {
            throw new BusinessRuleException("Scooter battery must be at least 15% to mark it as available.");
        }

        ScooterStatus previousStatus = scooter.status;
        scooter.status = targetStatus;
        scooter.lastActivityAt = Instant.now();
        Scooter savedScooter = scooterRepository.save(scooter);
        auditLogWriter.logSuccess(
            AuditActionType.SCOOTER_STATUS_CHANGE,
            AuditEntityType.SCOOTER,
            savedScooter.id,
            payload("previousStatus", previousStatus, "newStatus", targetStatus)
        );
        return toResponse(savedScooter, null);
    }

    @Override
    public void delete(Long scooterId) {
        Scooter scooter = findScooterOrThrow(scooterId);

        if (rentalRepository.existsByScooterIdAndStatusIn(scooterId, ACTIVE_RENTAL_STATUSES)) {
            throw new BusinessRuleException("Scooter cannot be deleted while it has an active or pending rental.");
        }

        auditLogWriter.logSuccess(
            AuditActionType.SCOOTER_DELETE,
            AuditEntityType.SCOOTER,
            scooter.id,
            scooterPayload(scooter)
        );
        scooterRepository.delete(scooter);
    }

    private List<ScooterResponse> applyFilters(
        List<Scooter> scooters,
        ScooterFilterRequest filter,
        boolean availableOnly
    ) {
        int minimumBattery = availableOnly
            ? Math.max(filter.minBattery() == null ? MINIMUM_UNLOCK_BATTERY : filter.minBattery(), MINIMUM_UNLOCK_BATTERY)
            : filter.minBattery() == null ? 0 : filter.minBattery();

        String normalizedQuery = filter.query() == null ? null : filter.query().trim().toLowerCase(Locale.ROOT);

        return scooters.stream()
            .filter(scooter -> !availableOnly || scooter.status == ScooterStatus.AVAILABLE)
            .filter(scooter -> filter.status() == null || scooter.status == filter.status())
            .filter(scooter -> scooter.batteryPercentage >= minimumBattery)
            .filter(scooter -> normalizedQuery == null || normalizedQuery.isBlank() || matchesQuery(scooter, normalizedQuery))
            .map(scooter -> toResponse(scooter, calculateDistance(filter.latitude(), filter.longitude(), scooter)))
            .filter(response -> filter.radiusKm() == null
                || response.distanceKm() == null
                || response.distanceKm() <= filter.radiusKm().doubleValue())
            .sorted(Comparator
                .comparing((ScooterResponse response) -> response.distanceKm() == null)
                .thenComparing(response -> response.distanceKm() == null ? Double.MAX_VALUE : response.distanceKm())
                .thenComparing(ScooterResponse::publicCode))
            .collect(Collectors.toList());
    }

    private ScooterResponse toResponse(Scooter scooter, Double distanceKmOverride) {
        Double distanceKm = distanceKmOverride;

        UnlockValidation unlockValidation = evaluateUnlockability(scooter);

        return new ScooterResponse(
            scooter.id,
            scooter.publicCode,
            scooter.model,
            scooter.batteryPercentage,
            scooter.latitude,
            scooter.longitude,
            scooter.address,
            scooter.status,
            scooter.kilometersTraveled,
            scooter.maintenanceNotes,
            scooter.lastActivityAt,
            distanceKm,
            unlockValidation.unlockable(),
            unlockValidation.reason()
        );
    }

    private UnlockValidation evaluateUnlockability(Scooter scooter) {
        if (scooter.status != ScooterStatus.AVAILABLE) {
            return new UnlockValidation(false, "Scooter is not available.");
        }

        if (scooter.batteryPercentage < MINIMUM_UNLOCK_BATTERY) {
            return new UnlockValidation(false, "Battery must be at least 15%.");
        }

        if (rentalRepository.existsByScooterIdAndStatusIn(scooter.id, ACTIVE_RENTAL_STATUSES)) {
            return new UnlockValidation(false, "Scooter already has an active rental.");
        }

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        if (rentalRepository.existsByUserIdAndStatusIn(currentUserId, ACTIVE_RENTAL_STATUSES)) {
            return new UnlockValidation(false, "You already have an active rental.");
        }

        return new UnlockValidation(true, null);
    }

    private boolean matchesQuery(Scooter scooter, String query) {
        return scooter.publicCode.toLowerCase(Locale.ROOT).contains(query)
            || scooter.model.toLowerCase(Locale.ROOT).contains(query)
            || (scooter.address != null && scooter.address.toLowerCase(Locale.ROOT).contains(query));
    }

    private PageResponse<ScooterResponse> paginate(List<ScooterResponse> responses, Integer page, Integer size) {
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

    private Scooter findScooterOrThrow(Long scooterId) {
        return scooterRepository.findById(scooterId)
            .orElseThrow(() -> new ResourceNotFoundException("Scooter not found."));
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessRuleException("Latitude and longitude must both be provided together.");
        }

        if (latitude != null && (latitude.doubleValue() < -90 || latitude.doubleValue() > 90)) {
            throw new BusinessRuleException("Latitude must be between -90 and 90.");
        }

        if (longitude != null && (longitude.doubleValue() < -180 || longitude.doubleValue() > 180)) {
            throw new BusinessRuleException("Longitude must be between -180 and 180.");
        }
    }

    private void validateKilometers(BigDecimal kilometersTraveled) {
        if (kilometersTraveled != null && kilometersTraveled.signum() < 0) {
            throw new BusinessRuleException("Kilometers traveled cannot be negative.");
        }
    }

    private void validateStatusTransition(Scooter scooter, ScooterStatus targetStatus) {
        if (scooter.status == targetStatus) {
            return;
        }

        if (rentalRepository.existsByScooterIdAndStatusIn(scooter.id, List.of(RentalStatus.ACTIVE))
            && targetStatus != ScooterStatus.IN_USE
            && targetStatus != ScooterStatus.MAINTENANCE
            && targetStatus != ScooterStatus.DISABLED
            && targetStatus != ScooterStatus.LOCKED) {
            throw new BusinessRuleException("Scooter has an active rental and cannot transition to this status.");
        }

        boolean allowed = switch (scooter.status) {
            case AVAILABLE -> EnumSet.of(
                ScooterStatus.RESERVED,
                ScooterStatus.MAINTENANCE,
                ScooterStatus.DISABLED,
                ScooterStatus.LOCKED
            ).contains(targetStatus);
            case RESERVED -> EnumSet.of(
                ScooterStatus.IN_USE,
                ScooterStatus.AVAILABLE,
                ScooterStatus.MAINTENANCE,
                ScooterStatus.DISABLED,
                ScooterStatus.LOCKED
            ).contains(targetStatus);
            case IN_USE -> EnumSet.of(
                ScooterStatus.LOCKED,
                ScooterStatus.MAINTENANCE,
                ScooterStatus.DISABLED
            ).contains(targetStatus);
            case LOCKED -> EnumSet.of(
                ScooterStatus.AVAILABLE,
                ScooterStatus.MAINTENANCE,
                ScooterStatus.DISABLED
            ).contains(targetStatus);
            case MAINTENANCE -> EnumSet.of(
                ScooterStatus.AVAILABLE,
                ScooterStatus.DISABLED,
                ScooterStatus.LOCKED
            ).contains(targetStatus);
            case DISABLED -> EnumSet.of(
                ScooterStatus.AVAILABLE,
                ScooterStatus.MAINTENANCE,
                ScooterStatus.LOCKED
            ).contains(targetStatus);
        };

        if (!allowed) {
            throw new BusinessRuleException(
                "Scooter cannot transition from " + scooter.status + " to " + targetStatus + "."
            );
        }
    }

    private String normalizePublicCode(String publicCode) {
        return publicCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Double calculateDistance(BigDecimal originLatitude, BigDecimal originLongitude, Scooter scooter) {
        if (originLatitude == null
            || originLongitude == null
            || scooter.latitude == null
            || scooter.longitude == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double lat1 = Math.toRadians(originLatitude.doubleValue());
        double lon1 = Math.toRadians(originLongitude.doubleValue());
        double lat2 = Math.toRadians(scooter.latitude.doubleValue());
        double lon2 = Math.toRadians(scooter.longitude.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BigDecimal.valueOf(earthRadiusKm * c)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private Map<String, Object> scooterPayload(Scooter scooter) {
        return payload(
            "publicCode", scooter.publicCode,
            "model", scooter.model,
            "batteryPercentage", scooter.batteryPercentage,
            "status", scooter.status,
            "latitude", scooter.latitude,
            "longitude", scooter.longitude
        );
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            payload.put(String.valueOf(values[index]), values[index + 1]);
        }
        return payload;
    }

    private record UnlockValidation(boolean unlockable, String reason) {
    }
}

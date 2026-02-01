package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.scooter.ScooterCreateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterFilterRequest;
import com.rideflow.demo.api.dto.scooter.ScooterStatusUpdateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterUpdateRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AuditLogWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScooterServiceImplTest {

    @Mock
    private ScooterRepository scooterRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private ScooterServiceImpl scooterService;

    @Test
    void createNormalizesCodeAndLocksLowBatteryScooter() {
        ScooterCreateRequest request = new ScooterCreateRequest(
            " rf-100 ",
            "Model X",
            10,
            new BigDecimal("33.5731"),
            new BigDecimal("-7.5898"),
            "Casablanca",
            new BigDecimal("0"),
            null
        );

        when(scooterRepository.existsByPublicCode("RF-100")).thenReturn(false);
        when(scooterRepository.save(any(Scooter.class))).thenAnswer(invocation -> {
            Scooter scooter = invocation.getArgument(0);
            scooter.id = 1L;
            return scooter;
        });

        var response = scooterService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.publicCode()).isEqualTo("RF-100");
        assertThat(response.status()).isEqualTo(ScooterStatus.LOCKED);
        assertThat(response.kilometersTraveled()).isEqualByComparingTo("0.00");
        verify(auditLogWriter).logSuccess(any(), any(), any(), any());
    }

    @Test
    void updateMovesAvailableScooterToLockedWhenBatteryDropsBelowThreshold() {
        Scooter scooter = scooter(5L, ScooterStatus.AVAILABLE, 80);
        when(scooterRepository.findById(5L)).thenReturn(Optional.of(scooter));
        when(scooterRepository.save(any(Scooter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = scooterService.update(
            5L,
            new ScooterUpdateRequest(
                "Updated Model",
                12,
                null,
                null,
                "Dock A",
                new BigDecimal("12.40"),
                "Needs inspection"
            )
        );

        assertThat(response.status()).isEqualTo(ScooterStatus.LOCKED);
        assertThat(scooter.model).isEqualTo("Updated Model");
        assertThat(scooter.kilometersTraveled).isEqualByComparingTo("12.40");
    }

    @Test
    void updateStatusRejectsAvailableWhenBatteryIsTooLow() {
        Scooter scooter = scooter(7L, ScooterStatus.LOCKED, 12);
        when(scooterRepository.findById(7L)).thenReturn(Optional.of(scooter));

        assertThatThrownBy(() -> scooterService.updateStatus(7L, new ScooterStatusUpdateRequest(ScooterStatus.AVAILABLE)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Scooter battery must be at least 15% to mark it as available.");
    }

    @Test
    void findAvailableFiltersByBatteryDistanceAndActiveRentalState() {
        Scooter availableNear = scooter(1L, ScooterStatus.AVAILABLE, 90);
        availableNear.publicCode = "RF-001";
        availableNear.latitude = new BigDecimal("33.5731000");
        availableNear.longitude = new BigDecimal("-7.5898000");

        Scooter availableFar = scooter(2L, ScooterStatus.AVAILABLE, 90);
        availableFar.publicCode = "RF-002";
        availableFar.latitude = new BigDecimal("35.0000000");
        availableFar.longitude = new BigDecimal("-7.5898000");

        Scooter locked = scooter(3L, ScooterStatus.LOCKED, 90);

        when(scooterRepository.findAll()).thenReturn(List.of(availableFar, locked, availableNear));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(9L);
        when(rentalRepository.existsByUserIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(rentalRepository.existsByScooterIdAndStatusIn(anyLong(), any())).thenAnswer(invocation -> {
            Long scooterId = invocation.getArgument(0);
            return scooterId.equals(2L);
        });

        var page = scooterService.findAvailable(
            new ScooterFilterRequest(
                "rf",
                null,
                20,
                new BigDecimal("33.5731000"),
                new BigDecimal("-7.5898000"),
                new BigDecimal("5"),
                0,
                10
            )
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(1L);
        assertThat(page.content().get(0).unlockable()).isTrue();
        assertThat(page.content().get(0).distanceKm()).isEqualTo(0.0d);
    }

    private Scooter scooter(Long id, ScooterStatus status, int battery) {
        Scooter scooter = new Scooter();
        scooter.id = id;
        scooter.publicCode = "RF-" + id;
        scooter.model = "Model-" + id;
        scooter.status = status;
        scooter.batteryPercentage = battery;
        scooter.kilometersTraveled = BigDecimal.ZERO.setScale(2);
        return scooter;
    }
}

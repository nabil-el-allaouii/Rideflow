package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.pricing.PricingConfigRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.model.PricingConfig;
import com.rideflow.demo.domain.repository.PricingConfigRepository;
import com.rideflow.demo.service.AuditLogWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private PricingServiceImpl pricingService;

    @Test
    void getCurrentPricingCreatesDefaultConfigWhenNoneExists() {
        when(pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()).thenReturn(Optional.empty());
        when(pricingConfigRepository.saveAndFlush(any(PricingConfig.class))).thenAnswer(invocation -> {
            PricingConfig config = invocation.getArgument(0);
            config.id = 10L;
            return config;
        });

        var response = pricingService.getCurrentPricing();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.unlockFee()).isEqualByComparingTo("1.00");
        assertThat(response.ratePerMinute()).isEqualByComparingTo("0.15");
        assertThat(response.batteryConsumptionRate()).isEqualByComparingTo("0.50");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.active()).isTrue();
    }

    @Test
    void updatePricingDeactivatesPreviousActiveConfigAndPersistsNewSnapshot() {
        PricingConfig existing = new PricingConfig();
        existing.id = 2L;
        existing.active = true;
        existing.currency = "USD";
        existing.effectiveFrom = Instant.parse("2026-03-20T10:15:30Z");

        when(pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()).thenReturn(Optional.of(existing));
        when(pricingConfigRepository.save(any(PricingConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pricingConfigRepository.saveAndFlush(any(PricingConfig.class))).thenAnswer(invocation -> {
            PricingConfig config = invocation.getArgument(0);
            config.id = 3L;
            return config;
        });

        var response = pricingService.updatePricing(
            new PricingConfigRequest(
                new BigDecimal("2"),
                new BigDecimal("0.4"),
                new BigDecimal("0.75"),
                " mad "
            )
        );

        ArgumentCaptor<PricingConfig> captor = ArgumentCaptor.forClass(PricingConfig.class);
        verify(pricingConfigRepository).saveAndFlush(captor.capture());

        PricingConfig saved = captor.getValue();
        assertThat(existing.active).isFalse();
        assertThat(saved.unlockFee).isEqualByComparingTo("2.00");
        assertThat(saved.ratePerMinute).isEqualByComparingTo("0.40");
        assertThat(saved.batteryConsumptionRate).isEqualByComparingTo("0.75");
        assertThat(saved.currency).isEqualTo("MAD");

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.unlockFee()).isEqualByComparingTo("2.00");
        assertThat(response.currency()).isEqualTo("MAD");
    }

    @Test
    void updatePricingRejectsInvalidCurrency() {
        assertThatThrownBy(() -> pricingService.updatePricing(
            new PricingConfigRequest(
                new BigDecimal("1.00"),
                new BigDecimal("0.20"),
                new BigDecimal("0.50"),
                "           "
            )
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Currency is required.");

        verify(pricingConfigRepository, never()).saveAndFlush(any(PricingConfig.class));
    }
}

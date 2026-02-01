package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.pricing.PricingConfigRequest;
import com.rideflow.demo.api.dto.pricing.PricingConfigResponse;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.model.PricingConfig;
import com.rideflow.demo.domain.repository.PricingConfigRepository;
import com.rideflow.demo.service.AuditLogWriter;
import com.rideflow.demo.service.PricingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal DEFAULT_UNLOCK_FEE = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_RATE_PER_MINUTE = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_BATTERY_CONSUMPTION_RATE = new BigDecimal("0.50");
    private static final String DEFAULT_CURRENCY = "USD";

    private final PricingConfigRepository pricingConfigRepository;
    private final AuditLogWriter auditLogWriter;

    public PricingServiceImpl(
        PricingConfigRepository pricingConfigRepository,
        AuditLogWriter auditLogWriter
    ) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public PricingConfigResponse getCurrentPricing() {
        return toResponse(getOrCreateActivePricing());
    }

    @Override
    public PricingConfigResponse updatePricing(PricingConfigRequest request) {
        validateRequest(request);

        pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
            .ifPresent(config -> {
                config.active = false;
                pricingConfigRepository.save(config);
            });

        PricingConfig pricingConfig = new PricingConfig();
        pricingConfig.unlockFee = request.unlockFee().setScale(2, RoundingMode.HALF_UP);
        pricingConfig.ratePerMinute = request.ratePerMinute().setScale(2, RoundingMode.HALF_UP);
        pricingConfig.batteryConsumptionRate = request.batteryConsumptionRate().setScale(2, RoundingMode.HALF_UP);
        pricingConfig.currency = request.currency().trim().toUpperCase();
        pricingConfig.effectiveFrom = Instant.now();
        pricingConfig.active = true;

        PricingConfig savedConfig = pricingConfigRepository.saveAndFlush(pricingConfig);
        auditLogWriter.logSuccess(
            AuditActionType.SCOOTER_UPDATE,
            AuditEntityType.PRICING_CONFIG,
            savedConfig.id,
            payload(savedConfig)
        );

        return toResponse(savedConfig);
    }

    private PricingConfig getOrCreateActivePricing() {
        return pricingConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
            .orElseGet(() -> {
                PricingConfig pricingConfig = new PricingConfig();
                pricingConfig.unlockFee = DEFAULT_UNLOCK_FEE;
                pricingConfig.ratePerMinute = DEFAULT_RATE_PER_MINUTE;
                pricingConfig.batteryConsumptionRate = DEFAULT_BATTERY_CONSUMPTION_RATE;
                pricingConfig.currency = DEFAULT_CURRENCY;
                pricingConfig.effectiveFrom = Instant.now();
                pricingConfig.active = true;
                return pricingConfigRepository.saveAndFlush(pricingConfig);
            });
    }

    private void validateRequest(PricingConfigRequest request) {
        String currency = request.currency() == null ? "" : request.currency().trim();
        if (currency.isEmpty()) {
            throw new BusinessRuleException("Currency is required.");
        }

        if (currency.length() > 10) {
            throw new BusinessRuleException("Currency must be 10 characters or fewer.");
        }
    }

    private PricingConfigResponse toResponse(PricingConfig pricingConfig) {
        return new PricingConfigResponse(
            pricingConfig.id,
            pricingConfig.unlockFee,
            pricingConfig.ratePerMinute,
            pricingConfig.batteryConsumptionRate,
            pricingConfig.currency,
            pricingConfig.effectiveFrom,
            pricingConfig.active
        );
    }

    private Map<String, Object> payload(PricingConfig pricingConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unlockFee", pricingConfig.unlockFee);
        payload.put("ratePerMinute", pricingConfig.ratePerMinute);
        payload.put("batteryConsumptionRate", pricingConfig.batteryConsumptionRate);
        payload.put("currency", pricingConfig.currency);
        payload.put("effectiveFrom", pricingConfig.effectiveFrom);
        payload.put("active", pricingConfig.active);
        return payload;
    }
}

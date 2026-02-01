package com.rideflow.demo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "pricing_configs",
    indexes = {
        @Index(name = "idx_pricing_active", columnList = "is_active"),
        @Index(name = "idx_pricing_effective_from", columnList = "effective_from")
    }
)
public class PricingConfig extends BaseEntity {

    @Column(name = "unlock_fee", nullable = false, precision = 10, scale = 2)
    public BigDecimal unlockFee;

    @Column(name = "rate_per_minute", nullable = false, precision = 10, scale = 2)
    public BigDecimal ratePerMinute;

    @Column(name = "battery_consumption_rate", nullable = false, precision = 5, scale = 2)
    public BigDecimal batteryConsumptionRate;

    @Column(name = "currency", nullable = false, length = 10)
    public String currency;

    @Column(name = "effective_from", nullable = false)
    public Instant effectiveFrom;

    @Column(name = "is_active", nullable = false)
    public Boolean active;
}

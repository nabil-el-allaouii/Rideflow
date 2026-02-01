package com.rideflow.demo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "receipts",
    indexes = {
        @Index(name = "idx_receipts_rental_id", columnList = "rental_id"),
        @Index(name = "idx_receipts_receipt_code", columnList = "receipt_code")
    }
)
public class Receipt extends BaseEntity {

    @Column(name = "receipt_code", nullable = false, unique = true, length = 80)
    public String receiptCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    public Rental rental;

    @Column(name = "generated_at", nullable = false)
    public Instant generatedAt;

    @Column(name = "unlock_fee_charged", precision = 10, scale = 2)
    public BigDecimal unlockFeeCharged;

    @Column(name = "usage_cost", precision = 10, scale = 2)
    public BigDecimal usageCost;

    @Column(name = "total_cost", precision = 10, scale = 2)
    public BigDecimal totalCost;
}

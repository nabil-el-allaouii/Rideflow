package com.rideflow.demo.domain.model;

import com.rideflow.demo.domain.enums.RentalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(
    name = "rentals",
    indexes = {
        @Index(name = "idx_rentals_user_id", columnList = "user_id"),
        @Index(name = "idx_rentals_scooter_id", columnList = "scooter_id"),
        @Index(name = "idx_rentals_status", columnList = "status"),
        @Index(name = "idx_rentals_created_at", columnList = "created_at")
    }
)
public class Rental extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scooter_id", nullable = false)
    public Scooter scooter;

    @Column(name = "start_time")
    public Instant startTime;

    @Column(name = "end_time")
    public Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public RentalStatus status;

    @Column(name = "battery_at_start")
    public Integer batteryAtStart;

    @Column(name = "battery_at_end")
    public Integer batteryAtEnd;

    @Column(name = "distance_traveled", precision = 12, scale = 2)
    public BigDecimal distanceTraveled;

    @Column(name = "duration_minutes")
    public Integer durationMinutes;

    @Column(name = "unlock_fee_applied", nullable = false, precision = 10, scale = 2)
    public BigDecimal unlockFeeApplied;

    @Column(name = "rate_per_minute_applied", nullable = false, precision = 10, scale = 2)
    public BigDecimal ratePerMinuteApplied;

    @Column(name = "total_cost", precision = 10, scale = 2)
    public BigDecimal totalCost;

    @OneToMany(mappedBy = "rental")
    public List<Payment> payments;

    @OneToOne(mappedBy = "rental")
    public Receipt receipt;
}

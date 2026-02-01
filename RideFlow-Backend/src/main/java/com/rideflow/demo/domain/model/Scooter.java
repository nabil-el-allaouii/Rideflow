package com.rideflow.demo.domain.model;

import com.rideflow.demo.domain.enums.ScooterStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(
    name = "scooters",
    indexes = {
        @Index(name = "idx_scooters_public_code", columnList = "public_code"),
        @Index(name = "idx_scooters_status", columnList = "status")
    }
)
public class Scooter extends BaseEntity {

    @Column(name = "public_code", nullable = false, unique = true, length = 30)
    public String publicCode;

    @Column(name = "model", nullable = false, length = 100)
    public String model;

    @Column(name = "battery_percentage", nullable = false)
    public Integer batteryPercentage;

    @Column(name = "latitude", precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    public BigDecimal longitude;

    @Column(name = "address", length = 500)
    public String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public ScooterStatus status;

    @Column(name = "kilometers_traveled", precision = 12, scale = 2)
    public BigDecimal kilometersTraveled;

    @Column(name = "maintenance_notes", length = 2000)
    public String maintenanceNotes;

    @Column(name = "last_activity_at")
    public Instant lastActivityAt;

    @OneToMany(mappedBy = "scooter")
    public List<Rental> rentals;
}

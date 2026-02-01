package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.model.Scooter;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScooterRepository extends JpaRepository<Scooter, Long> {
    Optional<Scooter> findByPublicCode(String publicCode);
    boolean existsByPublicCode(String publicCode);
    boolean existsByPublicCodeAndIdNot(String publicCode, Long id);
    List<Scooter> findByStatus(ScooterStatus status);
    List<Scooter> findByStatusIn(Collection<ScooterStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select scooter from Scooter scooter where scooter.id = :scooterId")
    Optional<Scooter> findByIdForUpdate(@Param("scooterId") Long scooterId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update scooters
            set status = :status,
                battery_percentage = :batteryPercentage,
                kilometers_traveled = :kilometersTraveled,
                last_activity_at = :lastActivityAt
            where id = :scooterId
            """,
        nativeQuery = true
    )
    int updateRideCompletionState(
        @Param("scooterId") Long scooterId,
        @Param("status") String status,
        @Param("batteryPercentage") Integer batteryPercentage,
        @Param("kilometersTraveled") BigDecimal kilometersTraveled,
        @Param("lastActivityAt") Instant lastActivityAt
    );
}

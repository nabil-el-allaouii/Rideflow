package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.model.Rental;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findByUserId(Long userId);
    List<Rental> findByScooterId(Long scooterId);
    List<Rental> findByScooterIdAndStatusIn(Long scooterId, Collection<RentalStatus> statuses);
    List<Rental> findByStatus(RentalStatus status);
    List<Rental> findByStatusAndCreatedAtBefore(RentalStatus status, java.time.Instant before);
    Optional<Rental> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RentalStatus status);
    Optional<Rental> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<RentalStatus> statuses);
    Optional<Rental> findFirstByScooterIdAndStatusInOrderByEndTimeDesc(Long scooterId, Collection<RentalStatus> statuses);
    boolean existsByScooterIdAndStatusIn(Long scooterId, Collection<RentalStatus> statuses);
    boolean existsByUserIdAndStatusIn(Long userId, Collection<RentalStatus> statuses);
    Page<Rental> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Rental> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select rental
        from Rental rental
        join fetch rental.user
        join fetch rental.scooter
        where rental.id = :rentalId
        """)
    Optional<Rental> findByIdForUpdate(@Param("rentalId") Long rentalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select rental
        from Rental rental
        join fetch rental.user
        join fetch rental.scooter
        where rental.id = :rentalId
          and rental.user.id = :userId
        """)
    Optional<Rental> findByIdAndUserIdForUpdate(@Param("rentalId") Long rentalId, @Param("userId") Long userId);

    @Query("select rental.scooter.id from Rental rental where rental.id = :rentalId")
    Optional<Long> findScooterIdByRentalId(@Param("rentalId") Long rentalId);
}

package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.model.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    @Query("""
        select receipt
        from Receipt receipt
        join fetch receipt.rental rental
        join fetch rental.user user
        join fetch rental.scooter scooter
        where rental.id = :rentalId
        """)
    Optional<Receipt> findByRentalId(@Param("rentalId") Long rentalId);
}

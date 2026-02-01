package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.enums.PaymentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "rental", "rental.scooter"})
    List<Payment> findAll();

    @EntityGraph(attributePaths = {"user", "rental", "rental.scooter"})
    List<Payment> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "rental", "rental.scooter"})
    List<Payment> findByRentalId(Long rentalId);

    @EntityGraph(attributePaths = {"user", "rental", "rental.scooter"})
    Optional<Payment> findFirstByRentalIdAndTypeOrderByCreatedAtDesc(Long rentalId, PaymentType type);

    @Override
    @EntityGraph(attributePaths = {"user", "rental", "rental.scooter"})
    Optional<Payment> findById(Long paymentId);

    Optional<Payment> findByTransactionReference(String transactionReference);
}

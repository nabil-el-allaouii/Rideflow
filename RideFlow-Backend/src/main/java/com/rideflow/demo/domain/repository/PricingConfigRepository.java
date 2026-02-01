package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.model.PricingConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
    Optional<PricingConfig> findFirstByActiveTrueOrderByEffectiveFromDesc();
}

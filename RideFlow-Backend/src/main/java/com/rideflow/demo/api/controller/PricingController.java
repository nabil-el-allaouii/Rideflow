package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.pricing.PricingConfigRequest;
import com.rideflow.demo.api.dto.pricing.PricingConfigResponse;
import com.rideflow.demo.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pricing")
public class PricingController {
    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PricingConfigResponse> getCurrentPricing() {
        return ResponseEntity.ok(pricingService.getCurrentPricing());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PricingConfigResponse> updatePricing(@Valid @RequestBody PricingConfigRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(pricingService.updatePricing(request));
    }
}

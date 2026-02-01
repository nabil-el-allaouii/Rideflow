package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.pricing.PricingConfigRequest;
import com.rideflow.demo.api.dto.pricing.PricingConfigResponse;

public interface PricingService {
    PricingConfigResponse getCurrentPricing();
    PricingConfigResponse updatePricing(PricingConfigRequest request);
}

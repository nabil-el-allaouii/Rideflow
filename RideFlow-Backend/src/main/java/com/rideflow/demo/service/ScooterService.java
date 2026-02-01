package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.scooter.ScooterCreateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterFilterRequest;
import com.rideflow.demo.api.dto.scooter.ScooterResponse;
import com.rideflow.demo.api.dto.scooter.ScooterStatusUpdateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterUpdateRequest;

public interface ScooterService {
    PageResponse<ScooterResponse> findAll(ScooterFilterRequest filter);
    PageResponse<ScooterResponse> findAvailable(ScooterFilterRequest filter);
    ScooterResponse findById(Long scooterId);
    ScooterResponse create(ScooterCreateRequest request);
    ScooterResponse update(Long scooterId, ScooterUpdateRequest request);
    ScooterResponse updateStatus(Long scooterId, ScooterStatusUpdateRequest request);
    void delete(Long scooterId);
}

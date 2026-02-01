package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.scooter.ScooterCreateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterFilterRequest;
import com.rideflow.demo.api.dto.scooter.ScooterResponse;
import com.rideflow.demo.api.dto.scooter.ScooterStatusUpdateRequest;
import com.rideflow.demo.api.dto.scooter.ScooterUpdateRequest;
import com.rideflow.demo.service.ScooterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scooters")
public class ScooterController {

    private final ScooterService scooterService;

    public ScooterController(ScooterService scooterService) {
        this.scooterService = scooterService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ScooterResponse>> findAll(@Valid @ModelAttribute ScooterFilterRequest filter) {
        return ResponseEntity.ok(scooterService.findAll(filter));
    }

    @GetMapping("/available")
    public ResponseEntity<PageResponse<ScooterResponse>> findAvailable(@Valid @ModelAttribute ScooterFilterRequest filter) {
        return ResponseEntity.ok(scooterService.findAvailable(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScooterResponse> findById(@PathVariable("id") Long scooterId) {
        return ResponseEntity.ok(scooterService.findById(scooterId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScooterResponse> create(@Valid @RequestBody ScooterCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scooterService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScooterResponse> update(
        @PathVariable("id") Long scooterId,
        @Valid @RequestBody ScooterUpdateRequest request
    ) {
        return ResponseEntity.ok(scooterService.update(scooterId, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScooterResponse> updateStatus(
        @PathVariable("id") Long scooterId,
        @Valid @RequestBody ScooterStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(scooterService.updateStatus(scooterId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long scooterId) {
        scooterService.delete(scooterId);
        return ResponseEntity.noContent().build();
    }
}

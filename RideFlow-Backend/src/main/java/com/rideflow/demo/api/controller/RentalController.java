package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.rental.ActiveRentalResponse;
import com.rideflow.demo.api.dto.rental.ForceEndRentalRequest;
import com.rideflow.demo.api.dto.rental.RentalFilterRequest;
import com.rideflow.demo.api.dto.rental.RentalResponse;
import com.rideflow.demo.api.dto.rental.UnlockScooterRequest;
import com.rideflow.demo.service.RentalService;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/unlock")
    public ResponseEntity<RentalResponse> unlock(@Valid @RequestBody UnlockScooterRequest request) {
        return ResponseEntity.ok(rentalService.unlock(request));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<RentalResponse> startRide(@PathVariable("id") Long rentalId) {
        return ResponseEntity.ok(rentalService.startRide(rentalId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RentalResponse> cancelRide(@PathVariable("id") Long rentalId) {
        return ResponseEntity.ok(rentalService.cancel(rentalId));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<RentalResponse> endRide(@PathVariable("id") Long rentalId) {
        return ResponseEntity.ok(rentalService.endRide(rentalId));
    }

    @GetMapping("/my-rentals")
    public ResponseEntity<PageResponse<RentalResponse>> findMyRentals(@ModelAttribute RentalFilterRequest filter) {
        return ResponseEntity.ok(rentalService.findMyRentals(filter));
    }

    @GetMapping("/active")
    public ResponseEntity<ActiveRentalResponse> findMyActiveRental() {
        Optional<ActiveRentalResponse> activeRental = rentalService.findMyActiveRental();
        return activeRental.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<RentalResponse>> findAll(@ModelAttribute RentalFilterRequest filter) {
        return ResponseEntity.ok(rentalService.findAll(filter));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAll(@ModelAttribute RentalFilterRequest filter) {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rentals.csv\"")
            .body(rentalService.exportAllAsCsv(filter));
    }

    @PostMapping("/{id}/force-end")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentalResponse> forceEnd(
        @PathVariable("id") Long rentalId,
        @Valid @RequestBody ForceEndRentalRequest request
    ) {
        return ResponseEntity.ok(rentalService.forceEnd(rentalId, request));
    }
}

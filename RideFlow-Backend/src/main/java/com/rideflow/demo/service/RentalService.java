package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.rental.ActiveRentalResponse;
import com.rideflow.demo.api.dto.rental.ForceEndRentalRequest;
import com.rideflow.demo.api.dto.rental.RentalFilterRequest;
import com.rideflow.demo.api.dto.rental.RentalResponse;
import com.rideflow.demo.api.dto.rental.UnlockScooterRequest;
import java.util.Optional;

public interface RentalService {
    RentalResponse unlock(UnlockScooterRequest request);
    RentalResponse cancel(Long rentalId);
    RentalResponse startRide(Long rentalId);
    RentalResponse endRide(Long rentalId);
    Optional<ActiveRentalResponse> findMyActiveRental();
    PageResponse<RentalResponse> findMyRentals(RentalFilterRequest filter);
    PageResponse<RentalResponse> findAll(RentalFilterRequest filter);
    byte[] exportAllAsCsv(RentalFilterRequest filter);
    RentalResponse forceEnd(Long rentalId, ForceEndRentalRequest request);
}

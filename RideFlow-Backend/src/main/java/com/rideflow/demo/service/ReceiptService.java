package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.receipt.ReceiptResponse;

public interface ReceiptService {
    ReceiptResponse findByRentalId(Long rentalId);
    String generateHtmlByRentalId(Long rentalId);
    byte[] generatePdfByRentalId(Long rentalId);
}

package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.receipt.ReceiptResponse;
import com.rideflow.demo.service.ReceiptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<ReceiptResponse> findByRentalId(@PathVariable("rentalId") Long rentalId) {
        return ResponseEntity.ok(receiptService.findByRentalId(rentalId));
    }

    @GetMapping("/{rentalId}/html")
    public ResponseEntity<String> renderHtml(@PathVariable("rentalId") Long rentalId) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(receiptService.generateHtmlByRentalId(rentalId));
    }

    @GetMapping("/{rentalId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("rentalId") Long rentalId) {
        String filename = "receipt-" + rentalId + ".pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(receiptService.generatePdfByRentalId(rentalId));
    }
}

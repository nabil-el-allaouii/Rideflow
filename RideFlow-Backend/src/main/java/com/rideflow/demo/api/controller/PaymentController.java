package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.payment.FinalPaymentRequest;
import com.rideflow.demo.api.dto.payment.PaymentFilterRequest;
import com.rideflow.demo.api.dto.payment.PaymentResponse;
import com.rideflow.demo.api.dto.payment.UnlockFeePaymentRequest;
import com.rideflow.demo.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/unlock-fee")
    public ResponseEntity<PaymentResponse> payUnlockFee(@Valid @RequestBody UnlockFeePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.payUnlockFee(request));
    }

    @PostMapping("/final-payment")
    public ResponseEntity<PaymentResponse> payFinalPayment(@Valid @RequestBody FinalPaymentRequest request) {
        return ResponseEntity.ok(paymentService.payFinalAmount(request));
    }

    @GetMapping("/my-payments")
    public ResponseEntity<PageResponse<PaymentResponse>> findMyPayments(@ModelAttribute PaymentFilterRequest filter) {
        return ResponseEntity.ok(paymentService.findMyPayments(filter));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> findAll(@ModelAttribute PaymentFilterRequest filter) {
        return ResponseEntity.ok(paymentService.findAll(filter));
    }
}

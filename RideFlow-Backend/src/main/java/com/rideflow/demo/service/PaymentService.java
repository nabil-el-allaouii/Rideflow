package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.payment.FinalPaymentRequest;
import com.rideflow.demo.api.dto.payment.PaymentFilterRequest;
import com.rideflow.demo.api.dto.payment.PaymentResponse;
import com.rideflow.demo.api.dto.payment.UnlockFeePaymentRequest;

public interface PaymentService {
    PaymentResponse payUnlockFee(UnlockFeePaymentRequest request);
    PaymentResponse payFinalAmount(FinalPaymentRequest request);
    PageResponse<PaymentResponse> findMyPayments(PaymentFilterRequest filter);
    PageResponse<PaymentResponse> findAll(PaymentFilterRequest filter);
    PaymentResponse findById(Long paymentId);
}

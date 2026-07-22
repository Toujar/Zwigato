package com.fooddelivery.service;

import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.PaymentResponse;

/**
 * Contract for payment processing operations.
 */
public interface PaymentService {

    PaymentResponse initiatePayment(PaymentRequest request);

    PaymentResponse confirmPayment(String transactionId);

    PaymentResponse getPaymentByOrderId(Long orderId);
}

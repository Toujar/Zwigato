package com.fooddelivery.service;

import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.PaymentResponse;

/**
 * Contract for payment processing operations.
 * Supports Razorpay payment gateway integration with retry and refund handling.
 */
public interface PaymentService {

    /**
     * Initiates a payment with Razorpay.
     * Creates a Razorpay order and returns client-side details.
     *
     * @param request payment request with orderId and paymentMethod
     * @return payment response with razorpay order details
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /**
     * Confirms payment after Razorpay webhook or verification.
     * Validates the transaction and updates payment status to SUCCESS.
     *
     * @param transactionId Razorpay payment ID
     * @param razorpayOrderId Razorpay order ID
     * @param signature Razorpay signature for verification
     * @return confirmed payment response
     */
    PaymentResponse confirmPayment(String transactionId, String razorpayOrderId, String signature);

    /**
     * Retries a failed payment.
     * Increments retry counter and allows user to re-attempt payment.
     *
     * @param orderId order ID for which payment failed
     * @return payment response with new retry opportunity
     */
    PaymentResponse retryPayment(Long orderId);

    /**
     * Processes refund for a cancelled order.
     * Creates refund request with Razorpay and tracks refund status.
     *
     * @param orderId order ID for which refund is requested
     * @param refundAmount amount to refund (partial or full)
     * @return payment response with refund status
     */
    PaymentResponse processRefund(Long orderId, java.math.BigDecimal refundAmount);

    /**
     * Gets refund status from Razorpay.
     * Polls gateway for async refund completion.
     *
     * @param orderId order ID to check refund status
     * @return payment response with updated refund status
     */
    PaymentResponse checkRefundStatus(Long orderId);

    /**
     * Retrieves payment details for an order.
     *
     * @param orderId order ID
     * @return payment response
     */
    PaymentResponse getPaymentByOrderId(Long orderId);
}

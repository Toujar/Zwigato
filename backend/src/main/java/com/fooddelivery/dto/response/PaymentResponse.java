package com.fooddelivery.dto.response;

import com.fooddelivery.entity.enums.PaymentMethod;
import com.fooddelivery.entity.enums.PaymentStatus;
import com.fooddelivery.entity.enums.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a Payment record.
 */
@Data
@Builder
public class PaymentResponse {
    private Long          id;
    private Long          orderId;
    private BigDecimal    amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String        transactionId;
    private String        razorpayOrderId;
    private String        razorpayPaymentId;
    
    // Refund information
    private RefundStatus  refundStatus;
    private BigDecimal    refundAmount;
    private String        razorpayRefundId;
    
    // Retry information
    private Integer       retryCount;
    private Integer       maxRetries;
    private LocalDateTime lastRetryAt;
    
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

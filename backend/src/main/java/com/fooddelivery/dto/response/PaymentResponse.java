package com.fooddelivery.dto.response;

import com.fooddelivery.entity.enums.PaymentMethod;
import com.fooddelivery.entity.enums.PaymentStatus;
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
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}

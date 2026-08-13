package com.fooddelivery.entity.enums;

/**
 * Payment transaction states.
 * Maps to the MySQL ENUM in the `payments` table.
 */
public enum PaymentStatus {
    /**
     * Payment is awaiting gateway confirmation.
     */
    PENDING,

    /**
     * Payment has been successfully processed.
     */
    SUCCESS,

    /**
     * Payment was rejected or timed out.
     */
    FAILED,

    /**
     * Refund is in progress (async).
     */
    REFUND_IN_PROGRESS,

    /**
     * Payment has been fully or partially refunded.
     */
    REFUNDED
}

package com.fooddelivery.entity.enums;

/**
 * Refund transaction states.
 * Used to track async refund processing from payment gateway.
 */
public enum RefundStatus {
    /**
     * Refund has been initiated but not yet confirmed by gateway.
     */
    PENDING,

    /**
     * Refund has been successfully processed by the gateway.
     */
    COMPLETED,

    /**
     * Refund failed at the gateway (e.g., insufficient balance).
     */
    FAILED
}

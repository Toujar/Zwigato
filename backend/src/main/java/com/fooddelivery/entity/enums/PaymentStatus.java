package com.fooddelivery.entity.enums;

/**
 * Payment transaction states.
 * Maps to the MySQL ENUM in the `payments` table.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

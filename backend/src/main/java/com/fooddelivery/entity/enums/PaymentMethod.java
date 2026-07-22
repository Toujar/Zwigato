package com.fooddelivery.entity.enums;

/**
 * Supported payment methods.
 * Maps to the MySQL ENUM in the `payments` table.
 */
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    WALLET,
    CASH_ON_DELIVERY
}

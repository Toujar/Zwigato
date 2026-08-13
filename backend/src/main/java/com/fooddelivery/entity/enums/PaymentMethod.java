package com.fooddelivery.entity.enums;

/**
 * Supported payment methods.
 * Maps to the MySQL ENUM in the `payments` table.
 */
public enum PaymentMethod {
    ONLINE,           // Online payments (UPI, cards, net banking, wallets)
    COD,              // Cash on Delivery
    CREDIT_CARD,      // Credit card payments
    DEBIT_CARD,       // Debit card payments
    UPI,              // UPI payments
    NET_BANKING,      // Net banking
    WALLET,           // Digital wallets
    CASH_ON_DELIVERY  // Cash on delivery (alternative to COD)
}

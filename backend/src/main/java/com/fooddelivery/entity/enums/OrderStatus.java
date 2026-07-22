package com.fooddelivery.entity.enums;

/**
 * Lifecycle states of an order.
 * Maps to the MySQL ENUM in the `orders` table.
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

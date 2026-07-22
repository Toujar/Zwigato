package com.fooddelivery.entity.enums;

/**
 * Roles a user can hold in the system.
 * Maps to the MySQL ENUM in the `users` table.
 */
public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DELIVERY_AGENT,
    ADMIN
}

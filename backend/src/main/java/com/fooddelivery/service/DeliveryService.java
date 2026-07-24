package com.fooddelivery.service;

import com.fooddelivery.dto.response.OrderResponse;

/**
 * Delivery-agent-specific operations.
 */
public interface DeliveryService {

    /** Returns the full order for an assigned delivery agent to view. */
    OrderResponse getDeliveryStatus(Long orderId);

    /**
     * Assigns a delivery agent to an order and transitions
     * status to OUT_FOR_DELIVERY. ADMIN only.
     */
    OrderResponse assignDelivery(Long orderId, Long deliveryAgentId);

    /**
     * Marks an order as DELIVERED.
     * Only the assigned delivery agent or ADMIN may call this.
     */
    OrderResponse markDelivered(Long orderId);
}

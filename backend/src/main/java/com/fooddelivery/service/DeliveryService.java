package com.fooddelivery.service;

import com.fooddelivery.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Delivery-agent-specific operations.
 */
public interface DeliveryService {

    /** Returns orders that are ready for pickup and have no agent assigned yet. */
    List<OrderResponse> getAvailableOrders();

    /** Returns all orders assigned to the current delivery agent. */
    Page<OrderResponse> getMyDeliveries(Pageable pageable);

    /** Agent self-assigns to an unassigned order. */
    OrderResponse acceptDelivery(Long orderId);

    /** Returns the full order for an assigned delivery agent to view. */
    OrderResponse getDeliveryStatus(Long orderId);

    /** Assigns a delivery agent to an order (ADMIN only). */
    OrderResponse assignDelivery(Long orderId, Long deliveryAgentId);

    /** Marks an order as DELIVERED. */
    OrderResponse markDelivered(Long orderId);
}

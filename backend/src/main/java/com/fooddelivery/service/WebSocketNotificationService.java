package com.fooddelivery.service;

import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.enums.OrderStatus;

/**
 * Service for sending real-time notifications via WebSocket.
 * 
 * Provides pub-sub messaging for order lifecycle events:
 * - Order placement, confirmation, preparation
 * - Pickup and delivery status changes  
 * - Agent assignment and location updates
 * - Restaurant notifications for new orders
 */
public interface WebSocketNotificationService {

    /**
     * Notify customer about order status change.
     */
    void notifyCustomerOrderUpdate(Long customerId, Long orderId, OrderStatus status, String message);

    /**
     * Notify restaurant about new order.
     */
    void notifyRestaurantNewOrder(Long restaurantId, OrderResponse orderDetails);

    /**
     * Notify delivery agent about order assignment.
     */
    void notifyAgentOrderAssignment(Long agentId, OrderResponse orderDetails);

    /**
     * Notify customer about agent location update.
     */
    void notifyCustomerAgentLocation(Long customerId, Long orderId, Double latitude, Double longitude, String eta);

    /**
     * Broadcast order update to all relevant parties.
     */
    void broadcastOrderUpdate(Order order, String message);

    /**
     * Test connection endpoint for debugging.
     */
    void sendTestMessage(Long userId, String message);
}
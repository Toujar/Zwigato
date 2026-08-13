package com.fooddelivery.service;

import com.fooddelivery.dto.request.OrderRequest;
import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for order lifecycle operations.
 */
public interface OrderService {

    OrderResponse placeOrder(OrderRequest request);

    Page<OrderResponse> getOrdersForCurrentUser(Pageable pageable);

    OrderResponse getOrderById(Long id);

    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);

    OrderResponse cancelOrder(Long id);

    /**
     * Reorder — Copies items from a past order into the cart.
     * Allows the customer to place a repeat order with one click.
     *
     * @param orderId the ID of the previous order to reorder from
     * @return confirmation response
     * @throws ResourceNotFoundException if order not found or user doesn't own it
     */
    OrderResponse reorderFromPastOrder(Long orderId);
}

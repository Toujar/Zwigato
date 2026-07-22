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
}

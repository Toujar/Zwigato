
package com.fooddelivery.service;

import com.fooddelivery.dto.response.OrderResponse;

public interface DeliveryService {
    OrderResponse getDeliveryStatus(Long orderId);
    OrderResponse assignDelivery(Long orderId, Long deliveryAgentId);
}

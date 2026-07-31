package com.fooddelivery.dto.response;

import com.fooddelivery.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload representing a full Order.
 */
@Data
@Builder
public class OrderResponse {
    private Long                    id;
    private Long                    userId;
    private String                  userName;
    private Long                    restaurantId;
    private String                  restaurantName;
    private String                  restaurantAddress;   // full address for map routing
    private Long                    deliveryAgentId;
    private String                  deliveryAgentName;
    private String                  deliveryAddress;
    private OrderStatus             status;
    private BigDecimal              subtotal;
    private BigDecimal              deliveryFee;
    private BigDecimal              tax;
    private BigDecimal              totalAmount;
    private String                  specialInstructions;
    private LocalDateTime           placedAt;
    private LocalDateTime           updatedAt;
    private List<OrderItemResponse> items;
    private PaymentResponse         payment;
}

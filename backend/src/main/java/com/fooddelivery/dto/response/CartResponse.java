package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response payload representing the full cart.
 */
@Data
@Builder
public class CartResponse {
    private Long                   id;
    private Long                   userId;
    private Long                   restaurantId;
    private String                 restaurantName;
    private List<CartItemResponse> items;
    private BigDecimal             totalAmount;  // sum of all item subtotals
}

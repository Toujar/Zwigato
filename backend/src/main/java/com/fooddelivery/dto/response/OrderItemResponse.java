package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response payload for a single order item line.
 */
@Data
@Builder
public class OrderItemResponse {
    private Long       id;
    private Long       foodItemId;
    private String     foodItemName;
    private String     imageUrl;
    private Integer    quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    
    // Customization fields (immutable snapshots from order time)
    private String     size;
    private String     spiceLevel;
    private String     addOns;
    private String     specialInstructions;
}

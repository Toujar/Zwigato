package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response payload for a single cart item.
 */
@Data
@Builder
public class CartItemResponse {
    private Long       id;
    private Long       foodItemId;
    private String     foodItemName;
    private String     imageUrl;
    private Integer    quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;   // quantity × unitPrice (computed)
    
    // Customization fields
    private String     size;
    private String     spiceLevel;
    private String     addOns;
    private String     specialInstructions;
}

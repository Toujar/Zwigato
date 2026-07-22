package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a FoodItem.
 */
@Data
@Builder
public class FoodItemResponse {
    private Long          id;
    private Long          restaurantId;
    private String        restaurantName;
    private Long          categoryId;
    private String        categoryName;
    private String        name;
    private String        description;
    private BigDecimal    price;
    private String        imageUrl;
    private Boolean       isVegetarian;
    private Boolean       isAvailable;
    private LocalDateTime createdAt;
}

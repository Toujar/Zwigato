package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a FoodItem.
 *
 * @NoArgsConstructor + @AllArgsConstructor required for Jackson
 * deserialization from Redis cache.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a Restaurant.
 *
 * @NoArgsConstructor + @AllArgsConstructor are required alongside @Builder
 * so that Jackson (GenericJackson2JsonRedisSerializer) can deserialize
 * this class when reading it back from the Redis cache.
 * Without @NoArgsConstructor, Jackson throws:
 *   "no Creators, like default constructor, exist"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private Long          id;
    private Long          ownerId;
    private String        ownerName;
    private String        name;
    private String        description;
    private String        address;
    private String        city;
    private String        phone;
    private String        email;
    private String        imageUrl;
    private BigDecimal    rating;
    private Integer       deliveryTime;
    private BigDecimal    minOrderAmount;
    private Boolean       isOpen;
    private Boolean       isActive;
    private LocalDateTime createdAt;
}

package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a Restaurant.
 */
@Data
@Builder
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

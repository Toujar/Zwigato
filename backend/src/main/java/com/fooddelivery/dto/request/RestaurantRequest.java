package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for creating or updating a restaurant.
 */
@Data
public class RestaurantRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$")
    private String phone;

    @Email
    private String email;

    private String imageUrl;

    @Min(1)
    private Integer deliveryTime;

    @DecimalMin("0.0")
    private BigDecimal minOrderAmount;
}

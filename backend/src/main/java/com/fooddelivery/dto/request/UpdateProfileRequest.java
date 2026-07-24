package com.fooddelivery.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for PUT /api/users/me
 * All fields are optional — only non-null values are applied.
 * Email and password changes are handled by separate dedicated endpoints.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(
        regexp  = "^[+]?[0-9]{10,15}$",
        message = "Phone must be 10–15 digits, optionally prefixed with +"
    )
    private String phone;

    private String address;
}

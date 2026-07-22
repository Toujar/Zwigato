package com.fooddelivery.dto.response;

import com.fooddelivery.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload representing a User.
 * Password is intentionally excluded.
 */
@Data
@Builder
public class UserResponse {
    private Long          id;
    private String        name;
    private String        email;
    private String        phone;
    private String        address;
    private UserRole      role;
    private Boolean       isActive;
    private LocalDateTime createdAt;
}

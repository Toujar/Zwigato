package com.fooddelivery.dto.response;

import com.fooddelivery.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for admin user management operations.
 * Extended user info for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private Boolean isActive;
    private String createdAt;
    private String lastLoginAt;
    
    // Additional stats for admin view
    private Long totalOrders;       // For CUSTOMER
    private Long totalRestaurants;  // For RESTAURANT_OWNER
    private Long totalDeliveries;   // For DELIVERY_AGENT
}

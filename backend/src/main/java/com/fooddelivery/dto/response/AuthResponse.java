package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload for login and register endpoints.
 */
@Data
@Builder
public class AuthResponse {
    private String       accessToken;
    private String       refreshToken;
    @Builder.Default
    private String       tokenType    = "Bearer";
    private Long         expiresIn;    // milliseconds
    private UserResponse user;
}

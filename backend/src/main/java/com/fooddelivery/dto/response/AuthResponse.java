package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for login and register endpoints.
 *
 * @NoArgsConstructor + @AllArgsConstructor required for Jackson
 * deserialization (e.g. from Redis cache or test context).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String       accessToken;
    private String       refreshToken;
    @Builder.Default
    private String       tokenType    = "Bearer";
    private Long         expiresIn;
    private UserResponse user;
}

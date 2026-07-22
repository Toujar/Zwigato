package com.fooddelivery.service;

import com.fooddelivery.dto.request.LoginRequest;
import com.fooddelivery.dto.request.RegisterRequest;
import com.fooddelivery.dto.response.AuthResponse;

/**
 * Contract for authentication operations.
 * Implementation will be added in the next phase.
 */
public interface AuthService {

    /** Register a new customer account and return JWT tokens. */
    AuthResponse register(RegisterRequest request);

    /** Authenticate an existing user and return JWT tokens. */
    AuthResponse login(LoginRequest request);

    /** Issue a new access token from a valid refresh token. */
    AuthResponse refreshToken(String refreshToken);
}

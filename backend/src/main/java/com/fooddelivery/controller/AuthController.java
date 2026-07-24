package com.fooddelivery.controller;

import com.fooddelivery.dto.request.LoginRequest;
import com.fooddelivery.dto.request.RegisterRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.AuthResponse;
import com.fooddelivery.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 *  Controller : AuthController
 *  Base path  : /api/auth
 *  Access     : PUBLIC — no authentication required
 * ============================================================
 *
 *  Handles user registration, login, and JWT token refresh.
 *  These endpoints are explicitly whitelisted in SecurityConfig
 *  so they do not require a Bearer token.
 *
 *  All endpoints return ApiResponse<AuthResponse> which includes
 *  the access token, refresh token, expiry, and user profile.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "Register, login, and refresh JWT tokens")
public class AuthController {

    private final AuthService authService;

    // ----------------------------------------------------------
    // POST /api/auth/register
    // ----------------------------------------------------------
    @PostMapping("/register")
    @Operation(
        summary     = "Register a new customer account",
        description = "Creates a new user with the CUSTOMER role and returns JWT tokens immediately. "
                    + "Fails with 400 if email or phone is already registered."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email or phone already registered / validation failed")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "Account created successfully"));
    }

    // ----------------------------------------------------------
    // POST /api/auth/login
    // ----------------------------------------------------------
    @PostMapping("/login")
    @Operation(
        summary     = "Login with email and password",
        description = "Authenticates the user and returns a new access token + refresh token. "
                    + "Returns 401 if credentials are wrong or account is deactivated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    // ----------------------------------------------------------
    // POST /api/auth/refresh-token
    // ----------------------------------------------------------
    @PostMapping("/refresh-token")
    @Operation(
        summary     = "Refresh an expired access token",
        description = "Provide the refresh token in the 'Refresh-Token' request header. "
                    + "Returns a new access token + refresh token pair. "
                    + "Returns 400 if the refresh token is invalid or expired."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken) {

        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
    }
}

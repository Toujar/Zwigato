package com.fooddelivery.controller;

import com.fooddelivery.dto.request.UpdateProfileRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.UserResponse;
import com.fooddelivery.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 *  Controller : UserController
 *  Base path  : /api/users
 *  Access     : All endpoints require a valid JWT.
 * ============================================================
 *
 *  Role matrix:
 *  ┌─────────────────────────┬──────────────────────────────┐
 *  │ Endpoint                │ Allowed roles                │
 *  ├─────────────────────────┼──────────────────────────────┤
 *  │ GET  /me                │ CUSTOMER, RESTAURANT_OWNER,  │
 *  │                         │ DELIVERY_AGENT, ADMIN        │
 *  │                         │ (any authenticated user)     │
 *  ├─────────────────────────┼──────────────────────────────┤
 *  │ PUT  /me                │ CUSTOMER, RESTAURANT_OWNER,  │
 *  │                         │ DELIVERY_AGENT, ADMIN        │
 *  │                         │ (any authenticated user)     │
 *  ├─────────────────────────┼──────────────────────────────┤
 *  │ GET  /{id}              │ ADMIN only                   │
 *  ├─────────────────────────┼──────────────────────────────┤
 *  │ DELETE /{id}            │ ADMIN only                   │
 *  └─────────────────────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "2. Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")   // applies JWT lock to entire controller in Swagger
public class UserController {

    private final UserService userService;

    // ----------------------------------------------------------------
    // GET /api/users/me
    // Any authenticated user — reads their own profile from the JWT
    // ----------------------------------------------------------------
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Get my profile",
        description = "Returns the full profile of the currently authenticated user. "
                    + "The user is identified from the JWT — no ID required in the URL."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getCurrentUser(), "Profile retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // PUT /api/users/me
    // Any authenticated user — updates their own profile
    // ----------------------------------------------------------------
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Update my profile",
        description = "Partially updates name, phone, and/or address. "
                    + "Only the fields you send are changed. "
                    + "Email and password changes require dedicated endpoints. "
                    + "Returns 400 if the new phone number is already taken."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Phone conflict / validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(request), "Profile updated successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/users/{id}
    // ADMIN only — look up any user by their ID
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Get any user by ID (ADMIN only)",
        description = "Returns the full profile of any registered user. "
                    + "Returns 404 if the user does not exist."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getUserById(id), "User retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/users/{id}
    // ADMIN only — soft-delete (is_active = false)
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Deactivate a user account (ADMIN only)",
        description = "Sets is_active = false. The user can no longer log in. "
                    + "The row is preserved for historical order integrity. "
                    + "Returns 404 if not found. Returns 400 if already deactivated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User account deactivated successfully"));
    }
}

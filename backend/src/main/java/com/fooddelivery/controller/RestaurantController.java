package com.fooddelivery.controller;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 *  Controller : RestaurantController
 *  Base path  : /api/restaurants
 * ============================================================
 *
 *  Role matrix:
 *  ┌───────────────────────────┬──────────────────────────────────────┐
 *  │ Endpoint                  │ Allowed roles                        │
 *  ├───────────────────────────┼──────────────────────────────────────┤
 *  │ GET  /                    │ PUBLIC (no token)                    │
 *  │ GET  /{id}                │ PUBLIC (no token)                    │
 *  ├───────────────────────────┼──────────────────────────────────────┤
 *  │ POST /                    │ RESTAURANT_OWNER, ADMIN              │
 *  │ PUT  /{id}                │ RESTAURANT_OWNER (own), ADMIN        │
 *  │ DELETE /{id}              │ RESTAURANT_OWNER (own), ADMIN        │
 *  │ PATCH /{id}/toggle-open   │ RESTAURANT_OWNER (own), ADMIN        │
 *  └───────────────────────────┴──────────────────────────────────────┘
 *
 *  Ownership check (is this YOUR restaurant?) is enforced in
 *  RestaurantServiceImpl so it has access to the full entity.
 *  @PreAuthorize here handles only role-level access.
 */
@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "4. Restaurants", description = "Restaurant discovery and management")
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ----------------------------------------------------------------
    // GET /api/restaurants  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(
        summary     = "Browse restaurants",
        description = "Returns a paginated list of active + open restaurants. "
                    + "Pass 'keyword' to search by name or city. "
                    + "No authentication required."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Restaurants returned")
    })
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @Parameter(description = "Keyword to search by name or city")
            @RequestParam(required = false) String keyword) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sortBy));
        Page<RestaurantResponse> result = (keyword != null && !keyword.isBlank())
                ? restaurantService.searchRestaurants(keyword, pageable)
                : restaurantService.getAllRestaurants(pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Restaurants retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/restaurants/{id}  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(
        summary     = "Get a restaurant by ID",
        description = "Returns full details of a single restaurant. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Restaurant found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                restaurantService.getRestaurantById(id),
                "Restaurant retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/restaurants  — RESTAURANT_OWNER or ADMIN
    // ----------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Register a new restaurant (RESTAURANT_OWNER / ADMIN)",
        description = "Creates a restaurant owned by the currently authenticated user. "
                    + "Returns 400 if the same name already exists in that city."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Restaurant created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duplicate / validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "RESTAURANT_OWNER or ADMIN role required")
    })
    public ResponseEntity<ApiResponse<RestaurantResponse>> create(
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        restaurantService.createRestaurant(request),
                        "Restaurant created successfully"));
    }

    // ----------------------------------------------------------------
    // PUT /api/restaurants/{id}  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Update a restaurant (owner / ADMIN)",
        description = "Updates restaurant fields. RESTAURANT_OWNER can only update their own. "
                    + "Returns 403 if not the owner. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Restaurant updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner or wrong role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiResponse<RestaurantResponse>> update(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                restaurantService.updateRestaurant(id, request),
                "Restaurant updated successfully"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/restaurants/{id}  — RESTAURANT_OWNER (own) or ADMIN
    // Soft-delete: is_active = false, is_open = false
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Deactivate a restaurant (owner / ADMIN)",
        description = "Sets is_active = false and is_open = false. "
                    + "Restaurant is hidden from browse but data is preserved. "
                    + "Returns 403 if not the owner."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Restaurant deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Restaurant deactivated successfully"));
    }

    // ----------------------------------------------------------------
    // PATCH /api/restaurants/{id}/toggle-open  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @PatchMapping("/{id}/toggle-open")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Toggle restaurant open/closed (owner / ADMIN)",
        description = "Flips the isOpen flag. Closed restaurants are hidden from browse. "
                    + "Owners use this to temporarily stop taking orders. "
                    + "Returns 403 if not the owner. Returns 400 if restaurant is deactivated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status toggled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Deactivated restaurant cannot be opened"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiResponse<RestaurantResponse>> toggleOpen(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long id) {
        RestaurantResponse r = restaurantService.toggleOpen(id);
        String msg = Boolean.TRUE.equals(r.getIsOpen())
                ? "Restaurant is now open"
                : "Restaurant is now closed";
        return ResponseEntity.ok(ApiResponse.success(r, msg));
    }

    // ----------------------------------------------------------------
    // GET /api/restaurants/nearby  — PUBLIC (Location-based search)
    // ----------------------------------------------------------------
    @GetMapping("/nearby")
    @Operation(
        summary     = "Get nearby restaurants by location",
        description = "Returns restaurants near the user's coordinates, sorted by distance. "
                    + "Optional city filter and latitude/longitude."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nearby restaurants returned")
    })
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getNearby(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "User's latitude")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "User's longitude")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "City to filter by")
            @RequestParam(required = false) String city) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<RestaurantResponse> result = restaurantService.getNearbyRestaurants(
                latitude, longitude, city, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Nearby restaurants retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/restaurants/search/all  — PUBLIC (Platform-wide search)
    // ----------------------------------------------------------------
    @GetMapping("/search/all")
    @Operation(
        summary     = "Search restaurants across all cities",
        description = "Returns restaurants matching the keyword from all cities. "
                    + "Can optionally filter by location proximity."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> searchAll(
            @Parameter(description = "Search keyword")
            @RequestParam String keyword,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "User's latitude for distance sorting")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "User's longitude for distance sorting")
            @RequestParam(required = false) Double longitude) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<RestaurantResponse> result = restaurantService.searchAllRestaurants(
                keyword, latitude, longitude, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Search results retrieved successfully"));
    }
}

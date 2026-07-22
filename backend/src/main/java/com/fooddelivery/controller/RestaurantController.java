package com.fooddelivery.controller;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Manages restaurant listings and metadata.
 * Base path: /api/restaurants
 */
@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Browse and manage restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    @Operation(summary = "Get all active open restaurants (paginated)")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE)   int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY)     String sortBy,
            @RequestParam(required = false) String keyword) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a restaurant by ID")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PostMapping
    @Operation(summary = "Create a new restaurant (RESTAURANT_OWNER / ADMIN)")
    public ResponseEntity<ApiResponse<RestaurantResponse>> create(
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a restaurant (owner / ADMIN)")
    public ResponseEntity<ApiResponse<RestaurantResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a restaurant (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PatchMapping("/{id}/toggle-open")
    @Operation(summary = "Toggle restaurant open/closed status")
    public ResponseEntity<ApiResponse<RestaurantResponse>> toggleOpen(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }
}

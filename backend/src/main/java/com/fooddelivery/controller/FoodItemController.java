package com.fooddelivery.controller;

import com.fooddelivery.dto.request.FoodItemRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.FoodItemResponse;
import com.fooddelivery.service.FoodItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages food items (menus) per restaurant.
 * Base path: /api/food-items
 */
@RestController
@RequestMapping("/food-items")
@RequiredArgsConstructor
@Tag(name = "Food Items", description = "Manage restaurant menu items")
public class FoodItemController {

    private final FoodItemService foodItemService;

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get all food items for a restaurant")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getByRestaurant(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a food item by ID")
    public ResponseEntity<ApiResponse<FoodItemResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PostMapping
    @Operation(summary = "Add a food item to a restaurant (RESTAURANT_OWNER / ADMIN)")
    public ResponseEntity<ApiResponse<FoodItemResponse>> create(
            @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a food item")
    public ResponseEntity<ApiResponse<FoodItemResponse>> update(
            @PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a food item")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PatchMapping("/{id}/toggle-availability")
    @Operation(summary = "Toggle a food item's availability")
    public ResponseEntity<ApiResponse<FoodItemResponse>> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }
}

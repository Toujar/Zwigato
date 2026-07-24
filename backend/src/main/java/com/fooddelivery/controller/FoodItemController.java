package com.fooddelivery.controller;

import com.fooddelivery.dto.request.FoodItemRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.FoodItemResponse;
import com.fooddelivery.service.FoodItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 *  Controller : FoodItemController
 *  Base path  : /api/food-items
 * ============================================================
 *
 *  Role matrix:
 *  ┌──────────────────────────────────┬────────────────────────────────────┐
 *  │ Endpoint                         │ Allowed roles                      │
 *  ├──────────────────────────────────┼────────────────────────────────────┤
 *  │ GET  /restaurant/{restaurantId}  │ PUBLIC (no token)                  │
 *  │ GET  /{id}                       │ PUBLIC (no token)                  │
 *  ├──────────────────────────────────┼────────────────────────────────────┤
 *  │ POST /                           │ RESTAURANT_OWNER (own), ADMIN      │
 *  │ PUT  /{id}                       │ RESTAURANT_OWNER (own), ADMIN      │
 *  │ DELETE /{id}                     │ RESTAURANT_OWNER (own), ADMIN      │
 *  │ PATCH /{id}/toggle-availability  │ RESTAURANT_OWNER (own), ADMIN      │
 *  └──────────────────────────────────┴────────────────────────────────────┘
 *
 *  Ownership check (is this item from YOUR restaurant?) is validated
 *  inside FoodItemServiceImpl.
 */
@RestController
@RequestMapping("/food-items")
@RequiredArgsConstructor
@Tag(name = "5. Food Items", description = "Restaurant menu item management")
public class FoodItemController {

    private final FoodItemService foodItemService;

    // ----------------------------------------------------------------
    // GET /api/food-items/restaurant/{restaurantId}  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping("/restaurant/{restaurantId}")
    @Operation(
        summary     = "Get menu items for a restaurant",
        description = "Returns all available food items for the given restaurant grouped by category. "
                    + "No authentication required."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menu returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getByRestaurant(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(
                foodItemService.getMenuByRestaurant(restaurantId),
                "Menu retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/food-items/{id}  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(
        summary     = "Get a food item by ID",
        description = "Returns full food item detail. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food item found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<FoodItemResponse>> getById(
            @Parameter(description = "Food item ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                foodItemService.getFoodItemById(id),
                "Food item retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/food-items  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Add a food item to a restaurant menu (owner / ADMIN)",
        description = "Creates a new food item. The caller must be the restaurant owner or ADMIN. "
                    + "Returns 403 if the caller does not own the target restaurant. "
                    + "Returns 400 if the category is deactivated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Food item created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation / inactive category"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the restaurant owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant or category not found")
    })
    public ResponseEntity<ApiResponse<FoodItemResponse>> create(
            @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        foodItemService.createFoodItem(request),
                        "Food item created successfully"));
    }

    // ----------------------------------------------------------------
    // PUT /api/food-items/{id}  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Update a food item (owner / ADMIN)",
        description = "Partially updates name, description, price, category, image, veg flag. "
                    + "Returns 403 if not the owner. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food item updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<FoodItemResponse>> update(
            @Parameter(description = "Food item ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                foodItemService.updateFoodItem(id, request),
                "Food item updated successfully"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/food-items/{id}  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Delete a food item (owner / ADMIN)",
        description = "Permanently removes the food item and scrubs it from all active carts. "
                    + "Fails if the item is referenced in order history (use toggle-availability instead). "
                    + "Returns 403 if not the owner."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food item deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Food item ID", required = true)
            @PathVariable Long id) {
        foodItemService.deleteFoodItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Food item deleted successfully"));
    }

    // ----------------------------------------------------------------
    // PATCH /api/food-items/{id}/toggle-availability  — RESTAURANT_OWNER (own) or ADMIN
    // ----------------------------------------------------------------
    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Toggle food item availability (owner / ADMIN)",
        description = "Flips isAvailable. Unavailable items are hidden from the customer menu. "
                    + "Preferred over deletion for seasonal / out-of-stock items. "
                    + "Returns 403 if not the owner."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability toggled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<FoodItemResponse>> toggleAvailability(
            @Parameter(description = "Food item ID", required = true)
            @PathVariable Long id) {
        FoodItemResponse item = foodItemService.toggleAvailability(id);
        String msg = Boolean.TRUE.equals(item.getIsAvailable())
                ? "Food item is now available"
                : "Food item is now unavailable";
        return ResponseEntity.ok(ApiResponse.success(item, msg));
    }
}

package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CartItemRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CartResponse;
import com.fooddelivery.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 *  Controller : CartController
 *  Base path  : /api/cart
 * ============================================================
 *
 *  Role matrix:
 *  ┌─────────────────────────┬──────────────────────────────────────┐
 *  │ Endpoint                │ Allowed roles                        │
 *  ├─────────────────────────┼──────────────────────────────────────┤
 *  │ ALL                     │ CUSTOMER only                        │
 *  │                         │ (restaurant owners and delivery      │
 *  │                         │ agents do not have shopping carts)   │
 *  └─────────────────────────┴──────────────────────────────────────┘
 *
 *  The class-level @PreAuthorize("hasRole('CUSTOMER')") enforces this
 *  for every endpoint at once, avoiding repetition on each method.
 *
 *  Ownership check (is this YOUR cart?) is enforced inside
 *  CartServiceImpl using the JWT identity — not needed here.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('CUSTOMER')")          // ← class-level: all endpoints require CUSTOMER role
@Tag(name = "6. Cart", description = "Shopping cart — CUSTOMER only")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    // ----------------------------------------------------------------
    // GET /api/cart  — CUSTOMER only
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(
        summary     = "Get my cart",
        description = "Returns the full cart with all items, quantities, unit prices, "
                    + "subtotals, locked restaurant, and grand total. "
                    + "Creates an empty cart on first call."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required")
    })
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.getCartForCurrentUser(), "Cart retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/cart/items  — CUSTOMER only
    // ----------------------------------------------------------------
    @PostMapping("/items")
    @Operation(
        summary     = "Add an item to the cart",
        description = "Adds the food item with the given quantity. "
                    + "If the same item exists, quantity is incremented instead. "
                    + "Cart is locked to one restaurant — adding from a different restaurant "
                    + "returns 400 (clear cart first)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item added"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Item unavailable / different restaurant / validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Food item not found")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(cartService.addItem(request), "Item added to cart"));
    }

    // ----------------------------------------------------------------
    // PUT /api/cart/items/{cartItemId}  — CUSTOMER only
    // ----------------------------------------------------------------
    @PutMapping("/items/{cartItemId}")
    @Operation(
        summary     = "Update cart item quantity",
        description = "Sets quantity to the provided value (must be ≥ 1). "
                    + "To remove an item use DELETE instead. "
                    + "Returns 403 if the item does not belong to your cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Quantity updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "quantity < 1"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "CUSTOMER role required or item does not belong to your cart"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable Long cartItemId,
            @Parameter(description = "New quantity (≥ 1)", required = true)
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateItemQuantity(cartItemId, quantity),
                "Cart item quantity updated"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/cart/items/{cartItemId}  — CUSTOMER only
    // ----------------------------------------------------------------
    @DeleteMapping("/items/{cartItemId}")
    @Operation(
        summary     = "Remove an item from the cart",
        description = "Removes the item row. If this was the last item, the restaurant lock is cleared. "
                    + "Returns 403 if the item does not belong to your cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed, updated cart returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "CUSTOMER role required or item does not belong to your cart"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.removeItem(cartItemId), "Item removed from cart"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/cart  — CUSTOMER only
    // ----------------------------------------------------------------
    @DeleteMapping
    @Operation(
        summary     = "Clear the entire cart",
        description = "Removes all items and releases the restaurant lock. "
                    + "Use this before adding items from a different restaurant."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart cleared"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }
}

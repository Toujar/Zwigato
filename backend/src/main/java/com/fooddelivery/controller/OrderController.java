package com.fooddelivery.controller;

import com.fooddelivery.dto.request.OrderRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.service.OrderService;
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
 *  Controller : OrderController
 *  Base path  : /api/orders
 * ============================================================
 *
 *  Role matrix:
 *  ┌──────────────────────────┬──────────────────────────────────────────────┐
 *  │ Endpoint                 │ Allowed roles                                │
 *  ├──────────────────────────┼──────────────────────────────────────────────┤
 *  │ POST /                   │ CUSTOMER only (places the order)             │
 *  ├──────────────────────────┼──────────────────────────────────────────────┤
 *  │ GET  /                   │ CUSTOMER — own order history                 │
 *  │                          │ RESTAURANT_OWNER, DELIVERY_AGENT, ADMIN      │
 *  │                          │ (everyone sees their own view in the service)│
 *  ├──────────────────────────┼──────────────────────────────────────────────┤
 *  │ GET  /{id}               │ Any authenticated (ownership check in svc)   │
 *  ├──────────────────────────┼──────────────────────────────────────────────┤
 *  │ PATCH /{id}/status       │ RESTAURANT_OWNER, DELIVERY_AGENT, ADMIN      │
 *  ├──────────────────────────┼──────────────────────────────────────────────┤
 *  │ PATCH /{id}/cancel       │ CUSTOMER only (cancels their own order)      │
 *  └──────────────────────────┴──────────────────────────────────────────────┘
 *
 *  Fine-grained ownership (e.g., "only the owner of THIS restaurant
 *  can change status") is validated in OrderServiceImpl.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "7. Orders", description = "Order placement and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    // ----------------------------------------------------------------
    // POST /api/orders  — CUSTOMER only
    // ----------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary     = "Place a new order (CUSTOMER only)",
        description = "Creates an order from the provided items. "
                    + "Validates restaurant is open, all items are available, "
                    + "and the minimum order amount is met. "
                    + "Cart is auto-cleared on success."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Validation / restaurant closed / item unavailable / below minimum"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Restaurant or food item not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.placeOrder(request), "Order placed successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/orders  — CUSTOMER, RESTAURANT_OWNER, DELIVERY_AGENT, ADMIN
    // Service returns only the caller's own orders (filtered by role)
    // ----------------------------------------------------------------
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Get my order history (paginated)",
        description = "Returns orders placed by the current user, sorted newest first. "
                    + "Paginated — default page size is 10."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("placedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrdersForCurrentUser(pageable),
                "Orders retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/orders/{id}  — Any authenticated user (ownership check in service)
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Get order details by ID",
        description = "Returns complete order including items, amounts, payment, and delivery agent. "
                    + "Access is allowed for: the order owner, the restaurant owner, "
                    + "the assigned delivery agent, and ADMIN. "
                    + "Returns 403 if none of the above."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to view this order"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(id), "Order retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // PATCH /api/orders/{id}/status  — RESTAURANT_OWNER, DELIVERY_AGENT, ADMIN
    // ----------------------------------------------------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary     = "Update order status (RESTAURANT_OWNER / DELIVERY_AGENT / ADMIN)",
        description = "Valid lifecycle transitions:\n"
                    + "PLACED → CONFIRMED (restaurant confirms)\n"
                    + "CONFIRMED → PREPARING (kitchen starts)\n"
                    + "PREPARING → OUT_FOR_DELIVERY (agent assigned)\n"
                    + "OUT_FOR_DELIVERY → DELIVERED (agent confirms drop-off)\n\n"
                    + "Only the restaurant owner of THIS order can advance to CONFIRMED/PREPARING. "
                    + "Only the assigned delivery agent or ADMIN can mark DELIVERED."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition or not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "RESTAURANT_OWNER / DELIVERY_AGENT / ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New order status", required = true, example = "CONFIRMED")
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrderStatus(id, status),
                "Order status updated to " + status.name()));
    }

    // ----------------------------------------------------------------
    // PATCH /api/orders/{id}/cancel  — CUSTOMER only
    // ----------------------------------------------------------------
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary     = "Cancel an order (CUSTOMER only)",
        description = "Cancels the order if status is PLACED or CONFIRMED. "
                    + "Cancellation is rejected once the kitchen starts PREPARING. "
                    + "Returns 400 if the order is in an uncancellable state. "
                    + "Returns 403 if the order does not belong to the current user."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot cancel / not the owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(id), "Order cancelled successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/orders/{id}/reorder  — CUSTOMER only
    // ----------------------------------------------------------------
    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary     = "Reorder from past order (CUSTOMER only)",
        description = "Copies all items from a past order into the cart. "
                    + "Preserves customizations (size, spice level, add-ons, special instructions). "
                    + "If the cart has items from a different restaurant, "
                    + "the cart will be cleared first. "
                    + "Returns the updated cart response. "
                    + "Returns 403 if the order doesn't belong to the current user."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Items added to cart"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> reorder(
            @Parameter(description = "Order ID to reorder from", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.reorderFromPastOrder(id),
                "Order items added to cart. Ready to proceed to checkout."));
    }
}

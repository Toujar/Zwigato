package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 *  Controller : DeliveryController
 *  Base path  : /api/delivery
 * ============================================================
 *
 *  Role matrix:
 *  ┌──────────────────────────────────┬──────────────────────────────────────┐
 *  │ Endpoint                         │ Allowed roles                        │
 *  ├──────────────────────────────────┼──────────────────────────────────────┤
 *  │ GET  /{orderId}/status           │ DELIVERY_AGENT (assigned), ADMIN     │
 *  │ PATCH /{orderId}/delivered       │ DELIVERY_AGENT (assigned), ADMIN     │
 *  ├──────────────────────────────────┼──────────────────────────────────────┤
 *  │ POST  /{orderId}/assign          │ ADMIN only                           │
 *  └──────────────────────────────────┴──────────────────────────────────────┘
 *
 *  Class-level @PreAuthorize restricts the whole controller to
 *  DELIVERY_AGENT + ADMIN.  The /assign endpoint tightens that to
 *  ADMIN-only with a method-level annotation.
 *
 *  Ownership check ("is this your assigned order?") is enforced
 *  inside DeliveryServiceImpl — not here — because the service has
 *  access to the full Order entity to compare agent IDs.
 */
@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")   // class-level default
@Tag(name = "9. Delivery", description = "Delivery agent operations and agent assignment")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ----------------------------------------------------------------
    // GET /api/delivery/available  — orders ready for agent pickup
    // ----------------------------------------------------------------
    @GetMapping("/available")
    @Operation(summary = "Get orders available for pickup (DELIVERY_AGENT)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.getAvailableOrders(), "Available orders"));
    }

    // ----------------------------------------------------------------
    // GET /api/delivery/my-deliveries  — agent's own assigned orders
    // ----------------------------------------------------------------
    @GetMapping("/my-deliveries")
    @Operation(summary = "Get my assigned deliveries (DELIVERY_AGENT)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyDeliveries(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("placedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.getMyDeliveries(pageable), "My deliveries"));
    }

    // ----------------------------------------------------------------
    // PATCH /api/delivery/{orderId}/accept  — agent self-accepts
    // ----------------------------------------------------------------
    @PatchMapping("/{orderId}/accept")
    @Operation(summary = "Accept an available delivery order (DELIVERY_AGENT)")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptDelivery(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.acceptDelivery(orderId), "Delivery accepted"));
    }

    // ----------------------------------------------------------------
    // GET /api/delivery/{orderId}/status
    // DELIVERY_AGENT (must be the assigned agent) or ADMIN
    // ----------------------------------------------------------------
    @GetMapping("/{orderId}/status")
    @Operation(
        summary     = "Get delivery status for an order",
        description = "Returns the full order: status, delivery address, items, customer info. "
                    + "DELIVERY_AGENT can only view orders they are assigned to. "
                    + "ADMIN can view any order. "
                    + "Returns 403 if the agent is not assigned to this order."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Not the assigned agent / wrong role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getDeliveryStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {

        OrderResponse order = deliveryService.getDeliveryStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(
                order, "Delivery status: " + order.getStatus().name()));
    }

    // ----------------------------------------------------------------
    // PATCH /api/delivery/{orderId}/delivered
    // DELIVERY_AGENT (must be the assigned agent) or ADMIN
    // ----------------------------------------------------------------
    @PatchMapping("/{orderId}/delivered")
    @Operation(
        summary     = "Mark an order as delivered",
        description = "Transitions order status: OUT_FOR_DELIVERY → DELIVERED. "
                    + "DELIVERY_AGENT can only mark orders they are personally assigned to. "
                    + "ADMIN can mark any order. "
                    + "Returns 400 if the order is not in OUT_FOR_DELIVERY status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order marked as DELIVERED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Invalid status transition / not the assigned agent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the assigned agent / wrong role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {

        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.markDelivered(orderId), "Order marked as delivered"));
    }

    // ----------------------------------------------------------------
    // POST /api/delivery/{orderId}/assign
    // ADMIN only (method-level annotation overrides class-level)
    // ----------------------------------------------------------------
    @PostMapping("/{orderId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Assign a delivery agent to an order (ADMIN only)",
        description = "Assigns a DELIVERY_AGENT user to the order and transitions "
                    + "status to OUT_FOR_DELIVERY. "
                    + "Order must be in CONFIRMED or PREPARING status. "
                    + "Returns 400 if an agent is already assigned, the status is wrong, "
                    + "or the target user is not a DELIVERY_AGENT."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Agent assigned, status → OUT_FOR_DELIVERY"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Already assigned / wrong status / not a delivery agent / agent deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Order not found / agent user not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> assignAgent(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "User ID of the delivery agent", required = true)
            @RequestParam Long agentId) {

        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.assignDelivery(orderId, agentId),
                "Delivery agent assigned successfully"));
    }
}

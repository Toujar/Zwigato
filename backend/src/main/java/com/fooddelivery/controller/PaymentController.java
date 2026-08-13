package com.fooddelivery.controller;

import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.service.PaymentService;
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

/**
 * ============================================================
 *  Controller : PaymentController
 *  Base path  : /api/payments
 * ============================================================
 *
 *  Role matrix:
 *  ┌────────────────────────────────────┬─────────────────────────────────────────┐
 *  │ Endpoint                           │ Allowed roles                           │
 *  ├────────────────────────────────────┼─────────────────────────────────────────┤
 *  │ POST /initiate                     │ CUSTOMER only                           │
 *  │                                    │ (only the customer pays for their order) │
 *  ├────────────────────────────────────┼─────────────────────────────────────────┤
 *  │ POST /confirm/{transactionId}      │ ADMIN only                              │
 *  │                                    │ In production this would be an internal  │
 *  │                                    │ endpoint called by the gateway webhook.  │
 *  │                                    │ ADMIN role prevents customers from       │
 *  │                                    │ self-confirming their own payments.      │
 *  ├────────────────────────────────────┼─────────────────────────────────────────┤
 *  │ GET  /order/{orderId}              │ CUSTOMER (own order), ADMIN             │
 *  │                                    │ (ownership checked in service)           │
 *  └────────────────────────────────────┴─────────────────────────────────────────┘
 *
 *  Note: in a real integration the gateway webhook would call
 *  /confirm via a machine-to-machine request authenticated with an
 *  API key or HMAC signature — not a user JWT.
 *  For this project, ADMIN role is used as the closest equivalent.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "8. Payments", description = "Order payment initiation and confirmation")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // ----------------------------------------------------------------
    // POST /api/payments/initiate  — CUSTOMER only
    // ----------------------------------------------------------------
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary     = "Initiate payment for an order (CUSTOMER only)",
        description = "Creates a PENDING payment row for the order. "
                    + "Amount is taken from the order total. "
                    + "Only the customer who placed the order can initiate payment. "
                    + "Returns 400 if a SUCCESS payment already exists. "
                    + "Returns PENDING payment if one already exists (idempotent)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment initiated (PENDING)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Already paid / cancelled order / not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        paymentService.initiatePayment(request),
                        "Payment initiated successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/payments/confirm/{transactionId}  — ADMIN only
    // (in production: called by payment gateway webhook with API key auth)
    // ----------------------------------------------------------------
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Confirm a payment via Razorpay callback (ADMIN / gateway webhook)",
        description = "Verifies Razorpay signature and marks payment as SUCCESS. "
                    + "Auto-advances order to CONFIRMED. "
                    + "Idempotent — duplicate callbacks are safely ignored. "
                    + "In production this is called by Razorpay webhook with signature verification."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment confirmed (SUCCESS)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid signature or already processed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order/Payment not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(
            @Parameter(description = "Razorpay payment ID", required = true)
            @RequestParam String razorpayPaymentId,
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Razorpay signature for verification", required = true)
            @RequestParam String signature) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.confirmPayment(razorpayPaymentId, razorpayOrderId, signature),
                "Payment confirmed successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/payments/retry/{orderId}  — CUSTOMER only
    // ----------------------------------------------------------------
    @PostMapping("/retry/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary     = "Retry a failed payment (CUSTOMER only)",
        description = "Allows customer to retry a FAILED payment. "
                    + "Increments retry counter (max 3 attempts). "
                    + "Enforces 5-minute delay between attempts to prevent abuse. "
                    + "Returns new Razorpay order for checkout. "
                    + "Returns 400 if max retries exceeded or payment not FAILED."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New payment initiated for retry"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Not a FAILED payment / max retries exceeded / retry delay not met"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required or not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or Payment not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> retry(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.retryPayment(orderId),
                "Payment retry initiated. Please complete the new payment."));
    }

    // ----------------------------------------------------------------
    // POST /api/payments/refund/{orderId}  — CUSTOMER or ADMIN
    // ----------------------------------------------------------------
    @PostMapping("/refund/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary     = "Process refund for cancelled order (CUSTOMER / ADMIN)",
        description = "Initiates async refund via Razorpay for a paid order. "
                    + "CUSTOMER can refund their own order. ADMIN can refund any order. "
                    + "Supports partial refunds (e.g., deduction for cancellation fee). "
                    + "Returns payment with refund status PENDING. "
                    + "Returns 400 if payment not SUCCESS or refund amount invalid."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund initiated (PENDING)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Not a SUCCESS payment / invalid refund amount / already refunded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "CUSTOMER / ADMIN role required or not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or Payment not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Refund amount (optional, defaults to full amount)", required = false)
            @RequestParam(required = false) java.math.BigDecimal refundAmount) {

        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        java.math.BigDecimal amount = refundAmount != null ? refundAmount : payment.getAmount();

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.processRefund(orderId, amount),
                "Refund initiated. Status will be updated when gateway confirms."));
    }

    // ----------------------------------------------------------------
    // GET /api/payments/refund/status/{orderId}  — CUSTOMER or ADMIN
    // ----------------------------------------------------------------
    @GetMapping("/refund/status/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary     = "Check refund status (CUSTOMER / ADMIN)",
        description = "Polls Razorpay for async refund completion status. "
                    + "Returns updated payment with refund status: PENDING/COMPLETED/FAILED. "
                    + "Returns 400 if no refund is in progress for this order."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund status returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No refund in progress"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "CUSTOMER / ADMIN role required or not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or Payment not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> checkRefundStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.checkRefundStatus(orderId),
                "Refund status retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/payments/order/{orderId}  — CUSTOMER (own) or ADMIN
    // ----------------------------------------------------------------
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary     = "Get payment details for an order (CUSTOMER / ADMIN)",
        description = "Returns the payment record for the given order. "
                    + "CUSTOMER can only view payment for their own orders. "
                    + "Returns 403 if it is not the order owner (enforced in service). "
                    + "Returns 404 if no payment has been initiated yet."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "CUSTOMER / ADMIN role required, or not the order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found for this order")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentByOrderId(orderId),
                "Payment retrieved successfully"));
    }
}

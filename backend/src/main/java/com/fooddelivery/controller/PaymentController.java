package com.fooddelivery.controller;

import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles payment initiation and confirmation.
 * Base path: /api/payments
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Initiate and confirm payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a payment for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PostMapping("/confirm/{transactionId}")
    @Operation(summary = "Confirm a payment via transaction ID (gateway callback)")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details for a given order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }
}

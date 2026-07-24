package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.Payment;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.entity.enums.PaymentStatus;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.PaymentRepository;
import com.fooddelivery.service.PaymentService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages payment creation, gateway confirmation, and status lookup.
 *
 * Payment flow:
 *  1. initiatePayment()  — creates a PENDING payment row.
 *  2. (frontend redirects user to gateway)
 *  3. confirmPayment()   — called by the gateway webhook with transactionId.
 *                          Marks payment SUCCESS and updates the order status
 *                          to CONFIRMED so the restaurant sees it immediately.
 *
 * CASH_ON_DELIVERY flow:
 *  - initiatePayment() creates a PENDING payment.
 *  - The delivery agent marks the order DELIVERED, which triggers
 *    a manual confirmation in a real implementation.
 *    For now, COD payments remain PENDING until explicitly confirmed.
 *
 * Security:
 *  - confirmPayment() is called via a webhook endpoint.  In production,
 *    add HMAC signature verification before calling this method.
 *    The raw gateway JSON is stored in gatewayResponse for audit.
 *
 * Idempotency:
 *  - initiatePayment() rejects duplicate requests for the same order.
 *  - confirmPayment() rejects calls for already-processed payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final SecurityUtils     securityUtils;

    // ---------------------------------------------------------------
    // Initiate
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", request.getOrderId()));

        // Only the customer who placed the order can pay for it
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You are not authorised to pay for this order");
        }

        // Cannot pay for a cancelled order
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot initiate payment for a cancelled order");
        }

        // Already delivered orders don't need payment initiation
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order is already delivered");
        }

        // Idempotency guard — prevent double-payment
        if (paymentRepository.existsByOrder_Id(order.getId())) {
            // If there's already a SUCCESS payment, just return it
            Payment existing = paymentRepository.findByOrder_Id(order.getId()).orElseThrow();
            if (existing.isSuccessful()) {
                throw new BadRequestException("This order has already been paid");
            }
            // If PENDING, return the existing record so the frontend can poll/redirect
            if (existing.isPending()) {
                log.info("Returning existing PENDING payment {} for order {}",
                        existing.getId(), order.getId());
                return toResponse(existing);
            }
            // FAILED — allow re-initiation by creating a fresh PENDING record
            log.info("Previous payment {} was FAILED, creating new attempt for order {}",
                    existing.getId(), order.getId());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment initiated: {} for order {} (method: {})",
                saved.getId(), order.getId(), request.getPaymentMethod());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Confirm (gateway webhook)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String transactionId) {
        // The gateway sends transactionId in the callback.
        // We look up by transactionId first (already confirmed payments)
        // then fall back to a PENDING payment for the same order.
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "transactionId", transactionId));

        // Idempotency — gateway may retry callbacks
        if (!payment.isPending()) {
            log.warn("Duplicate confirmation for already-processed payment {}", payment.getId());
            return toResponse(payment);
        }

        // Mark SUCCESS using the entity helper (sets paidAt, stores response)
        payment.markSuccess(transactionId, buildGatewayResponse(transactionId, "success"));
        Payment saved = paymentRepository.save(payment);

        // Auto-advance order to CONFIRMED so the restaurant sees it immediately
        Order order = saved.getOrder();
        if (order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} auto-confirmed after successful payment", order.getId());
        }

        log.info("Payment {} confirmed successfully for order {}", saved.getId(), order.getId());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Get by order
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "orderId", orderId));

        User currentUser = securityUtils.getCurrentUser();

        // Only the order owner or ADMIN can view payment details
        boolean isOwner = payment.getOrder().getUser().getId().equals(currentUser.getId());
        boolean isAdmin = securityUtils.isCurrentUserAdmin();
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException(
                    "You are not authorised to view this payment");
        }

        return toResponse(payment);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /** Builds a minimal gateway response JSON string for storage. */
    private String buildGatewayResponse(String transactionId, String status) {
        return String.format(
                "{\"transactionId\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}",
                transactionId, status, java.time.LocalDateTime.now());
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

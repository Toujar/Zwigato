package com.fooddelivery.service.impl;

import com.fooddelivery.config.RazorpayConfig;
import com.fooddelivery.dto.request.PaymentRequest;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.Payment;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.entity.enums.PaymentStatus;
import com.fooddelivery.entity.enums.RefundStatus;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.PaymentRepository;
import com.fooddelivery.service.PaymentService;
import com.fooddelivery.util.SecurityUtils;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Manages payment creation, Razorpay gateway integration, retry logic, and refund handling.
 *
 * Payment flow:
 *  1. initiatePayment()  — creates Razorpay order, returns client-side details.
 *  2. (frontend collects payment via Razorpay Checkout)
 *  3. confirmPayment()   — called after Razorpay success, verifies signature,
 *                          marks payment SUCCESS and confirms order.
 *
 * Retry flow (on payment failure):
 *  1. retryPayment()     — increments retry counter, allows new attempt.
 *  2. initiatePayment()  — creates new Razorpay order, same customer tries again.
 *
 * Refund flow (on order cancellation):
 *  1. processRefund()    — calls Razorpay refund API, sets refund PENDING.
 *  2. checkRefundStatus()— polls Razorpay for refund completion status.
 *
 * Webhook Security:
 *  - confirmPayment() validates Razorpay signature before accepting.
 *  - Raw gateway JSON stored in gatewayResponse for audit/disputes.
 *
 * Idempotency:
 *  - initiatePayment() rejects duplicate active payments.
 *  - confirmPayment() is idempotent for already-processed payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final SecurityUtils     securityUtils;
    private final RazorpayConfig    razorpayConfig;

    private static final String RECEIPT_PREFIX = "order_";
    private static final int    RETRY_DELAY_SECONDS = 300; // 5 minutes

    // ---------------------------------------------------------------
    // Initiate (create Razorpay order)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", request.getOrderId()));

        // Only the customer can pay
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorised to pay for this order");
        }

        // Cannot pay for cancelled/delivered orders
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot initiate payment for a cancelled order");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order is already delivered");
        }

        // Check for existing active payment
        Payment existing = paymentRepository.findByOrder_Id(order.getId()).orElse(null);
        if (existing != null && existing.isSuccessful()) {
            throw new BadRequestException("This order has already been paid");
        }

        try {
            // Create Razorpay order
            RazorpayClient razorpayClient = razorpayConfig.razorpayClient();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()); // amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", RECEIPT_PREFIX + order.getId());
            orderRequest.put("payment_capture", 1); // auto-capture on success

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id").toString();

            // Create or update payment record
            Payment payment;
            if (existing != null) {
                // Reuse failed payment, increment retry
                payment = existing;
                payment.setRazorpayOrderId(razorpayOrderId);
                payment.setRetryCount(payment.getRetryCount() + 1);
                payment.setLastRetryAt(LocalDateTime.now());
                log.info("Retrying payment {} for order {} (attempt {}/{})",
                        payment.getId(), order.getId(), payment.getRetryCount(), payment.getMaxRetries());
            } else {
                // New payment
                payment = Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .paymentMethod(request.getPaymentMethod())
                        .status(PaymentStatus.PENDING)
                        .razorpayOrderId(razorpayOrderId)
                        .retryCount(0)
                        .maxRetries(3)
                        .build();
                log.info("Payment initiated for order {} (razorpayOrderId: {})",
                        order.getId(), razorpayOrderId);
            }

            Payment saved = paymentRepository.save(payment);
            return toResponse(saved);

        } catch (Exception e) {
            log.error("Failed to create Razorpay order for order {}: {}", order.getId(), e.getMessage(), e);
            throw new BadRequestException("Failed to initiate payment: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Confirm (after Razorpay success)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String razorpayPaymentId, String razorpayOrderId, String signature) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "razorpayOrderId", razorpayOrderId));

        // Idempotency check
        if (!payment.isPending()) {
            log.warn("Payment {} already processed (status: {})", payment.getId(), payment.getStatus());
            return toResponse(payment);
        }

        try {
            // Verify Razorpay signature for security
            if (!verifyRazorpaySignature(razorpayOrderId, razorpayPaymentId, signature)) {
                throw new SecurityException("Invalid Razorpay signature");
            }

            // Mark successful
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayResponse(buildGatewayResponse(razorpayPaymentId, "success"));

            Payment saved = paymentRepository.save(payment);

            // Auto-confirm order so restaurant sees it immediately
            Order order = saved.getOrder();
            if (order.getStatus() == OrderStatus.PLACED) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                log.info("Order {} auto-confirmed after successful payment", order.getId());
            }

            log.info("Payment {} confirmed successfully (razorpayPaymentId: {})",
                    saved.getId(), razorpayPaymentId);
            return toResponse(saved);

        } catch (Exception e) {
            log.error("Failed to confirm payment {}: {}", payment.getId(), e.getMessage(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(buildGatewayResponse(razorpayPaymentId, "error: " + e.getMessage()));
            paymentRepository.save(payment);
            throw new BadRequestException("Payment confirmation failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Retry (user initiates retry after failure)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse retryPayment(Long orderId) {
        User currentUser = securityUtils.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorised to retry this payment");
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Only FAILED payments can be retried
        if (!payment.getStatus().equals(PaymentStatus.FAILED)) {
            throw new BadRequestException("Only failed payments can be retried");
        }

        // Check retry limit
        if (payment.getRetryCount() >= payment.getMaxRetries()) {
            throw new BadRequestException(
                    "Maximum retry attempts (" + payment.getMaxRetries() + ") exceeded. "
                    + "Please contact support.");
        }

        // Check retry delay (prevent immediate re-attempts)
        if (payment.getLastRetryAt() != null) {
            long secondsSinceLastRetry = java.time.temporal.ChronoUnit.SECONDS
                    .between(payment.getLastRetryAt(), LocalDateTime.now());
            if (secondsSinceLastRetry < RETRY_DELAY_SECONDS) {
                throw new BadRequestException(
                        "Please wait " + (RETRY_DELAY_SECONDS - secondsSinceLastRetry) + " seconds before retrying");
            }
        }

        // Reset payment for retry
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRetryCount(payment.getRetryCount() + 1);
        paymentRepository.save(payment);

        log.info("Payment {} marked for retry (attempt {}/{})",
                payment.getId(), payment.getRetryCount(), payment.getMaxRetries());

        // Create new Razorpay order via initiatePayment flow
        PaymentRequest retryRequest = new PaymentRequest();
        retryRequest.setOrderId(orderId);
        retryRequest.setPaymentMethod(payment.getPaymentMethod());

        return initiatePayment(retryRequest);
    }

    // ---------------------------------------------------------------
    // Refund (on order cancellation)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse processRefund(Long orderId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Only successful payments can be refunded
        if (!payment.isSuccessful()) {
            throw new BadRequestException("Cannot refund a payment that is not successful");
        }

        // Validate refund amount
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Invalid refund amount");
        }

        try {
            RazorpayClient razorpayClient = razorpayConfig.razorpayClient();

            // Create Razorpay refund
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).longValue()); // amount in paise

            com.razorpay.Refund razorpayRefund = razorpayClient.payments
                    .refund(payment.getRazorpayPaymentId(), refundRequest);

            String razorpayRefundId = razorpayRefund.get("id").toString();

            // Update payment record
            payment.setRefundAmount(refundAmount);
            payment.setRefundStatus(RefundStatus.PENDING);
            payment.setRazorpayRefundId(razorpayRefundId);
            payment.setStatus(PaymentStatus.REFUND_IN_PROGRESS);
            payment.setGatewayResponse(buildGatewayResponse(razorpayRefundId, "refund_initiated"));

            Payment saved = paymentRepository.save(payment);
            log.info("Refund initiated for payment {} (razorpayRefundId: {}, amount: {})",
                    payment.getId(), razorpayRefundId, refundAmount);

            return toResponse(saved);

        } catch (Exception e) {
            log.error("Failed to process refund for payment {}: {}", payment.getId(), e.getMessage(), e);
            throw new BadRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Check Refund Status (poll for async completion)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse checkRefundStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getRazorpayRefundId() == null) {
            throw new BadRequestException("No refund is pending for this order");
        }

        try {
            RazorpayClient razorpayClient = razorpayConfig.razorpayClient();
            com.razorpay.Refund razorpayRefund = razorpayClient.refunds
                    .fetch(payment.getRazorpayRefundId());

            String refundStatus = razorpayRefund.get("status").toString();

            // Update based on gateway response
            switch (refundStatus) {
                case "processed":
                    payment.setRefundStatus(RefundStatus.COMPLETED);
                    payment.setStatus(PaymentStatus.REFUNDED);
                    log.info("Refund {} completed for payment {}", payment.getRazorpayRefundId(), payment.getId());
                    break;
                case "failed":
                    payment.setRefundStatus(RefundStatus.FAILED);
                    log.warn("Refund {} failed for payment {}", payment.getRazorpayRefundId(), payment.getId());
                    break;
                case "pending":
                    payment.setRefundStatus(RefundStatus.PENDING);
                    log.debug("Refund {} still pending for payment {}", payment.getRazorpayRefundId(), payment.getId());
                    break;
            }

            payment.setGatewayResponse(razorpayRefund.toString());
            Payment saved = paymentRepository.save(payment);

            return toResponse(saved);

        } catch (Exception e) {
            log.error("Failed to check refund status for payment {}: {}", payment.getId(), e.getMessage(), e);
            throw new BadRequestException("Failed to check refund status: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Get by order
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        User currentUser = securityUtils.getCurrentUser();
        boolean isOwner = payment.getOrder().getUser().getId().equals(currentUser.getId());
        boolean isAdmin = securityUtils.isCurrentUserAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorised to view this payment");
        }

        return toResponse(payment);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Verifies the Razorpay signature using HMAC-SHA256.
     * This ensures the webhook callback is authentic and wasn't tampered with.
     *
     * Signature format: HMAC-SHA256(razorpay_order_id|razorpay_payment_id, api_secret)
     */
    private boolean verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String keySecret = razorpayConfig.getKeySecret();

            // Create HMAC-SHA256 hash
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = 
                    new javax.crypto.spec.SecretKeySpec(
                            keySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String expectedSignature = hexString.toString();
            boolean isValid = signature.equals(expectedSignature);

            if (!isValid) {
                log.warn("Signature verification failed. Expected: {}, Got: {}", expectedSignature, signature);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private String buildGatewayResponse(String referenceId, String status) {
        return String.format(
                "{\"referenceId\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}",
                referenceId, status, LocalDateTime.now());
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .refundStatus(p.getRefundStatus())
                .refundAmount(p.getRefundAmount())
                .razorpayRefundId(p.getRazorpayRefundId())
                .retryCount(p.getRetryCount())
                .maxRetries(p.getMaxRetries())
                .lastRetryAt(p.getLastRetryAt())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

package com.fooddelivery.entity;

import com.fooddelivery.entity.enums.PaymentMethod;
import com.fooddelivery.entity.enums.PaymentStatus;
import com.fooddelivery.entity.enums.RefundStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 *  Entity  : Payment
 *  Table   : payments
 * ============================================================
 *
 *  Stores the payment record for a single Order.
 *  Linked 1:1 to an Order — exactly one payment per order is
 *  enforced by a unique constraint on `order_id`.
 *
 *  Lifecycle (PaymentStatus):
 *   PENDING → SUCCESS   (payment gateway confirms)
 *   PENDING → FAILED    (gateway rejects / timeout)
 *   SUCCESS → REFUNDED  (after successful cancellation refund)
 *
 *  Integration points:
 *   - `transactionId`   : unique reference returned by the payment gateway.
 *                         Used to correlate webhook callbacks.
 *   - `gatewayResponse` : raw JSON body from the gateway stored for
 *                         audit trails and dispute resolution.
 *
 *  Relationships:
 *   - One-to-One → Order  (the order being paid for)
 *
 *  Auditing:
 *   - createdAt (INSERT only)
 *   - updatedAt (refreshed when status changes)
 *   - paidAt    (set explicitly by the service when status → SUCCESS)
 * ============================================================
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_payments_order_id",       columnNames = "order_id"),
        @UniqueConstraint(name = "uq_payments_transaction_id", columnNames = "transaction_id")
    },
    indexes = {
        @Index(name = "idx_payments_order_id",       columnList = "order_id"),
        @Index(name = "idx_payments_status",         columnList = "status"),
        @Index(name = "idx_payments_transaction_id", columnList = "transaction_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "order")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    // ----------------------------------------------------------
    // Primary Key
    // ----------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ----------------------------------------------------------
    // Relationships
    // ----------------------------------------------------------

    /**
     * The order this payment belongs to.
     * unique = true on the join column enforces 1:1 at the DB level.
     * ON DELETE RESTRICT — cannot delete an order that has a payment record.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "order_id",
        nullable   = false,
        unique     = true,
        foreignKey = @ForeignKey(name = "fk_payment_order")
    )
    private Order order;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    /**
     * Total amount charged — must match Order.totalAmount.
     * Validated on the service layer; stored here for payment-record independence.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount format invalid")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * How the customer chose to pay.
     * Stored as STRING (VARCHAR) for readability in the DB.
     */
    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "payment_method",
        nullable         = false,
        length           = 20,
        columnDefinition = "VARCHAR(20)"
    )
    private PaymentMethod paymentMethod;

    /**
     * Current state of the payment transaction.
     * Defaults to PENDING — the service updates this on gateway callback.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "status",
        nullable         = false,
        length           = 10,
        columnDefinition = "VARCHAR(10) DEFAULT 'PENDING'"
    )
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Unique transaction reference returned by the payment gateway.
     * Null until the gateway processes the request.
     * Used to match webhook callbacks to this payment record.
     */
    @Size(max = 255)
    @Column(name = "transaction_id", length = 255, unique = true)
    private String transactionId;

    /**
     * Razorpay order ID (for Razorpay integration).
     * Used to create payments and track against Razorpay's order records.
     */
    @Size(max = 255)
    @Column(name = "razorpay_order_id", length = 255)
    private String razorpayOrderId;

    /**
     * Razorpay payment ID (returned after payment success).
     * Used for refunds and payment reconciliation.
     */
    @Size(max = 255)
    @Column(name = "razorpay_payment_id", length = 255, unique = true)
    private String razorpayPaymentId;

    /**
     * Razorpay refund ID (if refund was initiated).
     * Set when refund is processed.
     */
    @Size(max = 255)
    @Column(name = "razorpay_refund_id", length = 255)
    private String razorpayRefundId;

    /**
     * Amount refunded (partial or full).
     * Initially 0, updated when refund is processed.
     */
    @DecimalMin(value = "0.0", message = "Refund amount cannot be negative")
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * Refund status (PENDING, COMPLETED, FAILED).
     * Tracks async refund processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "refund_status",
        length           = 20,
        columnDefinition = "VARCHAR(20)"
    )
    private RefundStatus refundStatus;

    /**
     * Number of retry attempts for failed payments.
     * Incremented each time payment is retried.
     */
    @Min(value = 0)
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Max retry attempts before giving up on payment.
     */
    @Min(value = 1)
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Timestamp of last retry attempt.
     * Used to implement exponential backoff.
     */
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    /**
     * Raw JSON response body from the payment gateway.
     * Stored as TEXT for auditing, reconciliation, and dispute resolution.
     * Never exposed in API responses.
     */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    /**
     * Exact timestamp when the payment reached SUCCESS status.
     * Null until the payment is confirmed.
     * Set explicitly by the service layer: payment.setPaidAt(LocalDateTime.now())
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ----------------------------------------------------------
    // Auditing
    // ----------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    /** Returns true when this payment has been successfully completed. */
    public boolean isSuccessful() {
        return PaymentStatus.SUCCESS.equals(this.status);
    }

    /** Returns true when this payment is still waiting for gateway confirmation. */
    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.status);
    }

    /** Returns true when this payment has been refunded. */
    public boolean isRefunded() {
        return PaymentStatus.REFUNDED.equals(this.status);
    }

    /**
     * Marks the payment as successful.
     * Called by the service layer on a confirmed gateway callback.
     */
    public void markSuccess(String transactionId, String gatewayResponse) {
        this.status          = PaymentStatus.SUCCESS;
        this.transactionId   = transactionId;
        this.gatewayResponse = gatewayResponse;
        this.paidAt          = LocalDateTime.now();
    }

    /**
     * Marks the payment as failed.
     * Called by the service layer on a rejected or timed-out gateway response.
     */
    public void markFailed(String gatewayResponse) {
        this.status          = PaymentStatus.FAILED;
        this.gatewayResponse = gatewayResponse;
    }

    /**
     * Marks the payment as refunded.
     * Called by the service layer when a cancellation refund is confirmed.
     */
    public void markRefunded(String gatewayResponse) {
        this.status          = PaymentStatus.REFUNDED;
        this.gatewayResponse = gatewayResponse;
    }
}

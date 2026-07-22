package com.fooddelivery.entity;

import com.fooddelivery.entity.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  Entity  : Order
 *  Table   : orders
 * ============================================================
 *
 *  Represents a customer's placed order.
 *  An order is an immutable snapshot of the cart at checkout time.
 *  Once placed, item prices, quantities, and the delivery address
 *  must not change (only `status` and `deliveryAgent` are mutable).
 *
 *  Lifecycle (OrderStatus):
 *   PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
 *                                  ↘ CANCELLED (any stage before DELIVERING)
 *
 *  Relationships:
 *   - Many-to-One → User       (the customer who placed the order)
 *   - Many-to-One → Restaurant (from which the order was placed)
 *   - Many-to-One → User       (the delivery agent — nullable until assigned)
 *   - One-to-Many → OrderItem  (the individual item lines)
 *   - One-to-One  → Payment    (the payment record — nullable until initiated)
 *
 *  Amount breakdown (all stored to prevent re-calculation drift):
 *   subtotal    = sum of all OrderItem.subtotals
 *   deliveryFee = charged on top of subtotal
 *   tax         = calculated as a percentage of subtotal
 *   totalAmount = subtotal + deliveryFee + tax
 *
 *  Auditing:
 *   - placedAt  — timestamp of order creation (named placedAt not createdAt
 *                 for domain clarity)
 *   - updatedAt — refreshed whenever status changes
 * ============================================================
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id",           columnList = "user_id"),
        @Index(name = "idx_orders_restaurant_id",     columnList = "restaurant_id"),
        @Index(name = "idx_orders_delivery_agent_id", columnList = "delivery_agent_id"),
        @Index(name = "idx_orders_status",            columnList = "status"),
        @Index(name = "idx_orders_placed_at",         columnList = "placed_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"orderItems", "payment"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    // ----------------------------------------------------------
    // Primary Key
    // ----------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ----------------------------------------------------------
    // Relationships — Owners / Participants
    // ----------------------------------------------------------

    /**
     * The customer who placed this order.
     * ON DELETE RESTRICT — cannot delete a user who has orders.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "user_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_order_user")
    )
    private User user;

    /**
     * The restaurant from which the order was placed.
     * ON DELETE RESTRICT — cannot delete a restaurant that has orders.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "restaurant_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_order_restaurant")
    )
    private Restaurant restaurant;

    /**
     * The delivery agent assigned to this order.
     * Null at placement time — set by ADMIN or an assignment service
     * when the order transitions to OUT_FOR_DELIVERY.
     * ON DELETE SET NULL — agent account deletion does not break the order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "delivery_agent_id",
        foreignKey = @ForeignKey(name = "fk_order_agent")
    )
    private User deliveryAgent;

    // ----------------------------------------------------------
    // Delivery Details
    // ----------------------------------------------------------

    /**
     * Snapshot of the delivery address at order time.
     * Stored independently from User.address so address changes
     * do not alter historical orders.
     */
    @NotBlank(message = "Delivery address is required")
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    // ----------------------------------------------------------
    // Status
    // ----------------------------------------------------------

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "status",
        nullable         = false,
        length           = 20,
        columnDefinition = "VARCHAR(20) DEFAULT 'PLACED'"
    )
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    // ----------------------------------------------------------
    // Amount Breakdown
    // ----------------------------------------------------------

    /**
     * Sum of all OrderItem subtotals (before fees and tax).
     * Validated as ≥ 0.01 to avoid zero-value orders.
     */
    @NotNull
    @DecimalMin(value = "0.01", message = "Subtotal must be greater than 0")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Fee charged for delivery.
     * May be 0.00 if the restaurant offers free delivery.
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "Delivery fee cannot be negative")
    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /**
     * Tax applied to the order subtotal.
     * May be 0.00 in tax-exempt regions.
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    @Column(name = "tax", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    /**
     * Grand total: subtotal + deliveryFee + tax.
     * Stored explicitly to prevent floating-point drift during display.
     */
    @NotNull
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ----------------------------------------------------------
    // Optional Notes
    // ----------------------------------------------------------

    /** Free-text notes from the customer (e.g. "No onions please"). */
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    // ----------------------------------------------------------
    // Auditing
    // ----------------------------------------------------------

    /** Named placedAt (not createdAt) for domain-clarity. */
    @CreatedDate
    @Column(name = "placed_at", nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ----------------------------------------------------------
    // Relationships — Collections
    // ----------------------------------------------------------

    /**
     * The line items that make up this order.
     * Cascade ALL + orphanRemoval mirrors the Cart → CartItem pattern.
     */
    @OneToMany(
        mappedBy      = "order",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * The payment record for this order.
     * Null until the customer initiates payment.
     * Cascade ALL so saving/deleting the order handles its payment.
     */
    @OneToOne(
        mappedBy = "order",
        cascade  = CascadeType.ALL,
        fetch    = FetchType.LAZY
    )
    private Payment payment;

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    /** Returns true if the order can still be cancelled by the customer. */
    public boolean isCancellable() {
        return status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED;
    }

    /** Returns true if the order has been successfully completed. */
    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    /** Returns true if the order has been paid. */
    public boolean isPaid() {
        return payment != null && payment.getStatus() != null
                && payment.getStatus().name().equals("SUCCESS");
    }

    /** Adds an OrderItem and wires the back-reference. */
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
}

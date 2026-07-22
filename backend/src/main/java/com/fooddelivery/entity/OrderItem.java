package com.fooddelivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ============================================================
 *  Entity  : OrderItem
 *  Table   : order_items
 * ============================================================
 *
 *  A single line entry inside an Order.
 *  Mirrors CartItem but belongs to a placed Order, not a Cart.
 *
 *  Immutability contract:
 *   Once an order is placed, its items must NEVER be modified.
 *   All financial fields (unitPrice, subtotal) are price snapshots
 *   taken at placement time, decoupled from FoodItem.price.
 *   This ensures that changing a menu price does not alter
 *   historical orders or invoices.
 *
 *  Subtotal:
 *   Stored as a computed-and-persisted value (quantity × unitPrice).
 *   Avoids re-computation on every read and prevents rounding
 *   inconsistencies across different Java environments.
 *
 *  Relationships:
 *   - Many-to-One → Order     (the parent order)
 *   - Many-to-One → FoodItem  (the menu item ordered — kept for name/image lookup)
 *
 *  No auditing columns:
 *   OrderItem creation time is derived from Order.placedAt.
 *   Items cannot be updated after placement.
 * ============================================================
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id",     columnList = "order_id"),
        @Index(name = "idx_order_items_food_item_id", columnList = "food_item_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "foodItem"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem {

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
     * The order this line item belongs to.
     * ON DELETE CASCADE — removing an order removes all its items.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "order_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_order_item_order")
    )
    private Order order;

    /**
     * The food item that was ordered.
     * ON DELETE RESTRICT — cannot delete a food item that appears in orders.
     * The FoodItem reference is kept to allow name/image display on order history.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "food_item_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_order_item_food")
    )
    private FoodItem foodItem;

    // ----------------------------------------------------------
    // Snapshot Fields (immutable after INSERT)
    // ----------------------------------------------------------

    /**
     * Number of units ordered.
     * Must be ≥ 1 — zero-quantity items must not be stored.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price per unit at the time the order was placed.
     * Copied from FoodItem.price by the service layer during checkout.
     * Immutable after INSERT.
     */
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Price format invalid")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Pre-computed line total: quantity × unitPrice.
     * Persisted to avoid recalculation and ensure consistent invoicing.
     * Must be set by the service layer before saving — not auto-generated.
     */
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.01", message = "Subtotal must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Subtotal format invalid")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // ----------------------------------------------------------
    // Factory Method
    // ----------------------------------------------------------

    /**
     * Creates an OrderItem from a CartItem, snapshotting the current
     * unit price and computing the subtotal.
     *
     * @param cartItem  the source cart line
     * @return a fully populated OrderItem (order reference still null — set by Order.addOrderItem)
     */
    public static OrderItem fromCartItem(CartItem cartItem) {
        BigDecimal unitPrice = cartItem.getUnitPrice();
        int        quantity  = cartItem.getQuantity();
        BigDecimal subtotal  = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return OrderItem.builder()
                .foodItem(cartItem.getFoodItem())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();
    }
}

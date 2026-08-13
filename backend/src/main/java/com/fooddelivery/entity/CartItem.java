package com.fooddelivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 *  Entity  : CartItem
 *  Table   : cart_items
 * ============================================================
 *
 *  A single line entry inside a Cart.
 *  Links a FoodItem to a Cart with a quantity and a price snapshot.
 *
 *  Composite unique constraint (cart_id, food_item_id):
 *   Ensures the same food item cannot appear twice as separate rows.
 *   When a user adds the same item again the service layer must
 *   increment `quantity` on the existing CartItem instead.
 *
 *  Price snapshot (unitPrice):
 *   Copied from FoodItem.price at the moment the item is added.
 *   This protects the cart total if the owner changes the price
 *   while the customer is still browsing.  The service must
 *   re-evaluate the price when the order is placed (configurable).
 *
 *  Relationships:
 *   - Many-to-One → Cart      (the parent cart)
 *   - Many-to-One → FoodItem  (the menu item being ordered)
 *
 *  Auditing:
 *   - createdAt (INSERT only)
 *   - updatedAt (refreshed when quantity is changed)
 * ============================================================
 */
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_cart_food",
            columnNames = {"cart_id", "food_item_id"}
        )
    },
    indexes = {
        @Index(name = "idx_cart_items_cart_id",      columnList = "cart_id"),
        @Index(name = "idx_cart_items_food_item_id", columnList = "food_item_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart", "foodItem"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem {

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
     * The cart this line item belongs to.
     * ON DELETE CASCADE — removing a cart removes all its items.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "cart_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_cart_item_cart")
    )
    private Cart cart;

    /**
     * The food item being added to the cart.
     * ON DELETE CASCADE — removing a food item also removes cart lines
     * referencing it (handled in schema; service should warn the user).
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "food_item_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_cart_item_food")
    )
    private FoodItem foodItem;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    /**
     * Number of units of this food item in the cart.
     * Must be at least 1 — removing the last unit should delete the row,
     * not set quantity to 0.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Price snapshot — copied from FoodItem.price when the item is added.
     *
     * Stored as a separate column so cart totals remain stable even if the
     * restaurant owner changes the price of the item between sessions.
     * The service layer may choose to refresh this value on checkout.
     */
    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ----------------------------------------------------------
    // Customization & Special Instructions
    // ----------------------------------------------------------

    /**
     * Selected size for this item (e.g., "small", "medium", "large").
     * Optional — items without variants will leave this null.
     */
    @Column(name = "size", length = 50)
    private String size;

    /**
     * Spice level preference (e.g., "mild", "medium", "hot").
     * Optional — defaults to restaurant preference if null.
     */
    @Column(name = "spice_level", length = 50)
    private String spiceLevel;

    /**
     * JSON or delimited string of add-ons selected for this item.
     * Example: "extra_cheese,bacon" or {"addOn1": true, "addOn2": false}
     */
    @Column(name = "add_ons", columnDefinition = "TEXT")
    private String addOns;

    /**
     * Item-level special instructions (e.g., "No onions", "Light sauce").
     * Stored at the CartItem level so it moves directly to OrderItem at checkout.
     */
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

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

    /**
     * Computes the line subtotal: quantity × unitPrice.
     * Not persisted — computed on the fly for response mapping.
     */
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Increments quantity by the given amount.
     * Used when the same food item is added again.
     */
    public void incrementQuantity(int amount) {
        this.quantity += amount;
    }
}

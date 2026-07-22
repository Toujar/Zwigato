package com.fooddelivery.entity;

import jakarta.persistence.*;
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
 *  Entity  : Cart
 *  Table   : carts
 * ============================================================
 *
 *  A persistent shopping cart — one per user (1:1 via UNIQUE on user_id).
 *  The cart is created on the user's first "Add to Cart" action
 *  and persists across sessions until the order is placed.
 *
 *  Restaurant lock:
 *   A cart can only contain items from ONE restaurant at a time.
 *   The `restaurant` field is set when the first item is added and
 *   is cleared (set to null) when the cart is emptied.
 *   Attempting to add an item from a different restaurant must be
 *   rejected by the service layer BEFORE calling the repository.
 *
 *  Relationships:
 *   - One-to-One  → User       (the cart owner)
 *   - Many-to-One → Restaurant (locked to one restaurant)
 *   - One-to-Many → CartItem   (individual line items)
 *
 *  CascadeType.ALL + orphanRemoval:
 *   Saving the cart saves its items.
 *   Removing an item from the `cartItems` list deletes it from DB.
 *
 *  Auditing:
 *   - createdAt (INSERT only)
 *   - updatedAt (refreshed whenever items change)
 * ============================================================
 */
@Entity
@Table(
    name = "carts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_carts_user_id", columnNames = "user_id")
    },
    indexes = {
        @Index(name = "idx_carts_user_id", columnList = "user_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "cartItems")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart {

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
     * The user who owns this cart.
     * OneToOne with unique=true enforces the "one cart per user" rule
     * at the database level.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "user_id",
        nullable   = false,
        unique     = true,
        foreignKey = @ForeignKey(name = "fk_cart_user")
    )
    private User user;

    /**
     * The restaurant all items in this cart belong to.
     * Null when the cart is empty.
     * ON DELETE SET NULL in the DB — if a restaurant is removed,
     * the cart loses its lock (cartItems should also be cleared by
     * the service layer in that scenario).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "restaurant_id",
        foreignKey = @ForeignKey(name = "fk_cart_restaurant")
    )
    private Restaurant restaurant;

    /**
     * The individual item lines in this cart.
     *
     * CascadeType.ALL  — persist/merge/remove cascades to items.
     * orphanRemoval    — removing an item from this list deletes it in DB.
     * FetchType.LAZY   — items are loaded only when accessed.
     */
    @OneToMany(
        mappedBy      = "cart",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

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
     * Adds a CartItem and wires the bidirectional reference.
     * Call this instead of cartItems.add() directly.
     */
    public void addItem(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
    }

    /**
     * Removes a CartItem and clears the back-reference.
     * orphanRemoval will delete the row from DB on next flush.
     */
    public void removeItem(CartItem item) {
        cartItems.remove(item);
        item.setCart(null);
    }

    /** Removes all items and clears the restaurant lock. */
    public void clear() {
        cartItems.clear();
        this.restaurant = null;
    }

    /**
     * Calculates the total cart value on the fly.
     * This is not persisted — the authoritative total is calculated
     * in the service layer when creating an Order.
     */
    public BigDecimal calculateTotal() {
        return cartItems.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Returns true when there are no items in the cart. */
    public boolean isEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }
}

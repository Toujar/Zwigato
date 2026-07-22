package com.fooddelivery.entity;

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
 *  Entity  : FoodItem
 *  Table   : food_items
 * ============================================================
 *
 *  Represents a single item on a restaurant's menu.
 *  Each item belongs to exactly one Restaurant and one Category.
 *
 *  Relationships:
 *   - Many-to-One → Restaurant  (which menu it belongs to)
 *   - Many-to-One → Category    (e.g. Pizza, Burger)
 *
 *  Price integrity:
 *   The price field stores the current/live price.
 *   Both CartItem.unitPrice and OrderItem.unitPrice hold
 *   snapshots of this value at add-to-cart / order-placement time,
 *   so historical records are never affected by future price changes.
 *
 *  Availability:
 *   - isAvailable : owner can mark an item as temporarily unavailable
 *   - The CartItem / OrderItem still reference the row even when
 *     unavailable (for historical accuracy).
 *
 *  Auditing:
 *   - createdAt (INSERT only)
 *   - updatedAt (refreshed on every UPDATE)
 * ============================================================
 */
@Entity
@Table(
    name = "food_items",
    indexes = {
        @Index(name = "idx_food_items_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_food_items_category_id",  columnList = "category_id"),
        @Index(name = "idx_food_items_is_available", columnList = "is_available"),
        @Index(name = "idx_food_items_is_veg",       columnList = "is_vegetarian")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"restaurant", "category"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FoodItem {

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
     * The restaurant this item belongs to.
     *
     * ON DELETE CASCADE in the DB schema — if the restaurant is deleted,
     * all its food items are deleted too.
     * In JPA, cascade is handled on the Restaurant side (CascadeType.ALL).
     */
    @NotNull(message = "Restaurant is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "restaurant_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_food_restaurant")
    )
    private Restaurant restaurant;

    /**
     * The food category (e.g. Pizza, Sushi, Desserts).
     *
     * ON DELETE RESTRICT — category cannot be deleted while items reference it.
     */
    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "category_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_food_category")
    )
    private Category category;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    @NotBlank(message = "Food item name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Detailed description — ingredients, allergens, preparation style. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current selling price.
     * Must be > 0 — free items are not supported in this model.
     * Use DECIMAL(10,2) to store up to 99,999,999.99 without rounding.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** URL of the item photo shown on the menu card. */
    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ----------------------------------------------------------
    // Dietary Flags
    // ----------------------------------------------------------

    /**
     * True when the item contains no meat, poultry, or seafood.
     * Used for vegetarian filter on the customer menu page.
     */
    @Column(name = "is_vegetarian", nullable = false)
    @Builder.Default
    private Boolean isVegetarian = false;

    // ----------------------------------------------------------
    // Availability
    // ----------------------------------------------------------

    /**
     * False when the item is temporarily out of stock.
     * "Add to Cart" is disabled for unavailable items on the frontend.
     */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

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
     * Toggles the availability flag and returns the new value.
     * Used by the RESTAURANT_OWNER to mark items in/out of stock.
     */
    public boolean toggleAvailability() {
        this.isAvailable = !this.isAvailable;
        return this.isAvailable;
    }
}

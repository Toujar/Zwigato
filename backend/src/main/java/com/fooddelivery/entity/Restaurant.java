package com.fooddelivery.entity;

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
 *  Entity  : Restaurant
 *  Table   : restaurants
 * ============================================================
 *
 *  Represents a restaurant registered on the platform.
 *  Each restaurant must be owned by a User with role RESTAURANT_OWNER.
 *
 *  Relationships:
 *   - Many-to-One  → User (owner)
 *   - One-to-Many  → FoodItem  (the restaurant's menu)
 *
 *  Business rules embedded here:
 *   - rating  must stay within [0.0 .. 5.0]
 *   - deliveryTime must be at least 1 minute
 *   - minOrderAmount must be ≥ 0
 *   - isOpen   controls real-time availability
 *   - isActive controls soft-delete
 *
 *  Auditing:
 *   - createdAt (INSERT only)
 *   - updatedAt (refreshed on every UPDATE)
 * ============================================================
 */
@Entity
@Table(
    name = "restaurants",
    indexes = {
        @Index(name = "idx_restaurants_owner_id",  columnList = "owner_id"),
        @Index(name = "idx_restaurants_city",      columnList = "city"),
        @Index(name = "idx_restaurants_is_active", columnList = "is_active"),
        @Index(name = "idx_restaurants_is_open",   columnList = "is_open")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "foodItems")          // avoid loading collection in toString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Restaurant {

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
     * The RESTAURANT_OWNER user who manages this restaurant.
     *
     * FetchType.LAZY — avoids loading the full User object on every
     * restaurant query.  Use JOIN FETCH in JPQL where owner data is needed.
     *
     * ON DELETE RESTRICT — prevents deleting a user who still owns restaurants.
     */
    @NotNull(message = "Restaurant must have an owner")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name           = "owner_id",
        nullable       = false,
        foreignKey     = @ForeignKey(name = "fk_restaurant_owner")
    )
    private User owner;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Short marketing description shown on the restaurant card. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Address is required")
    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank(message = "Phone is required")
    @Pattern(
        regexp  = "^[+]?[0-9]{10,15}$",
        message = "Phone must be 10–15 digits, optionally prefixed with +"
    )
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 150)
    @Column(name = "email", length = 150)
    private String email;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ----------------------------------------------------------
    // Operational Fields
    // ----------------------------------------------------------

    /**
     * Average customer rating from 0.0 to 5.0.
     * Updated asynchronously by a rating aggregation job — not
     * directly modifiable by users through the main API.
     */
    @DecimalMin(value = "0.0", message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Estimated delivery time in minutes shown to customers.
     * Minimum of 1 minute required.
     */
    @Min(value = 1, message = "Delivery time must be at least 1 minute")
    @Column(name = "delivery_time", nullable = false)
    @Builder.Default
    private Integer deliveryTime = 30;

    /**
     * Minimum basket value required to place an order.
     * 0.00 means no minimum enforced.
     */
    @DecimalMin(value = "0.0", message = "Minimum order amount cannot be negative")
    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /**
     * Real-time toggle — owners can temporarily close their restaurant
     * without deactivating the account.
     */
    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    /**
     * Soft-delete flag managed by ADMIN.
     * Inactive restaurants are hidden from all customer-facing APIs.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
    // Relationships — Collections
    // ----------------------------------------------------------

    /**
     * The full menu of this restaurant.
     *
     * CascadeType.ALL — saving/deleting a restaurant cascades to its items.
     * orphanRemoval   — removing an item from this list deletes it from DB.
     * FetchType.LAZY  — menu is NOT loaded unless explicitly requested.
     */
    @OneToMany(
        mappedBy      = "restaurant",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<FoodItem> foodItems = new ArrayList<>();

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    /** Convenience — adds an item and wires the back-reference. */
    public void addFoodItem(FoodItem item) {
        foodItems.add(item);
        item.setRestaurant(this);
    }

    /** Convenience — removes an item and clears the back-reference. */
    public void removeFoodItem(FoodItem item) {
        foodItems.remove(item);
        item.setRestaurant(null);
    }
}

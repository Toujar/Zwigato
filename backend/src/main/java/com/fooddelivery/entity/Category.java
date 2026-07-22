package com.fooddelivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  Entity  : Category
 *  Table   : categories
 * ============================================================
 *
 *  Represents a food category such as Pizza, Burger, Sushi,
 *  or Drinks.  Categories are managed by ADMIN users and are
 *  shared across all restaurants.
 *
 *  Relationships:
 *   - One Category HAS many FoodItems  (FoodItem.category)
 *     Mapped bidirectionally so the service layer can fetch
 *     items per category; load is LAZY to prevent full menu
 *     loads every time a category is queried.
 *
 *  Auditing:
 *   - createdAt only (categories rarely change, no updatedAt needed)
 *
 *  Soft-delete:
 *   - isActive flag — deactivated categories hide their items
 *     from the public menu without data loss.
 * ============================================================
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_categories_name", columnNames = "name")
    },
    indexes = {
        @Index(name = "idx_categories_name",      columnList = "name"),
        @Index(name = "idx_categories_is_active", columnList = "is_active")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "foodItems")          // avoid circular toString with FoodItem
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    // ----------------------------------------------------------
    // Primary Key
    // ----------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    /**
     * Human-readable label, e.g. "Pizza", "Burger", "Desserts".
     * Stored lowercase-compared via unique constraint — see repository.
     */
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** Optional short explanation shown on the UI category card. */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;

    /** URL of the category icon or hero image. */
    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ----------------------------------------------------------
    // Status
    // ----------------------------------------------------------

    /**
     * Soft-delete flag.
     * Inactive categories are hidden from the customer-facing API
     * but still referenced by existing FoodItems and Orders.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ----------------------------------------------------------
    // Auditing
    // ----------------------------------------------------------

    /** Timestamp of row creation — immutable after INSERT. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ----------------------------------------------------------
    // Relationships
    // ----------------------------------------------------------

    /**
     * All food items that belong to this category.
     *
     * FetchType.LAZY — do NOT load items when you only need the category name.
     * CascadeType is intentionally omitted: deleting a category should NOT
     * cascade-delete its food items (use business logic to guard this).
     */
    @OneToMany(
        mappedBy = "category",
        fetch    = FetchType.LAZY
    )
    @Builder.Default
    private List<FoodItem> foodItems = new ArrayList<>();
}

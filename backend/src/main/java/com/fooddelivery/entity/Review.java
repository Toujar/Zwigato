package com.fooddelivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  Entity  : Review
 *  Table   : reviews
 * ============================================================
 *
 *  Represents a customer review for either a restaurant or food item.
 *  Polymorphic: one review can target a restaurant OR a food item (not both).
 *
 *  Relationships:
 *   - Many-to-One → User       (the reviewer)
 *   - Many-to-One → Order      (the order being reviewed - for verification)
 *   - Many-to-One → Restaurant (optional - null if reviewing a food item)
 *   - Many-to-One → FoodItem   (optional - null if reviewing a restaurant)
 *
 *  Ratings:
 *   - rating: 1-5 stars
 *   - comment: optional text review
 *   - Restaurant average rating updates when new review is added
 *   - FoodItem average rating updates when new review is added
 *
 *  Auditing:
 *   - createdAt: review creation timestamp
 * ============================================================
 */
@Entity
@Table(
    name = "reviews",
    indexes = {
        @Index(name = "idx_reviews_user_id", columnList = "user_id"),
        @Index(name = "idx_reviews_order_id", columnList = "order_id"),
        @Index(name = "idx_reviews_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_reviews_food_item_id", columnList = "food_item_id"),
        @Index(name = "idx_reviews_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "order", "restaurant", "foodItem"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review {

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
     * The user who wrote this review.
     * ON DELETE CASCADE — deleting a user removes their reviews.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "user_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_review_user")
    )
    private User user;

    /**
     * The order being reviewed.
     * Used to verify the reviewer actually ordered from this restaurant/item.
     * ON DELETE CASCADE — deleting an order removes its reviews.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "order_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_review_order")
    )
    private Order order;

    /**
     * The restaurant being reviewed (if this is a restaurant review).
     * Null if this is a food item review.
     * ON DELETE CASCADE — deleting a restaurant removes its reviews.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "restaurant_id",
        foreignKey = @ForeignKey(name = "fk_review_restaurant")
    )
    private Restaurant restaurant;

    /**
     * The food item being reviewed (if this is a dish review).
     * Null if this is a restaurant review.
     * ON DELETE CASCADE — deleting a food item removes its reviews.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "food_item_id",
        foreignKey = @ForeignKey(name = "fk_review_food_item")
    )
    private FoodItem foodItem;

    // ----------------------------------------------------------
    // Review Content
    // ----------------------------------------------------------

    /**
     * Star rating: 1-5 (required).
     */
    @NotNull
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /**
     * Optional text comment (max 500 chars).
     */
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * Helpful count — number of users who marked this review as helpful.
     * Used for sorting/ranking reviews (most helpful first).
     */
    @Builder.Default
    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount = 0;

    // ----------------------------------------------------------
    // Auditing
    // ----------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ----------------------------------------------------------
    // Validation
    // ----------------------------------------------------------

    @PrePersist
    private void validateReviewTarget() {
        // Exactly one of restaurant or foodItem must be set
        if ((restaurant == null && foodItem == null) || (restaurant != null && foodItem != null)) {
            throw new IllegalArgumentException("Review must target either a restaurant OR a food item, not both or neither");
        }
    }

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    public boolean isRestaurantReview() {
        return restaurant != null;
    }

    public boolean isFoodItemReview() {
        return foodItem != null;
    }
}

package com.fooddelivery.repository;

import com.fooddelivery.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Review entities.
 *
 * Supports:
 *  - Finding reviews by restaurant
 *  - Finding reviews by food item
 *  - Finding reviews by user
 *  - Calculating average ratings
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a restaurant (paginated).
     *
     * @param restaurantId the restaurant ID
     * @param pageable pagination info
     * @return page of reviews for that restaurant
     */
    Page<Review> findByRestaurantId(Long restaurantId, Pageable pageable);

    /**
     * Find all reviews for a food item (paginated).
     *
     * @param foodItemId the food item ID
     * @param pageable pagination info
     * @return page of reviews for that item
     */
    Page<Review> findByFoodItemId(Long foodItemId, Pageable pageable);

    /**
     * Find all reviews by a user (their review history).
     *
     * @param userId the user ID
     * @return list of reviews written by that user
     */
    List<Review> findByUserId(Long userId);

    /**
     * Check if a user has already reviewed a specific order.
     * Prevents duplicate reviews for the same order.
     *
     * @param userId the user ID
     * @param orderId the order ID
     * @return true if a review exists for this user+order combination
     */
    boolean existsByUserIdAndOrderId(Long userId, Long orderId);

    /**
     * Find existing review for a user+order combo.
     * Used to update or delete existing reviews.
     *
     * @param userId the user ID
     * @param orderId the order ID
     * @return the review if one exists for this user+order
     */
    Optional<Review> findByUserIdAndOrderId(Long userId, Long orderId);

    /**
     * Calculate average rating for a restaurant.
     * Used to update Restaurant.rating field.
     *
     * @param restaurantId the restaurant ID
     * @return average rating, or 0.0 if no reviews
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double getAverageRestaurantRating(Long restaurantId);

    /**
     * Calculate average rating for a food item.
     * Used to update FoodItem.rating field.
     *
     * @param foodItemId the food item ID
     * @return average rating, or 0.0 if no reviews
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.foodItem.id = :foodItemId")
    Double getAverageFoodItemRating(Long foodItemId);

    /**
     * Count reviews for a restaurant.
     *
     * @param restaurantId the restaurant ID
     * @return total review count
     */
    Long countByRestaurantId(Long restaurantId);

    /**
     * Count reviews for a food item.
     *
     * @param foodItemId the food item ID
     * @return total review count
     */
    Long countByFoodItemId(Long foodItemId);
}

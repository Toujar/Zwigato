package com.fooddelivery.service;

import com.fooddelivery.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for review management.
 *
 * Responsibilities:
 *  - Create/update/delete reviews for restaurants and food items
 *  - Calculate and update average ratings
 *  - Retrieve reviews by restaurant, food item, or user
 *  - Ensure one review per order per user
 */
public interface ReviewService {

    /**
     * Submit a review for a restaurant or food item.
     *
     * Exactly one of restaurantId or foodItemId must be provided.
     * One review per order per user is enforced.
     *
     * @param orderId the order being reviewed
     * @param rating 1-5 stars
     * @param comment optional text review (max 500 chars)
     * @param restaurantId optional - if reviewing the restaurant
     * @param foodItemId optional - if reviewing a specific dish
     * @return the created Review entity
     * @throws IllegalArgumentException if both or neither restaurantId/foodItemId provided
     * @throws IllegalStateException if user already reviewed this order
     */
    Review submitReview(Long orderId, Integer rating, String comment, Long restaurantId, Long foodItemId);

    /**
     * Update an existing review.
     *
     * Only the review author can update their review.
     *
     * @param reviewId the review to update
     * @param rating new rating (1-5)
     * @param comment new comment
     * @return the updated Review entity
     * @throws IllegalAccessException if current user is not the review author
     */
    Review updateReview(Long reviewId, Integer rating, String comment);

    /**
     * Delete a review.
     *
     * Only the review author or ADMIN can delete reviews.
     *
     * @param reviewId the review to delete
     * @throws IllegalAccessException if current user is not the author or admin
     */
    void deleteReview(Long reviewId);

    /**
     * Get reviews for a restaurant (paginated).
     *
     * @param restaurantId the restaurant ID
     * @param pageable pagination info (sorted by most helpful/recent)
     * @return page of reviews
     */
    Page<Review> getRestaurantReviews(Long restaurantId, Pageable pageable);

    /**
     * Get reviews for a food item (paginated).
     *
     * @param foodItemId the food item ID
     * @param pageable pagination info
     * @return page of reviews
     */
    Page<Review> getFoodItemReviews(Long foodItemId, Pageable pageable);

    /**
     * Get all reviews written by the current user.
     *
     * @return list of user's reviews
     */
    java.util.List<Review> getCurrentUserReviews();

    /**
     * Mark a review as helpful (increment helpful count).
     *
     * @param reviewId the review to mark as helpful
     * @return the updated Review with incremented helpful count
     */
    Review markHelpful(Long reviewId);

    /**
     * Recalculate average rating for a restaurant.
     * Called after each review is added/modified/deleted.
     *
     * @param restaurantId the restaurant ID
     * @return the updated average rating
     */
    java.math.BigDecimal recalculateRestaurantRating(Long restaurantId);

    /**
     * Recalculate average rating for a food item.
     * Called after each review is added/modified/deleted.
     *
     * @param foodItemId the food item ID
     * @return the updated average rating
     */
    java.math.BigDecimal recalculateFoodItemRating(Long foodItemId);
}

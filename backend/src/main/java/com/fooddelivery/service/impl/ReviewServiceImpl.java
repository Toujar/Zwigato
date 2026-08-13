package com.fooddelivery.service.impl;

import com.fooddelivery.entity.*;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.ReviewService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Manages customer reviews for restaurants and food items.
 *
 * Workflow:
 * 1. After order is DELIVERED, customer can submit a review
 * 2. Review targets either restaurant OR food item (not both)
 * 3. One review per order per user is enforced
 * 4. Average rating and review count are auto-updated
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final SecurityUtils securityUtils;

    // ---------------------------------------------------------------
    // Submit Review
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public Review submitReview(Long orderId, Integer rating, String comment, Long restaurantId, Long foodItemId) {
        User currentUser = securityUtils.getCurrentUser();

        // Validate order belongs to current user
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only review your own orders");
        }

        if (order.getStatus().name().equals("DELIVERED") == false) {
            throw new BadRequestException("Can only review delivered orders");
        }

        // Check for existing review
        if (reviewRepository.existsByUserIdAndOrderId(currentUser.getId(), orderId)) {
            throw new BadRequestException("You have already reviewed this order");
        }

        // Validate exactly one of restaurantId or foodItemId is provided
        if ((restaurantId == null && foodItemId == null) || (restaurantId != null && foodItemId != null)) {
            throw new BadRequestException("Review must target either a restaurant OR a food item, not both or neither");
        }

        Review review = new Review();
        review.setUser(currentUser);
        review.setOrder(order);
        review.setRating(rating);
        review.setComment(comment);

        if (restaurantId != null) {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId.toString()));
            review.setRestaurant(restaurant);
        } else {
            FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", foodItemId.toString()));
            review.setFoodItem(foodItem);
        }

        Review saved = reviewRepository.save(review);
        log.info("Review submitted by user {} for order {}", currentUser.getId(), orderId);

        // Recalculate ratings
        if (restaurantId != null) {
            recalculateRestaurantRating(restaurantId);
        } else {
            recalculateFoodItemRating(foodItemId);
        }

        return saved;
    }

    // ---------------------------------------------------------------
    // Update Review
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public Review updateReview(Long reviewId, Integer rating, String comment) {
        User currentUser = securityUtils.getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId.toString()));

        // Verify ownership
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only update your own reviews");
        }

        review.setRating(rating);
        review.setComment(comment);
        Review updated = reviewRepository.save(review);

        // Recalculate ratings
        if (review.isRestaurantReview()) {
            recalculateRestaurantRating(review.getRestaurant().getId());
        } else {
            recalculateFoodItemRating(review.getFoodItem().getId());
        }

        log.info("Review {} updated by user {}", reviewId, currentUser.getId());
        return updated;
    }

    // ---------------------------------------------------------------
    // Delete Review
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User currentUser = securityUtils.getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId.toString()));

        // Verify ownership or admin
        if (!review.getUser().getId().equals(currentUser.getId()) && !securityUtils.isCurrentUserAdmin()) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        Long restaurantId = review.isRestaurantReview() ? review.getRestaurant().getId() : null;
        Long foodItemId = review.isFoodItemReview() ? review.getFoodItem().getId() : null;

        reviewRepository.deleteById(reviewId);
        log.info("Review {} deleted", reviewId);

        // Recalculate ratings
        if (restaurantId != null) {
            recalculateRestaurantRating(restaurantId);
        } else if (foodItemId != null) {
            recalculateFoodItemRating(foodItemId);
        }
    }

    // ---------------------------------------------------------------
    // Retrieve Reviews
    // ---------------------------------------------------------------

    @Override
    public Page<Review> getRestaurantReviews(Long restaurantId, Pageable pageable) {
        // Verify restaurant exists
        restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId.toString()));

        return reviewRepository.findByRestaurantId(restaurantId, pageable);
    }

    @Override
    public Page<Review> getFoodItemReviews(Long foodItemId, Pageable pageable) {
        // Verify food item exists
        foodItemRepository.findById(foodItemId)
            .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", foodItemId.toString()));

        return reviewRepository.findByFoodItemId(foodItemId, pageable);
    }

    @Override
    public List<Review> getCurrentUserReviews() {
        User currentUser = securityUtils.getCurrentUser();
        return reviewRepository.findByUserId(currentUser.getId());
    }

    // ---------------------------------------------------------------
    // Mark Helpful
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public Review markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId.toString()));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewRepository.save(review);
    }

    // ---------------------------------------------------------------
    // Rating Recalculation
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public BigDecimal recalculateRestaurantRating(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId.toString()));

        Double avgRating = reviewRepository.getAverageRestaurantRating(restaurantId);
        Long reviewCount = reviewRepository.countByRestaurantId(restaurantId);

        BigDecimal rating = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP);
        restaurant.setRating(rating);
        restaurant.setReviewCount(reviewCount.intValue());
        restaurantRepository.save(restaurant);

        log.debug("Restaurant {} rating recalculated: {} (from {} reviews)", restaurantId, rating, reviewCount);
        return rating;
    }

    @Override
    @Transactional
    public BigDecimal recalculateFoodItemRating(Long foodItemId) {
        FoodItem foodItem = foodItemRepository.findById(foodItemId)
            .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", foodItemId.toString()));

        Double avgRating = reviewRepository.getAverageFoodItemRating(foodItemId);
        Long reviewCount = reviewRepository.countByFoodItemId(foodItemId);

        BigDecimal rating = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP);
        foodItem.setRating(rating);
        foodItem.setReviewCount(reviewCount.intValue());
        foodItemRepository.save(foodItem);

        log.debug("FoodItem {} rating recalculated: {} (from {} reviews)", foodItemId, rating, reviewCount);
        return rating;
    }
}

package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.entity.Review;
import com.fooddelivery.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles review endpoints for restaurants and food items.
 *
 * POST   /api/reviews                 → submit a review
 * PUT    /api/reviews/{reviewId}      → update a review
 * DELETE /api/reviews/{reviewId}      → delete a review
 * GET    /api/restaurants/{id}/reviews → list restaurant reviews
 * GET    /api/food-items/{id}/reviews → list food item reviews
 * GET    /api/reviews/my-reviews      → customer's review history
 * POST   /api/reviews/{id}/helpful    → mark review as helpful
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "9. Reviews & Ratings")
public class ReviewController {

    private final ReviewService reviewService;

    // ── Request DTOs ──────────────────────────────────────────
    @Data
    static class SubmitReviewRequest {
        @NotNull(message = "Order ID is required")
        Long orderId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be 1-5 stars")
        @Max(value = 5, message = "Rating must be 1-5 stars")
        Integer rating;

        @Size(max = 500, message = "Comment cannot exceed 500 characters")
        String comment;

        Long restaurantId;  // One of these must be provided
        Long foodItemId;    // One of these must be provided
    }

    @Data
    static class UpdateReviewRequest {
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be 1-5 stars")
        @Max(value = 5, message = "Rating must be 1-5 stars")
        Integer rating;

        @Size(max = 500, message = "Comment cannot exceed 500 characters")
        String comment;
    }

    // ── Response DTO ──────────────────────────────────────────
    @Data
    static class ReviewResponse {
        Long id;
        Long userId;
        String userName;
        Integer rating;
        String comment;
        Integer helpfulCount;
        String createdAt;
        boolean isCurrentUserReview;
    }

    // ── POST /api/reviews ─────────────────────────────────────
    /**
     * Submit a new review for a restaurant or food item.
     */
    @PostMapping
    @Operation(summary = "Submit a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(@RequestBody SubmitReviewRequest req) {
        Review review = reviewService.submitReview(
            req.orderId,
            req.rating,
            req.comment,
            req.restaurantId,
            req.foodItemId
        );
        ReviewResponse response = toResponse(review);
        return ResponseEntity.ok(ApiResponse.success(response, "Review submitted successfully"));
    }

    // ── PUT /api/reviews/{reviewId} ──────────────────────────
    /**
     * Update an existing review.
     */
    @PutMapping("/{reviewId}")
    @Operation(summary = "Update a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest req) {
        Review review = reviewService.updateReview(reviewId, req.rating, req.comment);
        ReviewResponse response = toResponse(review);
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }

    // ── DELETE /api/reviews/{reviewId} ────────────────────────
    /**
     * Delete a review.
     */
    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }

    // ── GET /api/restaurants/{restaurantId}/reviews ──────────
    /**
     * Get paginated reviews for a restaurant.
     * Sorted by most helpful first, then most recent.
     */
    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get restaurant reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getRestaurantReviews(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("helpfulCount").descending().and(Sort.by("createdAt").descending()));
        Page<Review> reviews = reviewService.getRestaurantReviews(restaurantId, pageable);
        Page<ReviewResponse> responses = reviews.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses, "Reviews retrieved successfully"));
    }

    // ── GET /api/food-items/{foodItemId}/reviews ─────────────
    /**
     * Get paginated reviews for a food item.
     */
    @GetMapping("/food-item/{foodItemId}")
    @Operation(summary = "Get food item reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getFoodItemReviews(
            @PathVariable Long foodItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("helpfulCount").descending().and(Sort.by("createdAt").descending()));
        Page<Review> reviews = reviewService.getFoodItemReviews(foodItemId, pageable);
        Page<ReviewResponse> responses = reviews.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses, "Reviews retrieved successfully"));
    }

    // ── GET /api/reviews/my-reviews ──────────────────────────
    /**
     * Get current user's review history.
     */
    @GetMapping("/my-reviews")
    @Operation(summary = "Get my reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews() {
        List<Review> reviews = reviewService.getCurrentUserReviews();
        List<ReviewResponse> responses = reviews.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Reviews retrieved successfully"));
    }

    // ── POST /api/reviews/{reviewId}/helpful ─────────────────
    /**
     * Mark a review as helpful (increment helpful count).
     */
    @PostMapping("/{reviewId}/helpful")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(@PathVariable Long reviewId) {
        Review review = reviewService.markHelpful(reviewId);
        ReviewResponse response = toResponse(review);
        return ResponseEntity.ok(ApiResponse.success(response, "Review marked as helpful"));
    }

    // ────────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────────

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());
        response.setUserName(review.getUser().getName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setHelpfulCount(review.getHelpfulCount());
        response.setCreatedAt(review.getCreatedAt().toString());
        return response;
    }
}

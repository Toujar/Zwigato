package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.FoodItemRequest;
import com.fooddelivery.dto.response.FoodItemResponse;
import com.fooddelivery.entity.Category;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.service.FoodItemService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the food item (menu) layer.
 *
 * Key decisions:
 *  - getMenuByRestaurant()  returns only available items (customer view).
 *  - getFoodItemById()      returns any item including unavailable ones
 *                           so owners can see the full state.
 *  - deleteFoodItem()       removes the item from all active carts first
 *                           to prevent stale cart references at checkout.
 *  - toggleAvailability()   is the recommended way to hide/show items
 *                           because it preserves order history references.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodItemServiceImpl implements FoodItemService {

    private final FoodItemRepository    foodItemRepository;
    private final RestaurantRepository  restaurantRepository;
    private final CategoryRepository    categoryRepository;
    private final CartItemRepository    cartItemRepository;
    private final SecurityUtils         securityUtils;

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getMenuByRestaurant(Long restaurantId) {
        // Confirm the restaurant exists (throws 404 if not)
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", "id", restaurantId);
        }
        return foodItemRepository
                .findAvailableWithCategory(restaurantId)   // JOIN FETCH avoids N+1
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FoodItemResponse getFoodItemById(Long id) {
        return toResponse(findById(id));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public FoodItemResponse createFoodItem(FoodItemRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant", "id", request.getRestaurantId()));

        assertOwnerOrAdmin(restaurant, currentUser, "add items to");

        if (!restaurant.getIsActive()) {
            throw new BadRequestException("Cannot add items to a deactivated restaurant");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category", "id", request.getCategoryId()));

        if (!category.getIsActive()) {
            throw new BadRequestException(
                    "Category '" + category.getName() + "' is deactivated");
        }

        FoodItem foodItem = FoodItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .isVegetarian(Boolean.TRUE.equals(request.getIsVegetarian()))
                .isAvailable(true)
                .build();

        FoodItem saved = foodItemRepository.save(foodItem);
        log.info("Food item created: {} ({}) for restaurant {}",
                saved.getName(), saved.getId(), restaurant.getId());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public FoodItemResponse updateFoodItem(Long id, FoodItemRequest request) {
        FoodItem foodItem = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(foodItem.getRestaurant(), currentUser, "update");

        // Category change — validate new category exists and is active
        if (request.getCategoryId() != null
                && !request.getCategoryId().equals(foodItem.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category", "id", request.getCategoryId()));
            if (!category.getIsActive()) {
                throw new BadRequestException(
                        "Category '" + category.getName() + "' is deactivated");
            }
            foodItem.setCategory(category);
        }

        // Apply only non-null fields (partial update)
        if (request.getName()         != null) foodItem.setName(request.getName().trim());
        if (request.getDescription()  != null) foodItem.setDescription(request.getDescription());
        if (request.getPrice()        != null) foodItem.setPrice(request.getPrice());
        if (request.getImageUrl()     != null) foodItem.setImageUrl(request.getImageUrl());
        if (request.getIsVegetarian() != null) foodItem.setIsVegetarian(request.getIsVegetarian());

        FoodItem updated = foodItemRepository.save(foodItem);
        log.info("Food item updated: {} ({})", updated.getName(), id);
        return toResponse(updated);
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void deleteFoodItem(Long id) {
        FoodItem foodItem = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(foodItem.getRestaurant(), currentUser, "delete");

        // Scrub the item from every active cart before deleting
        // (prevents "item no longer available" errors at checkout)
        int cartItemsRemoved = cartItemRepository.deleteByFoodItem_Id(id);
        if (cartItemsRemoved > 0) {
            log.info("Removed food item {} from {} active cart(s)", id, cartItemsRemoved);
        }

        foodItemRepository.delete(foodItem);
        log.info("Food item deleted: {} ({})", foodItem.getName(), id);
    }

    // ---------------------------------------------------------------
    // Toggle availability
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public FoodItemResponse toggleAvailability(Long id) {
        FoodItem foodItem = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(foodItem.getRestaurant(), currentUser, "update");

        boolean newState = foodItem.toggleAvailability(); // calls entity helper
        FoodItem saved = foodItemRepository.save(foodItem);
        log.info("Food item {} availability → {} ({})", saved.getName(), newState, id);
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private FoodItem findById(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
    }

    private void assertOwnerOrAdmin(Restaurant restaurant, User currentUser, String action) {
        boolean isOwner = restaurant.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().equals(UserRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException(
                    "You are not authorised to " + action + " items in this restaurant");
        }
    }

    private FoodItemResponse toResponse(FoodItem f) {
        return FoodItemResponse.builder()
                .id(f.getId())
                .restaurantId(f.getRestaurant().getId())
                .restaurantName(f.getRestaurant().getName())
                .categoryId(f.getCategory().getId())
                .categoryName(f.getCategory().getName())
                .name(f.getName())
                .description(f.getDescription())
                .price(f.getPrice())
                .imageUrl(f.getImageUrl())
                .isVegetarian(f.getIsVegetarian())
                .isAvailable(f.getIsAvailable())
                .createdAt(f.getCreatedAt())
                .build();
    }
}

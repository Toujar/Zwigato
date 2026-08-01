package com.fooddelivery.service.impl;

import com.fooddelivery.config.CacheConstants;
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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================
 *  FoodItemServiceImpl — with Redis caching
 * ============================================================
 *
 *  Cache design for food items:
 *
 *  Two key spaces inside the "foodItems" cache:
 *    1. Single item   → key = item id          (e.g. 42)
 *    2. Restaurant menu → key = "menu_" + restaurantId (e.g. menu_7)
 *
 *  The "menu_" prefix prevents collision between item IDs and
 *  restaurant IDs (both are Longs).
 *
 *  Eviction rules:
 *    CREATE  → evict menu_<restaurantId>   (list is now stale)
 *    UPDATE  → put single item + evict menu_<restaurantId>
 *    DELETE  → evict single item + evict menu_<restaurantId>
 *    TOGGLE  → put single item + evict menu_<restaurantId>
 *
 *  Why CacheManager for deleteFoodItem()?
 *  ─────────────────────────────────────
 *  @CacheEvict on a void method cannot reference #result (no result).
 *  When we need to evict TWO keys — item id AND menu_<restaurantId> —
 *  and the restaurantId is only known at runtime (after loading the entity),
 *  the cleanest solution is to call CacheManager.getCache().evict() directly
 *  after the DB delete succeeds. This avoids self-invocation hacks and is
 *  fully testable.
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodItemServiceImpl implements FoodItemService {

    private final FoodItemRepository   foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository   categoryRepository;
    private final CartItemRepository   cartItemRepository;
    private final SecurityUtils        securityUtils;

    // Injected to perform programmatic cache eviction in deleteFoodItem()
    private final CacheManager         cacheManager;

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    /**
     * Cache the full menu for a restaurant keyed by restaurantId.
     * Key  : zwigato:foodItems::menu_<restaurantId>
     * TTL  : 10 min (CacheConstants.TTL_FOOD_ITEMS)
     *
     * Only available items are cached (customer view).
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.FOOD_ITEMS, key = "'menu_' + #restaurantId")
    public List<FoodItemResponse> getMenuByRestaurant(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", "id", restaurantId);
        }
        return foodItemRepository
                .findAvailableWithCategory(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cache a single food item by its ID.
     * Key: zwigato:foodItems::<id>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.FOOD_ITEMS, key = "#id")
    public FoodItemResponse getFoodItemById(Long id) {
        return toResponse(findById(id));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    /**
     * A new item was added → the restaurant's cached menu list is stale.
     * Evict it so the next GET rebuilds from DB.
     *
     * Key resolves before method body via #request.restaurantId —
     * this is safe because @CacheEvict does not need #result.
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.FOOD_ITEMS, key = "'menu_' + #request.restaurantId")
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

    /**
     * Two cache operations via @Caching:
     *
     *   @CachePut  key = #id
     *     → writes the updated FoodItemResponse into the single-item slot.
     *       Subsequent getFoodItemById() hits cache, not DB.
     *
     *   @CacheEvict key = 'menu_' + #result.restaurantId
     *     → nukes the restaurant's menu list so the next
     *       getMenuByRestaurant() reloads fresh data from DB.
     *     → #result is the FoodItemResponse returned by this method.
     *       Spring evaluates it AFTER the method returns (default behaviour).
     */
    @Override
    @Transactional
    @Caching(
        put   = { @CachePut(value  = CacheConstants.FOOD_ITEMS, key = "#id") },
        evict = { @CacheEvict(value = CacheConstants.FOOD_ITEMS,
                               key  = "'menu_' + #result.restaurantId") }
    )
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

    /**
     * Why programmatic eviction instead of @CacheEvict here?
     * ─────────────────────────────────────────────────────
     * This is a void method — #result is unavailable in SpEL.
     * We need two evictions:
     *   1. Single-item key  = id
     *   2. Menu-list key    = "menu_" + restaurantId
     *
     * restaurantId is only known at runtime (after loading the entity).
     * @Caching with two @CacheEvict annotations would need either:
     *   a) beforeInvocation = true (evicts even if method throws — unsafe)
     *   b) a separate Spring-proxied bean method (overengineered)
     *
     * The cleanest production solution: call CacheManager directly after
     * the DB delete succeeds. This is deterministic, transactional-safe
     * (eviction happens after TX commits via @Transactional), and testable.
     */
    @Override
    @Transactional
    public void deleteFoodItem(Long id) {
        FoodItem foodItem = findById(id);
        Long restaurantId = foodItem.getRestaurant().getId();
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(foodItem.getRestaurant(), currentUser, "delete");

        // Scrub from active carts first (prevents stale checkout)
        int cartItemsRemoved = cartItemRepository.deleteByFoodItem_Id(id);
        if (cartItemsRemoved > 0) {
            log.info("Removed food item {} from {} active cart(s)", id, cartItemsRemoved);
        }

        foodItemRepository.delete(foodItem);
        log.info("Food item deleted: {} ({})", foodItem.getName(), id);

        // Programmatic cache eviction — both keys evicted after DB delete
        var cache = cacheManager.getCache(CacheConstants.FOOD_ITEMS);
        if (cache != null) {
            cache.evict(id);                          // single-item key
            cache.evict("menu_" + restaurantId);      // restaurant menu list
            log.debug("Cache evicted: foodItems::{} and foodItems::menu_{}",
                    id, restaurantId);
        }
    }

    // ---------------------------------------------------------------
    // Toggle availability
    // ---------------------------------------------------------------

    /**
     * isAvailable flipped → both the single-item entry and the menu list
     * must reflect the new state immediately.
     *
     *   @CachePut  key = #id
     *     → writes the updated response into the single-item slot.
     *
     *   @CacheEvict key = 'menu_' + #result.restaurantId
     *     → nukes the menu list (item appears/disappears from customer view).
     *     → #result resolves to the returned FoodItemResponse after return.
     */
    @Override
    @Transactional
    @Caching(
        put   = { @CachePut(value  = CacheConstants.FOOD_ITEMS, key = "#id") },
        evict = { @CacheEvict(value = CacheConstants.FOOD_ITEMS,
                               key  = "'menu_' + #result.restaurantId") }
    )
    public FoodItemResponse toggleAvailability(Long id) {
        FoodItem foodItem = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(foodItem.getRestaurant(), currentUser, "update");

        boolean newState = foodItem.toggleAvailability();
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

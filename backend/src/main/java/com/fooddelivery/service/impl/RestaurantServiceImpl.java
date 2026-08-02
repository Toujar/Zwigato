package com.fooddelivery.service.impl;

import com.fooddelivery.config.CacheConstants;
import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Manages restaurant lifecycle — creation, updates, soft-delete, open/close toggle.
 *
 * Authorization model:
 *  - Any RESTAURANT_OWNER can create a restaurant (owned by themselves).
 *  - Only the restaurant's owner or an ADMIN can update / delete / toggle it.
 *  - Customers can browse but cannot modify.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final SecurityUtils        securityUtils;

    // ---------------------------------------------------------------
    // Browse (public)
    // ---------------------------------------------------------------

    // Paginated list intentionally NOT cached — the page/size/sort
    // combinations create an unbounded key space. Cache only by ID.
    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getAllRestaurants(Pageable pageable) {
        return restaurantRepository
                .findByIsActiveTrueAndIsOpenTrue(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantResponse> searchRestaurants(String keyword, Pageable pageable) {
        return restaurantRepository
                .searchByKeyword(keyword.trim(), pageable)
                .map(this::toResponse);
    }

    /**
     * Cache individual restaurant by ID.
     * Key: zwigato:restaurants::<id>
     * TTL: 10 minutes (configured in RedisConfig / CacheConstants).
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.RESTAURANTS, key = "#id")
    public RestaurantResponse getRestaurantById(Long id) {
        // ── CACHE MISS ── Only prints when Redis has no entry for this id.
        // Call GET /restaurants/{id} twice — you should see this log exactly once.
        log.info("[CACHE MISS] Loading restaurant from MySQL id={}", id);
        return toResponse(findById(id));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    /**
     * No caching on create — there is no existing entry to update.
     * The next getRestaurantById() for the new ID will populate the cache.
     */
    @Override
    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Role guard (belt-and-suspenders on top of @PreAuthorize)
        if (!currentUser.getRole().equals(UserRole.RESTAURANT_OWNER)
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("Only restaurant owners or admins can register restaurants");
        }

        // Duplicate guard — same name + city
        if (restaurantRepository.existsByNameIgnoreCaseAndCityIgnoreCase(
                request.getName(), request.getCity())) {
            throw new BadRequestException(
                    "A restaurant named '" + request.getName()
                            + "' already exists in " + request.getCity());
        }

        Restaurant restaurant = Restaurant.builder()
                .owner(currentUser)
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity().trim())
                .phone(request.getPhone())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .rating(BigDecimal.ZERO)
                .deliveryTime(request.getDeliveryTime() != null ? request.getDeliveryTime() : 30)
                .minOrderAmount(request.getMinOrderAmount() != null
                        ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .isOpen(true)
                .isActive(true)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created: {} ({}) by user {}", saved.getName(), saved.getId(),
                currentUser.getEmail());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    /**
     * @CachePut writes the fresh response back into the cache under the same
     * key so subsequent reads get the updated data without hitting the DB.
     * key = "#id" matches the key used in @Cacheable above.
     */
    @Override
    @Transactional
    @CachePut(value = CacheConstants.RESTAURANTS, key = "#id")
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant restaurant = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(restaurant, currentUser, "update");

        // Duplicate check only if name or city is actually changing
        boolean nameOrCityChanged =
                !restaurant.getName().equalsIgnoreCase(request.getName())
                        || !restaurant.getCity().equalsIgnoreCase(request.getCity());

        if (nameOrCityChanged && restaurantRepository.existsByNameIgnoreCaseAndCityIgnoreCase(
                request.getName(), request.getCity())) {
            throw new BadRequestException(
                    "A restaurant named '" + request.getName()
                            + "' already exists in " + request.getCity());
        }

        restaurant.setName(request.getName().trim());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity().trim());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setImageUrl(request.getImageUrl());
        if (request.getDeliveryTime()   != null) restaurant.setDeliveryTime(request.getDeliveryTime());
        if (request.getMinOrderAmount() != null) restaurant.setMinOrderAmount(request.getMinOrderAmount());

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("Restaurant updated: {} ({})", updated.getName(), id);
        return toResponse(updated);
    }

    // ---------------------------------------------------------------
    // Soft-delete
    // ---------------------------------------------------------------

    /**
     * Evict the cached entry — a deactivated restaurant must not be
     * served from cache to customers.
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.RESTAURANTS, key = "#id")
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(restaurant, currentUser, "delete");

        if (!restaurant.getIsActive()) {
            throw new BadRequestException("Restaurant is already deactivated");
        }

        restaurant.setIsActive(false);
        restaurant.setIsOpen(false);        // also close it so no stray orders arrive
        restaurantRepository.save(restaurant);
        log.info("Restaurant deactivated: {} ({})", restaurant.getName(), id);
    }

    // ---------------------------------------------------------------
    // Toggle open/closed
    // ---------------------------------------------------------------

    /**
     * isOpen changes — the cached entry is now stale.
     * @CachePut refreshes it in place so customers see the new open/closed
     * state immediately without waiting for TTL expiry.
     */
    @Override
    @Transactional
    @CachePut(value = CacheConstants.RESTAURANTS, key = "#id")
    public RestaurantResponse toggleOpen(Long id) {
        Restaurant restaurant = findById(id);
        User currentUser = securityUtils.getCurrentUser();
        assertOwnerOrAdmin(restaurant, currentUser, "toggle");

        if (!restaurant.getIsActive()) {
            throw new BadRequestException("Cannot open a deactivated restaurant");
        }

        restaurant.setIsOpen(!restaurant.getIsOpen());
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant {} toggled isOpen={}", id, saved.getIsOpen());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Restaurant findById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }

    /**
     * Throws UnauthorizedException if the current user is neither
     * the restaurant's owner nor an ADMIN.
     */
    private void assertOwnerOrAdmin(Restaurant restaurant, User currentUser, String action) {
        boolean isOwner = restaurant.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().equals(UserRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException(
                    "You are not authorised to " + action + " this restaurant");
        }
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .ownerId(r.getOwner().getId())
                .ownerName(r.getOwner().getName())
                .name(r.getName())
                .description(r.getDescription())
                .address(r.getAddress())
                .city(r.getCity())
                .phone(r.getPhone())
                .email(r.getEmail())
                .imageUrl(r.getImageUrl())
                .rating(r.getRating())
                .deliveryTime(r.getDeliveryTime())
                .minOrderAmount(r.getMinOrderAmount())
                .isOpen(r.getIsOpen())
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

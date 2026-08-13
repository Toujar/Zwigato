package com.fooddelivery.service;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.RestaurantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for restaurant management operations.
 */
public interface RestaurantService {

    Page<RestaurantResponse> getAllRestaurants(Pageable pageable);

    Page<RestaurantResponse> searchRestaurants(String keyword, Pageable pageable);

    RestaurantResponse getRestaurantById(Long id);

    RestaurantResponse createRestaurant(RestaurantRequest request);

    RestaurantResponse updateRestaurant(Long id, RestaurantRequest request);

    void deleteRestaurant(Long id);

    RestaurantResponse toggleOpen(Long id);

    /**
     * Get nearby restaurants sorted by distance from user's location.
     *
     * @param latitude  user's latitude (optional)
     * @param longitude user's longitude (optional)
     * @param city      optional city filter
     * @param pageable  pagination
     * @return paginated list of nearby restaurants
     */
    Page<RestaurantResponse> getNearbyRestaurants(Double latitude, Double longitude, String city, Pageable pageable);

    /**
     * Search restaurants across all cities by keyword.
     * Optional location-based sorting.
     *
     * @param keyword   search keyword
     * @param latitude  user's latitude (optional, for distance sorting)
     * @param longitude user's longitude (optional, for distance sorting)
     * @param pageable  pagination
     * @return paginated list of matching restaurants
     */
    Page<RestaurantResponse> searchAllRestaurants(String keyword, Double latitude, Double longitude, Pageable pageable);
}

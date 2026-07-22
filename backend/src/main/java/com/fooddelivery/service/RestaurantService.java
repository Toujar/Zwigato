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
}

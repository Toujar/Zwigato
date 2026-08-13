package com.fooddelivery.service;

import com.fooddelivery.dto.request.FoodItemRequest;
import com.fooddelivery.dto.response.FoodItemResponse;

import java.util.List;

/**
 * Contract for food item (menu) management operations.
 */
public interface FoodItemService {

    List<FoodItemResponse> getMenuByRestaurant(Long restaurantId);

    FoodItemResponse getFoodItemById(Long id);

    FoodItemResponse createFoodItem(FoodItemRequest request);

    FoodItemResponse updateFoodItem(Long id, FoodItemRequest request);

    void deleteFoodItem(Long id);

    FoodItemResponse toggleAvailability(Long id);

    /**
     * Search food items across all restaurants by keyword.
     *
     * @param keyword search term
     * @return list of all matching available food items
     */
    List<FoodItemResponse> searchAllFoodItems(String keyword);
}


package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.MenuItemResponse;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final FoodItemRepository foodItemRepository;

    public List<MenuItemResponse> getMenuByRestaurant(Long restaurantId) {
        return foodItemRepository.findByRestaurant_IdAndIsAvailableTrue(restaurantId)
                .stream()
                .map(this::mapToMenuItemResponse)
                .collect(Collectors.toList());
    }

    private MenuItemResponse mapToMenuItemResponse(FoodItem foodItem) {
        // Since MenuItemResponse is empty, let's just create a builder for it, assuming the fields
        return MenuItemResponse.builder()
                .id(foodItem.getId())
                .name(foodItem.getName())
                .description(foodItem.getDescription())
                .price(foodItem.getPrice())
                .category(foodItem.getCategory().getName())
                .imageUrl(foodItem.getImageUrl())
                .isAvailable(foodItem.getIsAvailable())
                .restaurantId(foodItem.getRestaurant().getId())
                .build();
    }
}

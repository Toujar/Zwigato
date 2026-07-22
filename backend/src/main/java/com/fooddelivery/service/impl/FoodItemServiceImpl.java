
package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.FoodItemRequest;
import com.fooddelivery.dto.response.FoodItemResponse;
import com.fooddelivery.entity.Category;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.FoodItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodItemServiceImpl implements FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<FoodItemResponse> getMenuByRestaurant(Long restaurantId) {
        return foodItemRepository.findByRestaurant_IdAndIsAvailableTrue(restaurantId).stream()
                .map(this::mapToFoodItemResponse)
                .toList();
    }

    @Override
    public FoodItemResponse getFoodItemById(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        return mapToFoodItemResponse(foodItem);
    }

    @Override
    @Transactional
    public FoodItemResponse createFoodItem(FoodItemRequest request) {
        User currentUser = getCurrentUser();
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));

        if (!restaurant.getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to add items to this restaurant");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        FoodItem foodItem = FoodItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .isVegetarian(request.getIsVegetarian() != null ? request.getIsVegetarian() : false)
                .isAvailable(true)
                .build();
        FoodItem savedFoodItem = foodItemRepository.save(foodItem);
        return mapToFoodItemResponse(savedFoodItem);
    }

    @Override
    @Transactional
    public FoodItemResponse updateFoodItem(Long id, FoodItemRequest request) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        User currentUser = getCurrentUser();

        if (!foodItem.getRestaurant().getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to update this food item");
        }

        if (request.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));
            foodItem.setRestaurant(restaurant);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            foodItem.setCategory(category);
        }
        if (request.getName() != null) foodItem.setName(request.getName());
        if (request.getDescription() != null) foodItem.setDescription(request.getDescription());
        if (request.getPrice() != null) foodItem.setPrice(request.getPrice());
        if (request.getImageUrl() != null) foodItem.setImageUrl(request.getImageUrl());
        if (request.getIsVegetarian() != null) foodItem.setIsVegetarian(request.getIsVegetarian());

        FoodItem updatedFoodItem = foodItemRepository.save(foodItem);
        return mapToFoodItemResponse(updatedFoodItem);
    }

    @Override
    @Transactional
    public void deleteFoodItem(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        User currentUser = getCurrentUser();
        if (!foodItem.getRestaurant().getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to delete this food item");
        }
        foodItemRepository.delete(foodItem);
    }

    @Override
    @Transactional
    public FoodItemResponse toggleAvailability(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        User currentUser = getCurrentUser();
        if (!foodItem.getRestaurant().getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to update this food item");
        }
        foodItem.setIsAvailable(!foodItem.getIsAvailable());
        FoodItem savedFoodItem = foodItemRepository.save(foodItem);
        return mapToFoodItemResponse(savedFoodItem);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private FoodItemResponse mapToFoodItemResponse(FoodItem foodItem) {
        return FoodItemResponse.builder()
                .id(foodItem.getId())
                .restaurantId(foodItem.getRestaurant().getId())
                .restaurantName(foodItem.getRestaurant().getName())
                .categoryId(foodItem.getCategory().getId())
                .categoryName(foodItem.getCategory().getName())
                .name(foodItem.getName())
                .description(foodItem.getDescription())
                .price(foodItem.getPrice())
                .imageUrl(foodItem.getImageUrl())
                .isVegetarian(foodItem.getIsVegetarian())
                .isAvailable(foodItem.getIsAvailable())
                .createdAt(foodItem.getCreatedAt())
                .build();
    }
}

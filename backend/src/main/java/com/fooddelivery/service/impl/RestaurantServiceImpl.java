
package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Override
    public Page<RestaurantResponse> getAllRestaurants(Pageable pageable) {
        return restaurantRepository.findByIsActiveTrueAndIsOpenTrue(pageable)
                .map(this::mapToRestaurantResponse);
    }

    @Override
    public Page<RestaurantResponse> searchRestaurants(String keyword, Pageable pageable) {
        return restaurantRepository.searchByKeyword(keyword, pageable)
                .map(this::mapToRestaurantResponse);
    }

    @Override
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return mapToRestaurantResponse(restaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.RESTAURANT_OWNER)
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("Only restaurant owners or admins can create restaurants");
        }

        if (restaurantRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.getName(), request.getCity())) {
            throw new BadRequestException("Restaurant with same name already exists in this city");
        }

        Restaurant restaurant = Restaurant.builder()
                .owner(currentUser)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .rating(BigDecimal.ZERO)
                .deliveryTime(request.getDeliveryTime() != null ? request.getDeliveryTime() : 30)
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .isOpen(true)
                .isActive(true)
                .build();
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(savedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        User currentUser = getCurrentUser();

        if (!restaurant.getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to update this restaurant");
        }

        if (!restaurant.getName().equalsIgnoreCase(request.getName())
                || !restaurant.getCity().equalsIgnoreCase(request.getCity())) {
            if (restaurantRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.getName(), request.getCity())) {
                throw new BadRequestException("Restaurant with same name already exists in this city");
            }
        }

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setImageUrl(request.getImageUrl());
        if (request.getDeliveryTime() != null) restaurant.setDeliveryTime(request.getDeliveryTime());
        if (request.getMinOrderAmount() != null) restaurant.setMinOrderAmount(request.getMinOrderAmount());

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(updatedRestaurant);
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        User currentUser = getCurrentUser();

        if (!restaurant.getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to delete this restaurant");
        }
        restaurant.setIsActive(false);
        restaurantRepository.save(restaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse toggleOpen(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        User currentUser = getCurrentUser();

        if (!restaurant.getOwner().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("You are not authorized to update this restaurant");
        }
        restaurant.setIsOpen(!restaurant.getIsOpen());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(savedRestaurant);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private RestaurantResponse mapToRestaurantResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwner().getId())
                .ownerName(restaurant.getOwner().getName())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .city(restaurant.getCity())
                .phone(restaurant.getPhone())
                .email(restaurant.getEmail())
                .imageUrl(restaurant.getImageUrl())
                .rating(restaurant.getRating())
                .deliveryTime(restaurant.getDeliveryTime())
                .minOrderAmount(restaurant.getMinOrderAmount())
                .isOpen(restaurant.getIsOpen())
                .isActive(restaurant.getIsActive())
                .createdAt(restaurant.getCreatedAt())
                .build();
    }
}

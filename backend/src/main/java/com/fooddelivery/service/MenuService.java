
package com.fooddelivery.service;

import com.fooddelivery.dto.response.MenuItemResponse;

import java.util.List;

public interface MenuService {
    List<MenuItemResponse> getMenuByRestaurant(Long restaurantId);
}

package com.fooddelivery.service;

import com.fooddelivery.dto.request.CartItemRequest;
import com.fooddelivery.dto.response.CartResponse;

/**
 * Contract for shopping cart operations.
 */
public interface CartService {

    CartResponse getCartForCurrentUser();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItemQuantity(Long cartItemId, int quantity);

    CartResponse removeItem(Long cartItemId);

    void clearCart();

    /**
     * Check if adding an item from a different restaurant would cause a conflict.
     * Used for the "Clear cart?" dialog UX.
     *
     * @param foodItemId the ID of the food item to be added
     * @return true if the cart has items from a different restaurant
     */
    boolean hasConflictingRestaurant(Long foodItemId);
}

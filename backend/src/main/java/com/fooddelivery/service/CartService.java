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
}

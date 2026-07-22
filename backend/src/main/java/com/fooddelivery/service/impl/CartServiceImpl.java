
package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.CartItemRequest;
import com.fooddelivery.dto.response.CartItemResponse;
import com.fooddelivery.dto.response.CartResponse;
import com.fooddelivery.entity.Cart;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.CartRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse getCartForCurrentUser() {
        User currentUser = getCurrentUser();
        Cart cart = cartRepository.findByUser_IdWithItems(currentUser.getId())
                .orElseGet(() -> createCartForUser(currentUser));
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User currentUser = getCurrentUser();
        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", request.getFoodItemId()));

        if (!foodItem.getIsAvailable()) {
            throw new BadRequestException("Food item is not available");
        }

        Cart cart = cartRepository.findByUser_Id(currentUser.getId())
                .orElseGet(() -> createCartForUser(currentUser));

        if (cart.getRestaurant() != null
                && !cart.getRestaurant().getId().equals(foodItem.getRestaurant().getId())) {
            throw new BadRequestException("Cart already contains items from another restaurant. Clear cart first.");
        }

        if (cart.getRestaurant() == null) {
            cart.setRestaurant(foodItem.getRestaurant());
            cart = cartRepository.save(cart);
        }

        CartItem existingItem = cartItemRepository
                .findByCart_IdAndFoodItem_Id(cart.getId(), foodItem.getId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.incrementQuantity(request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .foodItem(foodItem)
                    .quantity(request.getQuantity())
                    .unitPrice(foodItem.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.findByUser_IdWithItems(currentUser.getId()).orElseThrow();
        return mapToCartResponse(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long cartItemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        User currentUser = getCurrentUser();
        if (!cartItem.getCart().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to update this cart item");
        }
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.findByUser_IdWithItems(currentUser.getId()).orElseThrow();
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        User currentUser = getCurrentUser();
        if (!cartItem.getCart().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to remove this cart item");
        }
        Cart cart = cartItem.getCart();
        cartItemRepository.delete(cartItem);
        List<CartItem> remainingItems = cartItemRepository.findByCart_Id(cart.getId());
        if (remainingItems.isEmpty()) {
            cart.setRestaurant(null);
            cartRepository.save(cart);
        }
        Cart updatedCart = cartRepository.findByUser_IdWithItems(currentUser.getId()).orElseThrow();
        return mapToCartResponse(updatedCart);
    }

    @Override
    @Transactional
    public void clearCart() {
        User currentUser = getCurrentUser();
        Cart cart = cartRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", currentUser.getId()));
        cartItemRepository.deleteByCart_Id(cart.getId());
        cart.setRestaurant(null);
        cartRepository.save(cart);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Cart createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .restaurant(null)
                .build();
        return cartRepository.save(cart);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(this::mapToCartItemResponse)
                .toList();
        BigDecimal total = cart.getCartItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .restaurantId(cart.getRestaurant() != null ? cart.getRestaurant().getId() : null)
                .restaurantName(cart.getRestaurant() != null ? cart.getRestaurant().getName() : null)
                .items(itemResponses)
                .totalAmount(total)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .foodItemId(cartItem.getFoodItem().getId())
                .foodItemName(cartItem.getFoodItem().getName())
                .imageUrl(cartItem.getFoodItem().getImageUrl())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .subtotal(cartItem.getSubtotal())
                .build();
    }
}

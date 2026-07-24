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
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.CartRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.service.CartService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages the shopping cart for the currently authenticated user.
 *
 * Key design decisions:
 *  - Lazy cart creation: the cart row is created on first add-to-cart,
 *    not on user registration.
 *  - Restaurant lock: a cart is locked to one restaurant at a time.
 *    Adding an item from a different restaurant returns 400.
 *  - Add-or-increment: adding the same item again increments quantity
 *    instead of creating a duplicate row.
 *  - Unit price snapshot: CartItem.unitPrice is set from FoodItem.price
 *    at add time so the cart total is stable even if the owner changes
 *    the price mid-session.
 *  - When the last item is removed, the restaurant lock is cleared
 *    so the user can add from any restaurant on their next visit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final SecurityUtils      securityUtils;

    // ---------------------------------------------------------------
    // Get cart
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartForCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUser_IdWithItems(currentUser.getId())
                .orElseGet(() -> createCart(currentUser));
        return toResponse(cart);
    }

    // ---------------------------------------------------------------
    // Add item
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FoodItem", "id", request.getFoodItemId()));

        // Availability guard
        if (!foodItem.getIsAvailable()) {
            throw new BadRequestException(
                    "'" + foodItem.getName() + "' is currently unavailable");
        }
        if (!foodItem.getRestaurant().getIsActive()) {
            throw new BadRequestException("This restaurant is no longer active");
        }

        // Get or create cart
        Cart cart = cartRepository.findByUser_Id(currentUser.getId())
                .orElseGet(() -> createCart(currentUser));

        // Restaurant-lock check
        if (cart.getRestaurant() != null
                && !cart.getRestaurant().getId().equals(foodItem.getRestaurant().getId())) {
            throw new BadRequestException(
                    "Your cart contains items from '"
                            + cart.getRestaurant().getName()
                            + "'. Clear your cart before adding items from a different restaurant.");
        }

        // Lock cart to this restaurant if not yet locked
        if (cart.getRestaurant() == null) {
            cart.setRestaurant(foodItem.getRestaurant());
            cart = cartRepository.save(cart);
        }

        // Add-or-increment
        CartItem existingItem = cartItemRepository
                .findByCart_IdAndFoodItem_Id(cart.getId(), foodItem.getId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.incrementQuantity(request.getQuantity());
            cartItemRepository.save(existingItem);
            log.debug("Cart item incremented: foodItem={} qty+{}", foodItem.getId(), request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .foodItem(foodItem)
                    .quantity(request.getQuantity())
                    .unitPrice(foodItem.getPrice())   // price snapshot
                    .build();
            cartItemRepository.save(newItem);
            log.debug("Cart item added: foodItem={}", foodItem.getId());
        }

        // Re-fetch with JOIN FETCH so the response has all items
        Cart refreshed = cartRepository.findByUser_IdWithItems(currentUser.getId()).orElseThrow();
        return toResponse(refreshed);
    }

    // ---------------------------------------------------------------
    // Update quantity
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long cartItemId, int quantity) {
        CartItem cartItem = findCartItem(cartItemId);
        assertCartOwner(cartItem);

        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1. Use DELETE to remove an item.");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        Cart refreshed = cartRepository
                .findByUser_IdWithItems(securityUtils.getCurrentUser().getId())
                .orElseThrow();
        return toResponse(refreshed);
    }

    // ---------------------------------------------------------------
    // Remove single item
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse removeItem(Long cartItemId) {
        CartItem cartItem = findCartItem(cartItemId);
        assertCartOwner(cartItem);

        Cart cart = cartItem.getCart();
        cartItemRepository.delete(cartItem);

        // If no items remain, release the restaurant lock
        long remaining = cartItemRepository.countByCart_Id(cart.getId());
        if (remaining == 0) {
            cart.setRestaurant(null);
            cartRepository.save(cart);
            log.debug("Cart {} restaurant lock released (cart is now empty)", cart.getId());
        }

        Cart refreshed = cartRepository
                .findByUser_IdWithItems(securityUtils.getCurrentUser().getId())
                .orElseThrow();
        return toResponse(refreshed);
    }

    // ---------------------------------------------------------------
    // Clear entire cart
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void clearCart() {
        User currentUser = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart", "userId", currentUser.getId()));

        cartItemRepository.deleteByCart_Id(cart.getId());
        cart.setRestaurant(null);
        cartRepository.save(cart);
        log.debug("Cart {} cleared for user {}", cart.getId(), currentUser.getEmail());
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /** Creates a new empty cart for the user and persists it. */
    private Cart createCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .restaurant(null)
                .build();
        Cart saved = cartRepository.save(cart);
        log.debug("New cart created for user {}", user.getEmail());
        return saved;
    }

    private CartItem findCartItem(Long id) {
        return cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", id));
    }

    /** Throws if the cart item does not belong to the current user. */
    private void assertCartOwner(CartItem cartItem) {
        Long cartOwnerId = cartItem.getCart().getUser().getId();
        Long currentUserId = securityUtils.getCurrentUser().getId();
        if (!cartOwnerId.equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorised to modify this cart item");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .restaurantId(cart.getRestaurant() != null
                        ? cart.getRestaurant().getId() : null)
                .restaurantName(cart.getRestaurant() != null
                        ? cart.getRestaurant().getName() : null)
                .items(items)
                .totalAmount(total)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem ci) {
        return CartItemResponse.builder()
                .id(ci.getId())
                .foodItemId(ci.getFoodItem().getId())
                .foodItemName(ci.getFoodItem().getName())
                .imageUrl(ci.getFoodItem().getImageUrl())
                .quantity(ci.getQuantity())
                .unitPrice(ci.getUnitPrice())
                .subtotal(ci.getSubtotal())        // computed in entity
                .build();
    }
}

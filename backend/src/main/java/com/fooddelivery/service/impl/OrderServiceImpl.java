
package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.OrderItemRequest;
import com.fooddelivery.dto.request.OrderRequest;
import com.fooddelivery.dto.response.OrderItemResponse;
import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User currentUser = getCurrentUser();
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));

        if (!restaurant.getIsOpen() || !restaurant.getIsActive()) {
            throw new BadRequestException("Restaurant is not accepting orders right now");
        }

        // Create order first
        Order order = Order.builder()
                .user(currentUser)
                .restaurant(restaurant)
                .deliveryAddress(request.getDeliveryAddress())
                .status(OrderStatus.PLACED)
                .specialInstructions(request.getSpecialInstructions())
                .build();

        // Add order items and compute subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            FoodItem foodItem = foodItemRepository.findById(itemRequest.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", itemRequest.getFoodItemId()));

            if (!foodItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException("All items must be from the same restaurant");
            }
            if (!foodItem.getIsAvailable()) {
                throw new BadRequestException("Food item " + foodItem.getName() + " is no longer available");
            }

            BigDecimal unitPrice = foodItem.getPrice();
            int quantity = itemRequest.getQuantity();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = OrderItem.builder()
                    .foodItem(foodItem)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(itemSubtotal)
                    .build();
            order.addOrderItem(orderItem);
            subtotal = subtotal.add(itemSubtotal);
        }

        // Set order amounts
        order.setSubtotal(subtotal);
        order.setDeliveryFee(restaurant.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(50) : BigDecimal.ZERO); // Example delivery fee
        order.setTax(subtotal.multiply(BigDecimal.valueOf(0.05))); // 5% tax example
        order.setTotalAmount(order.getSubtotal().add(order.getDeliveryFee()).add(order.getTax()));

        Order savedOrder = orderRepository.save(order);

        // Clear cart if it exists and matches restaurant
        cartRepository.findByUser_Id(currentUser.getId()).ifPresent(cart -> {
            if (cart.getRestaurant() != null && cart.getRestaurant().getId().equals(restaurant.getId())) {
                cartItemRepository.deleteByCart_Id(cart.getId());
                cart.setRestaurant(null);
                cartRepository.save(cart);
            }
        });

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public Page<OrderResponse> getOrdersForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        return orderRepository.findByUser_IdOrderByPlacedAtDesc(currentUser.getId(), pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItemsAndPayment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId())
                && !currentUser.getRole().name().equals("ADMIN")
                && (order.getRestaurant().getOwner() == null || !order.getRestaurant().getOwner().getId().equals(currentUser.getId()))) {
            throw new BadRequestException("You are not authorized to view this order");
        }
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().name().equals("ADMIN")
                && (order.getRestaurant().getOwner() == null || !order.getRestaurant().getOwner().getId().equals(currentUser.getId()))) {
            throw new BadRequestException("You are not authorized to update this order status");
        }
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }
        if (!order.isCancellable()) {
            throw new BadRequestException("Order cannot be cancelled at this stage");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .deliveryAgentId(order.getDeliveryAgent() != null ? order.getDeliveryAgent().getId() : null)
                .deliveryAgentName(order.getDeliveryAgent() != null ? order.getDeliveryAgent().getName() : null)
                .deliveryAddress(order.getDeliveryAddress())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .tax(order.getTax())
                .totalAmount(order.getTotalAmount())
                .specialInstructions(order.getSpecialInstructions())
                .placedAt(order.getPlacedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getOrderItems().stream().map(this::mapToOrderItemResponse).toList())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .foodItemId(orderItem.getFoodItem().getId())
                .foodItemName(orderItem.getFoodItem().getName())
                .imageUrl(orderItem.getFoodItem().getImageUrl())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getSubtotal())
                .build();
    }
}

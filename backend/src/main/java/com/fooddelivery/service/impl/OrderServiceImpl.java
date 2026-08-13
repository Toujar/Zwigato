package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.OrderItemRequest;
import com.fooddelivery.dto.request.OrderRequest;
import com.fooddelivery.dto.response.OrderItemResponse;
import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.dto.response.PaymentResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.InvoiceService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Manages the full order lifecycle from placement to delivery.
 *
 * Amount calculation:
 *  subtotal     = sum of (unitPrice × quantity) per item
 *  deliveryFee  = flat ₹40 (replace with distance-based logic later)
 *  tax          = 5% of subtotal  (GST placeholder)
 *  totalAmount  = subtotal + deliveryFee + tax
 *
 * Authorization model:
 *  placeOrder    — any authenticated user
 *  getOrderById  — order owner, restaurant owner of that order, or ADMIN
 *  updateStatus  — restaurant owner of that order, DELIVERY_AGENT (assigned), or ADMIN
 *  cancelOrder   — order owner only, while status is PLACED or CONFIRMED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal DELIVERY_FEE     = BigDecimal.valueOf(40);
    private static final BigDecimal TAX_RATE         = BigDecimal.valueOf(0.05); // 5 %

    private final OrderRepository      orderRepository;
    private final FoodItemRepository   foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CartRepository       cartRepository;
    private final CartItemRepository   cartItemRepository;
    private final SecurityUtils        securityUtils;
    private final InvoiceService       invoiceService;

    // ---------------------------------------------------------------
    // Place order
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant", "id", request.getRestaurantId()));

        if (!restaurant.getIsActive()) {
            throw new BadRequestException("Restaurant is no longer active");
        }
        if (!restaurant.getIsOpen()) {
            throw new BadRequestException(
                    "'" + restaurant.getName() + "' is currently closed");
        }

        // Build Order shell (no subtotal yet)
        Order order = Order.builder()
                .user(currentUser)
                .restaurant(restaurant)
                .deliveryAddress(request.getDeliveryAddress().trim())
                .status(OrderStatus.PLACED)
                .specialInstructions(request.getSpecialInstructions())
                .deliveryFee(DELIVERY_FEE)
                .tax(BigDecimal.ZERO)           // recalculated below
                .subtotal(BigDecimal.ZERO)      // recalculated below
                .totalAmount(BigDecimal.ZERO)   // recalculated below
                .build();

        // Validate and add each item
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            FoodItem foodItem = foodItemRepository.findById(itemReq.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "FoodItem", "id", itemReq.getFoodItemId()));

            // Cross-restaurant guard
            if (!foodItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException(
                        "Food item '" + foodItem.getName()
                                + "' does not belong to the selected restaurant");
            }

            if (!foodItem.getIsAvailable()) {
                throw new BadRequestException(
                        "'" + foodItem.getName() + "' is no longer available");
            }

            BigDecimal unitPrice    = foodItem.getPrice();
            int        qty          = itemReq.getQuantity();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

            OrderItem orderItem = OrderItem.builder()
                    .foodItem(foodItem)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(lineSubtotal)
                    .build();
            order.addOrderItem(orderItem);
            subtotal = subtotal.add(lineSubtotal);
        }

        if (subtotal.compareTo(restaurant.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Minimum order amount for this restaurant is ₹"
                            + restaurant.getMinOrderAmount());
        }

        // Final amounts
        BigDecimal tax   = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(DELIVERY_FEE).add(tax);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Clear user's cart if it was locked to this restaurant
        cartRepository.findByUser_Id(currentUser.getId()).ifPresent(cart -> {
            if (cart.getRestaurant() != null
                    && cart.getRestaurant().getId().equals(restaurant.getId())) {
                cartItemRepository.deleteByCart_Id(cart.getId());
                cart.setRestaurant(null);
                cartRepository.save(cart);
            }
        });

        log.info("Order placed: {} for user {} at restaurant {}",
                saved.getId(), currentUser.getEmail(), restaurant.getName());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Get orders for current user  (role-aware)
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForCurrentUser(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();

        // RESTAURANT_OWNER → return orders placed at their restaurants
        if (currentUser.getRole().equals(UserRole.RESTAURANT_OWNER)) {
            return orderRepository
                    .findByRestaurant_Owner_IdOrderByPlacedAtDesc(currentUser.getId(), pageable)
                    .map(this::toResponse);
        }

        // ADMIN → return all orders
        if (currentUser.getRole().equals(UserRole.ADMIN)) {
            return orderRepository
                    .findAll(pageable)
                    .map(this::toResponse);
        }

        // CUSTOMER (default) → return orders they placed
        return orderRepository
                .findByUser_IdOrderByPlacedAtDesc(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // Get order by ID
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItemsAndPayment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        User currentUser = securityUtils.getCurrentUser();
        assertCanViewOrder(order, currentUser);
        return toResponse(order);
    }

    // ---------------------------------------------------------------
    // Update status
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        // Use the JOIN FETCH query so restaurant.owner is already loaded
        Order order = orderRepository.findByIdWithItemsAndPayment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        User currentUser = securityUtils.getCurrentUser();
        assertCanUpdateStatus(order, currentUser);

        // Validate the status transition
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        // Generate invoice when order is delivered
        if (newStatus == OrderStatus.DELIVERED) {
            try {
                invoiceService.generateInvoice(saved);
                log.info("Invoice generated for delivered order {}", id);
            } catch (IOException e) {
                log.error("Failed to generate invoice for order {}: {}", id, e.getMessage());
                // Invoice generation failure should not block order status update
            }
        }

        log.info("Order {} status changed: {} → {} by {}",
                id, previousStatus, newStatus, currentUser.getEmail());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Cancel order
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        User currentUser = securityUtils.getCurrentUser();

        // Only the customer who placed the order can cancel
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You are not authorised to cancel this order");
        }

        if (!order.isCancellable()) {
            throw new BadRequestException(
                    "Order cannot be cancelled once it is in '"
                            + order.getStatus().name() + "' status. "
                            + "Cancellation is only allowed before preparation starts.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        log.info("Order {} cancelled by user {}", id, currentUser.getEmail());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Reorder from past order
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse reorderFromPastOrder(Long orderId) {
        User currentUser = securityUtils.getCurrentUser();

        Order pastOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Only the customer who placed the order can reorder
        if (!pastOrder.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorised to reorder from this order");
        }

        // Get or create cart for current user
        Cart cart = cartRepository.findByUser_Id(currentUser.getId())
                .orElseGet(() -> Cart.builder().user(currentUser).build());

        // If cart has items from a different restaurant, clear it first
        if (cart.getRestaurant() != null 
                && !cart.getRestaurant().getId().equals(pastOrder.getRestaurant().getId())) {
            cartItemRepository.deleteByCart_Id(cart.getId());
            cart.setRestaurant(null);
        }

        // Lock cart to the same restaurant
        cart.setRestaurant(pastOrder.getRestaurant());
        cart = cartRepository.save(cart);

        // Copy items from past order into cart (preserving customizations)
        for (OrderItem orderItem : pastOrder.getOrderItems()) {
            // Check if this item already exists in cart
            CartItem existingItem = cartItemRepository
                    .findByCart_IdAndFoodItem_Id(cart.getId(), orderItem.getFoodItem().getId())
                    .orElse(null);

            if (existingItem != null) {
                // Increment quantity
                existingItem.incrementQuantity(orderItem.getQuantity());
                cartItemRepository.save(existingItem);
            } else {
                // Create new cart item with same customizations
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .foodItem(orderItem.getFoodItem())
                        .quantity(orderItem.getQuantity())
                        .unitPrice(orderItem.getFoodItem().getPrice())  // use current price, not historical
                        .size(orderItem.getSize())
                        .spiceLevel(orderItem.getSpiceLevel())
                        .addOns(orderItem.getAddOns())
                        .specialInstructions(orderItem.getSpecialInstructions())
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        log.info("User {} reordered from order {}", currentUser.getEmail(), orderId);
        return toResponse(pastOrder);
    }

    // ---------------------------------------------------------------
    // Private — authorisation helpers
    // ---------------------------------------------------------------

    /**
     * Who can VIEW an order:
     *  - The customer who placed it.
     *  - The owner of the restaurant the order was placed at.
     *  - Any ADMIN.
     *  - The assigned delivery agent.
     *  - Any DELIVERY_AGENT can view orders in PREPARING/OUT_FOR_DELIVERY
     *    (needed to inspect available orders before accepting).
     */
    private void assertCanViewOrder(Order order, User currentUser) {
        if (currentUser.getRole().equals(UserRole.ADMIN)) return;
        if (order.getUser().getId().equals(currentUser.getId())) return;
        if (order.getRestaurant().getOwner() != null
                && order.getRestaurant().getOwner().getId().equals(currentUser.getId())) return;
        if (order.getDeliveryAgent() != null
                && order.getDeliveryAgent().getId().equals(currentUser.getId())) return;
        // Any delivery agent can view orders that are ready for pickup
        if (currentUser.getRole().equals(UserRole.DELIVERY_AGENT)
                && (order.getStatus() == OrderStatus.PREPARING
                    || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY)) return;

        throw new UnauthorizedException("You are not authorised to view this order");
    }

    /**
     * Who can UPDATE order status:
     *  - The restaurant owner of this specific order.
     *  - The assigned delivery agent (to mark DELIVERED).
     *  - Any ADMIN.
     */
    private void assertCanUpdateStatus(Order order, User currentUser) {
        if (currentUser.getRole().equals(UserRole.ADMIN)) return;
        if (order.getRestaurant().getOwner() != null
                && order.getRestaurant().getOwner().getId().equals(currentUser.getId())) return;
        if (currentUser.getRole().equals(UserRole.DELIVERY_AGENT)
                && order.getDeliveryAgent() != null
                && order.getDeliveryAgent().getId().equals(currentUser.getId())) return;

        throw new UnauthorizedException("You are not authorised to update this order's status");
    }

    /**
     * Enforces valid lifecycle transitions.
     *
     * Valid forward transitions:
     *   PLACED → CONFIRMED
     *   CONFIRMED → PREPARING
     *   PREPARING → OUT_FOR_DELIVERY
     *   OUT_FOR_DELIVERY → DELIVERED
     *
     * CANCELLED is handled separately in cancelOrder().
     * Backward transitions are never allowed.
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PLACED            -> next == OrderStatus.CONFIRMED  || next == OrderStatus.CANCELLED;
            case CONFIRMED         -> next == OrderStatus.PREPARING  || next == OrderStatus.CANCELLED;
            case PREPARING         -> next == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY  -> next == OrderStatus.DELIVERED;
            default                -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + current.name() + " → " + next.name());
        }
    }

    // ---------------------------------------------------------------
    // Mappers
    // ---------------------------------------------------------------

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .userName(o.getUser().getName())
                .restaurantId(o.getRestaurant().getId())
                .restaurantName(o.getRestaurant().getName())
                .restaurantAddress(o.getRestaurant().getAddress() + ", " + o.getRestaurant().getCity())
                .restaurantDeliveryTime(o.getRestaurant().getDeliveryTime())
                .deliveryAgentId(o.getDeliveryAgent() != null
                        ? o.getDeliveryAgent().getId() : null)
                .deliveryAgentName(o.getDeliveryAgent() != null
                        ? o.getDeliveryAgent().getName() : null)
                .deliveryAddress(o.getDeliveryAddress())
                .status(o.getStatus())
                .subtotal(o.getSubtotal())
                .deliveryFee(o.getDeliveryFee())
                .tax(o.getTax())
                .totalAmount(o.getTotalAmount())
                .specialInstructions(o.getSpecialInstructions())
                .placedAt(o.getPlacedAt())
                .updatedAt(o.getUpdatedAt())
                .items(o.getOrderItems() != null
                        ? o.getOrderItems().stream().map(this::toItemResponse).toList()
                        : List.of())
                .payment(o.getPayment() != null ? toPaymentResponse(o.getPayment()) : null)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem oi) {
        return OrderItemResponse.builder()
                .id(oi.getId())
                .foodItemId(oi.getFoodItem().getId())
                .foodItemName(oi.getFoodItem().getName())
                .imageUrl(oi.getFoodItem().getImageUrl())
                .quantity(oi.getQuantity())
                .unitPrice(oi.getUnitPrice())
                .subtotal(oi.getSubtotal())
                .size(oi.getSize())
                .spiceLevel(oi.getSpiceLevel())
                .addOns(oi.getAddOns())
                .specialInstructions(oi.getSpecialInstructions())
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

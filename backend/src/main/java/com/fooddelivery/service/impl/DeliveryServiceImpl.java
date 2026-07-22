
package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.DeliveryService;
import com.fooddelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Override
    public OrderResponse getDeliveryStatus(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @Override
    @Transactional
    public OrderResponse assignDelivery(Long orderId, Long deliveryAgentId) {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Only admins can assign delivery agents");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        User deliveryAgent = userRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", deliveryAgentId));

        if (!deliveryAgent.getRole().name().equals("DELIVERY_AGENT")) {
            throw new BadRequestException("User is not a delivery agent");
        }

        order.setDeliveryAgent(deliveryAgent);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order savedOrder = orderRepository.save(order);

        return orderService.getOrderById(savedOrder.getId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}

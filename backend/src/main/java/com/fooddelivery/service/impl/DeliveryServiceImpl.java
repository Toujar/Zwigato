package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.OrderResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.DeliveryService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Delivery-agent-specific operations layered on top of OrderService.
 *
 * Responsibilities:
 *  getDeliveryStatus() — lets an agent view an order they are assigned to.
 *  assignDelivery()    — ADMIN assigns an agent, status → OUT_FOR_DELIVERY.
 *  markDelivered()     — agent (or ADMIN) confirms drop-off, status → DELIVERED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;
    private final SecurityUtils   securityUtils;
    private final OrderService    orderService;

    // ---------------------------------------------------------------
    // getAvailableOrders — DELIVERY_AGENT: see unassigned orders ready for pickup
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders() {
        User currentUser = securityUtils.getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.DELIVERY_AGENT)
                && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("Only delivery agents can view available orders");
        }
        List<OrderStatus> pickupStatuses = List.of(
                OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY);
        return orderRepository.findAvailableForAgent(pickupStatuses)
                .stream()
                .map(order -> orderService.getOrderById(order.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // ---------------------------------------------------------------
    // getMyDeliveries — DELIVERY_AGENT: see own assigned orders
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyDeliveries(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        return orderRepository
                .findByDeliveryAgent_IdOrderByPlacedAtDesc(currentUser.getId(), pageable)
                .map(o -> orderService.getOrderById(o.getId()));
    }

    // ---------------------------------------------------------------
    // acceptDelivery — DELIVERY_AGENT self-assigns to an unassigned order
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse acceptDelivery(Long orderId) {
        User currentUser = securityUtils.getCurrentUser();

        if (!currentUser.getRole().equals(UserRole.DELIVERY_AGENT)) {
            throw new UnauthorizedException("Only delivery agents can accept deliveries");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getDeliveryAgent() != null) {
            throw new BadRequestException("This order already has a delivery agent assigned");
        }

        if (order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException(
                    "Order is not ready for pickup. Status: " + order.getStatus().name());
        }

        order.setDeliveryAgent(currentUser);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        log.info("Agent {} accepted order {}", currentUser.getEmail(), orderId);
        return orderService.getOrderById(orderId);
    }

    // ---------------------------------------------------------------
    // getDeliveryStatus  — DELIVERY_AGENT (own) or ADMIN
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getDeliveryStatus(Long orderId) {
        User currentUser = securityUtils.getCurrentUser();

        Order order = orderRepository.findByIdWithItemsAndPayment(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Agents can only see orders they are assigned to
        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            boolean isAssigned = order.getDeliveryAgent() != null
                    && order.getDeliveryAgent().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new UnauthorizedException(
                        "You are not the assigned delivery agent for this order");
            }
        }

        return orderService.getOrderById(orderId);
    }

    // ---------------------------------------------------------------
    // assignDelivery  — ADMIN only
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse assignDelivery(Long orderId, Long deliveryAgentId) {
        User currentUser = securityUtils.getCurrentUser();

        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("Only admins can assign delivery agents");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PREPARING) {
            throw new BadRequestException(
                    "Agent can only be assigned when order is CONFIRMED or PREPARING. "
                            + "Current status: " + order.getStatus().name());
        }

        if (order.getDeliveryAgent() != null) {
            throw new BadRequestException("A delivery agent is already assigned to this order");
        }

        User agent = userRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", deliveryAgentId));

        if (!agent.getRole().equals(UserRole.DELIVERY_AGENT)) {
            throw new BadRequestException("User '" + agent.getName() + "' is not a delivery agent");
        }

        if (!agent.getIsActive()) {
            throw new BadRequestException("Delivery agent account is deactivated");
        }

        order.setDeliveryAgent(agent);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        log.info("Agent {} assigned to order {} by admin {}",
                agent.getEmail(), orderId, currentUser.getEmail());

        return orderService.getOrderById(orderId);
    }

    // ---------------------------------------------------------------
    // markDelivered  — DELIVERY_AGENT (own) or ADMIN
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse markDelivered(Long orderId) {
        User currentUser = securityUtils.getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Only the assigned agent or an ADMIN can mark as delivered
        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            boolean isAssigned = order.getDeliveryAgent() != null
                    && order.getDeliveryAgent().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new UnauthorizedException(
                        "You are not the assigned delivery agent for this order");
            }
        }

        // Status must be OUT_FOR_DELIVERY
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException(
                    "Order can only be marked as DELIVERED when it is OUT_FOR_DELIVERY. "
                            + "Current status: " + order.getStatus().name());
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        log.info("Order {} marked as DELIVERED by {}", orderId, currentUser.getEmail());
        return orderService.getOrderById(orderId);
    }
}

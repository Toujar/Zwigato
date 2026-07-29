package com.fooddelivery.scheduler;

import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Automatically advances active orders through the delivery pipeline.
 *
 * Pipeline:
 *   PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
 *
 * Runs every 60 seconds. Orders that are already DELIVERED or CANCELLED
 * are skipped. This simulates a real restaurant/delivery workflow for demo
 * purposes without requiring manual owner intervention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProgressScheduler {

    private final OrderRepository orderRepository;

    /** Maps each active status to the next one in the pipeline. */
    private static final Map<OrderStatus, OrderStatus> NEXT_STATUS = Map.of(
        OrderStatus.PLACED,           OrderStatus.CONFIRMED,
        OrderStatus.CONFIRMED,        OrderStatus.PREPARING,
        OrderStatus.PREPARING,        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED
    );

    /** The statuses we want to advance (terminal statuses are excluded). */
    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
        OrderStatus.PLACED,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING,
        OrderStatus.OUT_FOR_DELIVERY
    );

    /**
     * Runs every 60 seconds.
     * Fetches all non-terminal orders and bumps each one to the next status.
     */
    @Scheduled(fixedDelay = 60_000)   // 60 seconds after last run completes
    @Transactional
    public void advanceOrders() {
        List<Order> activeOrders = orderRepository.findByStatusIn(ACTIVE_STATUSES);

        if (activeOrders.isEmpty()) return;

        log.info("OrderProgressScheduler: advancing {} active order(s)", activeOrders.size());

        for (Order order : activeOrders) {
            OrderStatus next = NEXT_STATUS.get(order.getStatus());
            if (next == null) continue;

            OrderStatus previous = order.getStatus();
            order.setStatus(next);
            orderRepository.save(order);

            log.info("  Order #{} : {} → {}", order.getId(), previous, next);
        }
    }
}

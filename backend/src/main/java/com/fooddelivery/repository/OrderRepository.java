package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : OrderRepository
 *  Entity     : Order
 *  Table      : orders
 * ============================================================
 *
 *  Orders are the financial heart of the system.
 *  Queries serve four distinct actors:
 *   - Customer  : view own order history and order detail
 *   - Restaurant: see incoming orders and update status
 *   - Delivery Agent: see assigned deliveries
 *   - Admin     : full order management and reporting
 *
 *  Custom methods cover:
 *   1. Customer order history (paginated, sorted by newest)
 *   2. Full order detail with items (avoids N+1)
 *   3. Restaurant incoming order queue
 *   4. Delivery agent workload
 *   5. Admin status-based querying
 *   6. Status updates (targeted UPDATE — no full entity load)
 *   7. Revenue and statistics reporting
 *   8. Date-range queries
 * ============================================================
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ----------------------------------------------------------
    // 1. Customer Order History
    // ----------------------------------------------------------

    /**
     * Returns a paginated list of orders placed by a specific user,
     * sorted by placement time descending (newest first).
     *
     * Used by: Customer "Order History" page.
     * Paginated because a long-time customer may have hundreds of orders.
     *
     * SQL: SELECT * FROM orders WHERE user_id = ?
     *      ORDER BY placed_at DESC LIMIT ? OFFSET ?
     */
    Page<Order> findByUser_IdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    /**
     * Returns all orders for a user with a specific status.
     *
     * Used by: Customer filtering — "Show only active orders"
     * (status = PLACED, CONFIRMED, PREPARING, OUT_FOR_DELIVERY).
     *
     * SQL: SELECT * FROM orders WHERE user_id = ? AND status = ?
     *      ORDER BY placed_at DESC
     */
    List<Order> findByUser_IdAndStatusOrderByPlacedAtDesc(Long userId, OrderStatus status);

    // ----------------------------------------------------------
    // 2. Full Order Detail (JOIN FETCH to avoid N+1)
    // ----------------------------------------------------------

    /**
     * Fetches a complete order — including all order items and
     * their food item details — in a SINGLE database round-trip.
     *
     * Used by: OrderService.getOrderById() — the order detail page
     * needs items, food names, prices, and quantities.
     *
     * WHY LEFT JOIN FETCH?
     *   Without it: 1 query for Order + N queries for each OrderItem
     *   + N queries for each FoodItem = (1 + 2N) queries.
     *   With it: always 1 query.
     *
     * LEFT JOIN ensures the order is returned even if it has no items
     * (edge case: newly placed but not yet committed order).
     *
     * JPQL:
     *   SELECT o FROM Order o
     *   LEFT JOIN FETCH o.orderItems oi
     *   LEFT JOIN FETCH oi.foodItem
     *   WHERE o.id = :orderId
     */
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.foodItem
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    /**
     * Fetches a complete order with items, food items, AND payment
     * in one query.
     *
     * Used by: Order confirmation / receipt page — needs all data
     * including payment status.
     *
     * JPQL adds LEFT JOIN FETCH o.payment to the previous query.
     */
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.foodItem
        LEFT JOIN FETCH o.payment
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithItemsAndPayment(@Param("orderId") Long orderId);

    // ----------------------------------------------------------
    // 3. Restaurant Incoming Order Queue
    // ----------------------------------------------------------

    /**
     * Returns all orders for a restaurant, newest first.
     *
     * Used by: Restaurant dashboard — live order queue.
     * Returns all statuses so the owner can see the full picture.
     *
     * SQL: SELECT * FROM orders WHERE restaurant_id = ?
     *      ORDER BY placed_at DESC
     */
    List<Order> findByRestaurant_IdOrderByPlacedAtDesc(Long restaurantId);

    /**
     * Returns orders for a restaurant filtered by a specific status.
     *
     * Used by: Restaurant dashboard tabs — e.g.:
     *   "New Orders"    → status = PLACED
     *   "In Kitchen"    → status = PREPARING
     *   "Out"           → status = OUT_FOR_DELIVERY
     *
     * SQL: SELECT * FROM orders WHERE restaurant_id = ? AND status = ?
     *      ORDER BY placed_at DESC
     */
    List<Order> findByRestaurant_IdAndStatusOrderByPlacedAtDesc(
            Long restaurantId, OrderStatus status);

    /**
     * Counts orders for a restaurant that are in a given status.
     *
     * Used by: Restaurant dashboard badge counts — "3 new orders".
     *
     * SQL: SELECT COUNT(*) FROM orders WHERE restaurant_id = ? AND status = ?
     */
    long countByRestaurant_IdAndStatus(Long restaurantId, OrderStatus status);

    // ----------------------------------------------------------
    // 4. Delivery Agent Workload
    // ----------------------------------------------------------

    /**
     * Returns all orders assigned to a specific delivery agent.
     *
     * Used by: Delivery agent app — "My Deliveries" screen.
     * Sorted newest first.
     *
     * SQL: SELECT * FROM orders WHERE delivery_agent_id = ?
     *      ORDER BY placed_at DESC
     */
    List<Order> findByDeliveryAgent_IdOrderByPlacedAtDesc(Long agentId);

    /**
     * Returns active deliveries for an agent — only orders that
     * are currently OUT_FOR_DELIVERY.
     *
     * Used by: Delivery agent home screen — shows only what they
     * are actively delivering right now (not completed history).
     *
     * SQL: SELECT * FROM orders
     *      WHERE delivery_agent_id = ? AND status = 'OUT_FOR_DELIVERY'
     */
    List<Order> findByDeliveryAgent_IdAndStatus(Long agentId, OrderStatus status);

    /**
     * Finds unassigned orders that need a delivery agent.
     *
     * Used by: Delivery assignment service — scans for CONFIRMED orders
     * with no delivery_agent_id assigned yet.
     *
     * JPQL: WHERE delivery_agent IS NULL AND status = :status
     */
    @Query("SELECT o FROM Order o WHERE o.deliveryAgent IS NULL AND o.status = :status")
    List<Order> findUnassignedByStatus(@Param("status") OrderStatus status);

    // ----------------------------------------------------------
    // 5. Admin Status-Based Querying
    // ----------------------------------------------------------

    /**
     * Returns all orders with a given status (paginated).
     *
     * Used by: Admin order management — filter all platform orders
     * by status to find stuck or problematic orders.
     *
     * SQL: SELECT * FROM orders WHERE status = ?
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // ----------------------------------------------------------
    // 6. Targeted Status Update
    // ----------------------------------------------------------

    /**
     * Updates the status of an order without loading the full entity.
     *
     * Used by: OrderService.updateOrderStatus() — the most frequent
     * write operation on orders. Only one column changes, loading
     * the entire Order (with all its items) is unnecessary.
     *
     * @param id     the order to update
     * @param status the new OrderStatus value
     *
     * @Modifying   — required for JPQL UPDATE.
     * @Transactional — runs atomically.
     *
     * Returns: 1 if updated, 0 if order not found.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);

    /**
     * Assigns a delivery agent to an order.
     *
     * Used by: Delivery assignment service — once an agent is found,
     * writes delivery_agent_id and flips status to OUT_FOR_DELIVERY
     * in the same targeted update.
     *
     * JPQL: UPDATE orders SET delivery_agent_id = :agentId, status = 'OUT_FOR_DELIVERY'
     *       WHERE id = :orderId
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Order o
        SET o.deliveryAgent.id = :agentId,
            o.status           = com.fooddelivery.entity.enums.OrderStatus.OUT_FOR_DELIVERY
        WHERE o.id = :orderId
        """)
    int assignDeliveryAgent(@Param("orderId") Long orderId, @Param("agentId") Long agentId);

    // ----------------------------------------------------------
    // 7. Revenue and Statistics Reporting
    // ----------------------------------------------------------

    /**
     * Calculates total platform revenue from all DELIVERED orders.
     *
     * Used by: Admin dashboard — "Total Revenue" stat card.
     * SUM on total_amount for all orders where status = DELIVERED.
     *
     * Returns BigDecimal (null if no delivered orders exist — handle in service).
     *
     * JPQL: SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = DELIVERED
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = com.fooddelivery.entity.enums.OrderStatus.DELIVERED")
    BigDecimal calculateTotalRevenue();

    /**
     * Calculates revenue for a specific restaurant (DELIVERED orders only).
     *
     * Used by: Restaurant owner dashboard — "Your Total Revenue" stat.
     *
     * JPQL: SELECT SUM(total_amount) FROM orders
     *       WHERE restaurant_id = ? AND status = DELIVERED
     */
    @Query("""
        SELECT SUM(o.totalAmount) FROM Order o
        WHERE o.restaurant.id = :restaurantId
          AND o.status = com.fooddelivery.entity.enums.OrderStatus.DELIVERED
        """)
    BigDecimal calculateRevenueByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Counts total delivered orders for a restaurant.
     *
     * Used by: Restaurant owner dashboard — "Orders completed: 1,204".
     *
     * SQL: SELECT COUNT(*) FROM orders WHERE restaurant_id = ? AND status = 'DELIVERED'
     */
    long countByRestaurant_IdAndStatus(Long restaurantId, OrderStatus status);

    /**
     * Counts total orders placed by a specific customer.
     *
     * Used by: Customer profile — "You have placed 47 orders".
     *
     * SQL: SELECT COUNT(*) FROM orders WHERE user_id = ?
     */
    long countByUser_Id(Long userId);

    // ----------------------------------------------------------
    // 8. Date-Range Queries (Admin Reporting)
    // ----------------------------------------------------------

    /**
     * Returns all orders placed within a date range (paginated).
     *
     * Used by: Admin daily/weekly/monthly reports.
     * Both bounds are inclusive.
     *
     * @param from  start of the range (inclusive)
     * @param to    end of the range (inclusive)
     *
     * JPQL: WHERE placed_at BETWEEN :from AND :to ORDER BY placed_at DESC
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.placedAt BETWEEN :from AND :to
        ORDER BY o.placedAt DESC
        """)
    Page<Order> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);

    /**
     * Calculates revenue for a restaurant within a given date range.
     *
     * Used by: Restaurant revenue graph — weekly / monthly breakdown.
     *
     * JPQL: SELECT SUM(total_amount) WHERE restaurant_id = ?
     *         AND status = DELIVERED AND placed_at BETWEEN ? AND ?
     */
    @Query("""
        SELECT SUM(o.totalAmount) FROM Order o
        WHERE o.restaurant.id = :restaurantId
          AND o.status        = com.fooddelivery.entity.enums.OrderStatus.DELIVERED
          AND o.placedAt      BETWEEN :from AND :to
        """)
    BigDecimal calculateRevenueByRestaurantAndDateRange(
            @Param("restaurantId") Long restaurantId,
            @Param("from")         LocalDateTime from,
            @Param("to")           LocalDateTime to);
}

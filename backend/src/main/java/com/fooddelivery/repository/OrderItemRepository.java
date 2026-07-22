package com.fooddelivery.repository;

import com.fooddelivery.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================
 *  Repository : OrderItemRepository
 *  Entity     : OrderItem
 *  Table      : order_items
 * ============================================================
 *
 *  OrderItem is an immutable line-item record — once an order is
 *  placed, its items must NEVER be modified or deleted independently.
 *  Cascade settings on Order handle persistence automatically.
 *
 *  As a result this repository is intentionally lean: it provides
 *  read-only lookups used for reporting and analytical queries.
 *  Write operations go through the Order entity via CascadeType.ALL.
 *
 *  Custom methods cover:
 *   1. Fetching items for an order
 *   2. Best-seller analytics (most ordered food items)
 *   3. Revenue contribution per food item
 * ============================================================
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ----------------------------------------------------------
    // 1. Fetching Items for an Order
    // ----------------------------------------------------------

    /**
     * Returns all order items for a given order ID.
     *
     * Used by: Scenarios where the Order entity is already loaded
     * and only the items list needs to be fetched separately
     * (e.g., invoice generation, receipt print).
     *
     * For the primary use case (order detail page) prefer
     * OrderRepository.findByIdWithItems() which avoids an extra query.
     *
     * SQL: SELECT * FROM order_items WHERE order_id = ?
     */
    List<OrderItem> findByOrder_Id(Long orderId);

    /**
     * Returns all order items for an order, with food item data
     * pre-fetched to avoid N+1 queries.
     *
     * Used by: Invoice PDF generation — needs item names, prices,
     * and images without triggering lazy loads.
     *
     * JPQL:
     *   SELECT oi FROM OrderItem oi
     *   JOIN FETCH oi.foodItem
     *   WHERE oi.order.id = :orderId
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.foodItem WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrder_IdWithFoodItem(@Param("orderId") Long orderId);

    // ----------------------------------------------------------
    // 2. Best-Seller Analytics
    // ----------------------------------------------------------

    /**
     * Returns the top food items by total quantity ordered for a
     * specific restaurant, in descending order of popularity.
     *
     * Used by: Restaurant owner dashboard — "Your Best Sellers" section.
     * Helps owners understand which items to promote or keep stocked.
     *
     * @param restaurantId  the restaurant to analyse
     * @param pageable      use PageRequest.of(0, 5) to get top 5
     *
     * Returns Object[] rows: [Long foodItemId, String foodItemName, Long totalQuantity]
     * The service layer should project this into a BestSellerResponse DTO.
     *
     * JPQL:
     *   SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity)
     *   FROM OrderItem oi
     *   WHERE oi.order.restaurant.id = :restaurantId
     *   GROUP BY oi.foodItem.id, oi.foodItem.name
     *   ORDER BY SUM(oi.quantity) DESC
     */
    @Query("""
        SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity) AS totalQty
        FROM   OrderItem oi
        WHERE  oi.order.restaurant.id = :restaurantId
        GROUP  BY oi.foodItem.id, oi.foodItem.name
        ORDER  BY totalQty DESC
        """)
    List<Object[]> findBestSellersByRestaurant(
            @Param("restaurantId") Long restaurantId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Returns the platform-wide best sellers — items with the highest
     * cumulative order quantity across all restaurants.
     *
     * Used by: Admin analytics — "Top 10 Items on the Platform".
     *
     * Returns Object[] rows: [Long foodItemId, String foodItemName, Long totalQuantity]
     *
     * JPQL:
     *   SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity)
     *   FROM OrderItem oi
     *   GROUP BY oi.foodItem.id, oi.foodItem.name
     *   ORDER BY SUM(oi.quantity) DESC
     */
    @Query("""
        SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity) AS totalQty
        FROM   OrderItem oi
        GROUP  BY oi.foodItem.id, oi.foodItem.name
        ORDER  BY totalQty DESC
        """)
    List<Object[]> findPlatformWideBestSellers(
            org.springframework.data.domain.Pageable pageable);

    // ----------------------------------------------------------
    // 3. Revenue Contribution Per Food Item
    // ----------------------------------------------------------

    /**
     * Returns the total revenue generated by each food item for
     * a specific restaurant (DELIVERED orders only).
     *
     * Used by: Restaurant analytics — shows revenue contribution
     * per menu item so owners can identify high-value items.
     *
     * Returns Object[] rows: [Long foodItemId, String foodItemName, BigDecimal totalRevenue]
     *
     * JPQL:
     *   SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.subtotal)
     *   FROM OrderItem oi
     *   WHERE oi.order.restaurant.id = :restaurantId
     *     AND oi.order.status = DELIVERED
     *   GROUP BY oi.foodItem.id, oi.foodItem.name
     *   ORDER BY SUM(oi.subtotal) DESC
     */
    @Query("""
        SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.subtotal) AS revenue
        FROM   OrderItem oi
        WHERE  oi.order.restaurant.id = :restaurantId
          AND  oi.order.status = com.fooddelivery.entity.enums.OrderStatus.DELIVERED
        GROUP  BY oi.foodItem.id, oi.foodItem.name
        ORDER  BY revenue DESC
        """)
    List<Object[]> findRevenueByFoodItemForRestaurant(
            @Param("restaurantId") Long restaurantId);

    // ----------------------------------------------------------
    // 4. Count
    // ----------------------------------------------------------

    /**
     * Counts how many times a specific food item has been ordered
     * across all orders.
     *
     * Used by: Admin food item detail page — "Ordered 3,421 times".
     * Also useful for deciding whether a food item can be safely deleted
     * (high order count = historically popular, soft-delete instead).
     *
     * SQL: SELECT COUNT(*) FROM order_items WHERE food_item_id = ?
     */
    long countByFoodItem_Id(Long foodItemId);
}

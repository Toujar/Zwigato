package com.fooddelivery.repository;

import com.fooddelivery.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : CartItemRepository
 *  Entity     : CartItem
 *  Table      : cart_items
 * ============================================================
 *
 *  CartItem is the line-item layer of the cart.
 *  Most operations require both a cart ID and a food item ID
 *  because of the composite unique constraint (cart_id, food_item_id).
 *
 *  Custom methods cover:
 *   1. Fetching items for a cart
 *   2. Finding a specific item inside a cart (add-or-update logic)
 *   3. Existence checks
 *   4. Targeted quantity update (no full entity load)
 *   5. Bulk delete (clear cart / remove by food item)
 * ============================================================
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // ----------------------------------------------------------
    // 1. Fetch Items for a Cart
    // ----------------------------------------------------------

    /**
     * Returns all items in the given cart, joining food item data.
     *
     * Used by: CartService when CartRepository.findByUser_IdWithItems()
     * is not used — e.g., when the cart entity is already in context
     * and only the items need to be re-fetched.
     *
     * SQL: SELECT * FROM cart_items WHERE cart_id = ?
     */
    List<CartItem> findByCart_Id(Long cartId);

    /**
     * Returns all cart items for a given cart, with the food item
     * data pre-fetched to avoid N+1 queries.
     *
     * Used by: Any service method that needs item names, prices,
     * and availability in the same call.
     *
     * JPQL:
     *   SELECT ci FROM CartItem ci
     *   JOIN FETCH ci.foodItem
     *   WHERE ci.cart.id = :cartId
     */
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.foodItem WHERE ci.cart.id = :cartId")
    List<CartItem> findByCart_IdWithFoodItem(@Param("cartId") Long cartId);

    // ----------------------------------------------------------
    // 2. Single-Item Lookup (add-or-update pattern)
    // ----------------------------------------------------------

    /**
     * Finds a specific food item inside a specific cart.
     *
     * Used by: CartService.addItem() — the most critical cart query.
     * If a CartItem for this (cartId, foodItemId) pair already exists,
     * increment its quantity. If empty, create a new CartItem row.
     * This enforces the composite unique constraint at the application level.
     *
     * SQL: SELECT * FROM cart_items WHERE cart_id = ? AND food_item_id = ?
     */
    Optional<CartItem> findByCart_IdAndFoodItem_Id(Long cartId, Long foodItemId);

    /**
     * Same as above but also fetches the food item in the same query.
     *
     * Used by: CartService.updateItem() — needs food item data to
     * validate the price is still current before updating quantity.
     *
     * JPQL:
     *   SELECT ci FROM CartItem ci
     *   JOIN FETCH ci.foodItem
     *   WHERE ci.cart.id = :cartId AND ci.foodItem.id = :foodItemId
     */
    @Query("""
        SELECT ci FROM CartItem ci
        JOIN FETCH ci.foodItem
        WHERE ci.cart.id      = :cartId
          AND ci.foodItem.id  = :foodItemId
        """)
    Optional<CartItem> findByCart_IdAndFoodItem_IdWithFoodItem(
            @Param("cartId")     Long cartId,
            @Param("foodItemId") Long foodItemId);

    // ----------------------------------------------------------
    // 3. Existence Checks
    // ----------------------------------------------------------

    /**
     * Checks whether a specific food item is already in the cart.
     *
     * Used by: CartService.addItem() guard — cheaper boolean check
     * before deciding to create a new row vs increment an existing one.
     *
     * SQL: SELECT COUNT(*) > 0 FROM cart_items
     *      WHERE cart_id = ? AND food_item_id = ?
     */
    boolean existsByCart_IdAndFoodItem_Id(Long cartId, Long foodItemId);

    // ----------------------------------------------------------
    // 4. Targeted Quantity Update
    // ----------------------------------------------------------

    /**
     * Directly updates the quantity of a cart item WITHOUT loading
     * the full entity.
     *
     * Used by: CartService.updateItemQuantity() — only the quantity
     * column needs to change, loading the whole entity is wasteful.
     *
     * @param id        the cart item to update
     * @param quantity  the new quantity (must be ≥ 1, enforced in service layer)
     *
     * Returns: 1 if updated, 0 if item not found.
     */
    @Modifying
    @Transactional
    @Query("UPDATE CartItem ci SET ci.quantity = :quantity WHERE ci.id = :id")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    // ----------------------------------------------------------
    // 5. Bulk Delete
    // ----------------------------------------------------------

    /**
     * Deletes all items in a cart in one batch DELETE.
     *
     * Used by: CartService.clearCart() — removes every line item.
     * Much cheaper than loading the list then calling deleteAll(items).
     *
     * @Modifying   — required for JPQL DELETE.
     * @Transactional — runs atomically.
     *
     * SQL: DELETE FROM cart_items WHERE cart_id = ?
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteByCart_Id(@Param("cartId") Long cartId);

    /**
     * Deletes all cart items referencing a specific food item,
     * across ALL carts on the platform.
     *
     * Used by: FoodItemService.deleteFoodItem() — when a menu item is
     * permanently deleted, it must be scrubbed from every active cart
     * to prevent stale references (customers would otherwise see
     * "Item no longer available" errors at checkout).
     *
     * SQL: DELETE FROM cart_items WHERE food_item_id = ?
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.foodItem.id = :foodItemId")
    int deleteByFoodItem_Id(@Param("foodItemId") Long foodItemId);

    // ----------------------------------------------------------
    // 6. Count
    // ----------------------------------------------------------

    /**
     * Returns the number of distinct items (rows) in a cart.
     *
     * Used by: Navbar cart badge — shows item count without fetching
     * all items. Note: this is distinct item types, not total quantity.
     *
     * SQL: SELECT COUNT(*) FROM cart_items WHERE cart_id = ?
     */
    long countByCart_Id(Long cartId);
}

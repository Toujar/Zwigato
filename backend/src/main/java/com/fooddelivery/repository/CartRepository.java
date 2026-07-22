package com.fooddelivery.repository;

import com.fooddelivery.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * ============================================================
 *  Repository : CartRepository
 *  Entity     : Cart
 *  Table      : carts
 * ============================================================
 *
 *  The cart is a 1:1 entity — one row per user.
 *  All operations pivot around user_id rather than cart_id
 *  because the service layer works with the currently
 *  authenticated user, not a cart ID the user types in.
 *
 *  Custom methods cover:
 *   1. Finding a user's cart (with and without items pre-loaded)
 *   2. Existence check (create-or-get pattern)
 *   3. Clearing the restaurant lock after checkout
 * ============================================================
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // ----------------------------------------------------------
    // 1. Primary Lookup — by User
    // ----------------------------------------------------------

    /**
     * Finds the cart for a given user ID.
     *
     * Used by: The most common cart operation entry-point.
     * Returns Optional because a cart might not exist yet for a
     * brand-new user who has never added anything (lazy cart creation).
     *
     * SQL: SELECT * FROM carts WHERE user_id = ?
     */
    Optional<Cart> findByUser_Id(Long userId);

    /**
     * Finds the cart AND eagerly fetches all cart items with
     * their associated food items in a SINGLE SQL query.
     *
     * Used by:
     *   - CartService.getCart()  — render the full cart view
     *   - CheckoutService        — read all items before creating an Order
     *
     * WHY JOIN FETCH?
     *   Without this, loading cart items would trigger N+1 queries:
     *   1 query for the Cart + 1 per CartItem to load FoodItem.
     *   With LEFT JOIN FETCH, everything loads in one round-trip.
     *
     * LEFT JOIN (not INNER) — returns the cart even when it is empty
     * (no cart items yet).
     *
     * JPQL:
     *   SELECT c FROM Cart c
     *   LEFT JOIN FETCH c.cartItems ci
     *   LEFT JOIN FETCH ci.foodItem
     *   WHERE c.user.id = :userId
     */
    @Query("""
        SELECT c FROM Cart c
        LEFT JOIN FETCH c.cartItems ci
        LEFT JOIN FETCH ci.foodItem
        WHERE c.user.id = :userId
        """)
    Optional<Cart> findByUser_IdWithItems(@Param("userId") Long userId);

    /**
     * Finds the cart with items AND each item's restaurant pre-loaded.
     *
     * Used by: CartService validation — needs to check if the item's
     * restaurant matches the cart's locked restaurant in one query.
     *
     * JPQL adds JOIN FETCH on the restaurant through the food item.
     */
    @Query("""
        SELECT c FROM Cart c
        LEFT JOIN FETCH c.cartItems ci
        LEFT JOIN FETCH ci.foodItem fi
        LEFT JOIN FETCH fi.restaurant
        WHERE c.user.id = :userId
        """)
    Optional<Cart> findByUser_IdWithItemsAndRestaurant(@Param("userId") Long userId);

    // ----------------------------------------------------------
    // 2. Existence Check (create-or-get pattern)
    // ----------------------------------------------------------

    /**
     * Checks whether a cart already exists for the given user.
     *
     * Used by: CartService.addItem() — on first add-to-cart, the service
     * calls existsByUser_Id() first. If false, it creates a new Cart.
     * Cheaper than findByUser_Id() since no entity is loaded.
     *
     * SQL: SELECT COUNT(*) > 0 FROM carts WHERE user_id = ?
     */
    boolean existsByUser_Id(Long userId);

    // ----------------------------------------------------------
    // 3. Targeted UPDATE — Restaurant Lock
    // ----------------------------------------------------------

    /**
     * Clears the restaurant lock on a cart by setting restaurant_id to NULL.
     *
     * Used by: CartService.clearCart() — after clearing all items,
     * the restaurant lock must also be released so the next add-to-cart
     * can lock to a different restaurant.
     *
     * More efficient than loading the Cart entity and calling setRestaurant(null).
     *
     * @Modifying   — required for JPQL UPDATE.
     * @Transactional — runs atomically.
     *
     * JPQL: UPDATE Cart c SET c.restaurant = null WHERE c.user.id = :userId
     */
    @Modifying
    @Transactional
    @Query("UPDATE Cart c SET c.restaurant = null WHERE c.user.id = :userId")
    int clearRestaurantLock(@Param("userId") Long userId);

    /**
     * Clears the restaurant lock for every cart currently locked to a
     * specific restaurant.
     *
     * Used by: When a restaurant is deactivated or permanently deleted.
     * All carts holding that restaurant's items should be unlocked
     * so customers can shop elsewhere.
     *
     * JPQL: UPDATE Cart c SET c.restaurant = null WHERE c.restaurant.id = :restaurantId
     */
    @Modifying
    @Transactional
    @Query("UPDATE Cart c SET c.restaurant = null WHERE c.restaurant.id = :restaurantId")
    int clearRestaurantLockByRestaurant(@Param("restaurantId") Long restaurantId);
}

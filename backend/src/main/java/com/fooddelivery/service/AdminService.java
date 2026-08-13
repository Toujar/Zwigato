package com.fooddelivery.service;

import com.fooddelivery.dto.response.AdminDashboardResponse;
import com.fooddelivery.dto.response.UserManagementResponse;
import com.fooddelivery.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Contract for admin operations.
 *
 * Responsibilities:
 *  - Dashboard analytics and KPIs
 *  - User management (list, activate, deactivate, role changes)
 *  - Platform-wide reporting
 *  - Top performers ranking
 */
public interface AdminService {

    // ──────────────────────────────────────────────────────────
    // Dashboard Analytics
    // ──────────────────────────────────────────────────────────

    /**
     * Get comprehensive dashboard data with KPIs, charts, and rankings.
     *
     * @param startDate optional start date for time-series data (default: 30 days ago)
     * @param endDate optional end date for time-series data (default: today)
     * @return dashboard response with all analytics
     */
    AdminDashboardResponse getDashboardData(LocalDate startDate, LocalDate endDate);

    /**
     * Get revenue breakdown by date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of revenue data points
     */
    List<AdminDashboardResponse.RevenueDataPoint> getRevenueChart(LocalDate startDate, LocalDate endDate);

    /**
     * Get order volume breakdown by date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of order volume data points
     */
    List<AdminDashboardResponse.OrderVolumeDataPoint> getOrderVolumeChart(LocalDate startDate, LocalDate endDate);

    /**
     * Get top performing restaurants by revenue or order count.
     *
     * @param limit number of results (default: 10)
     * @param sortBy "revenue" or "orders"
     * @return list of top restaurants
     */
    List<AdminDashboardResponse.TopRestaurant> getTopRestaurants(int limit, String sortBy);

    /**
     * Get top selling dishes across all restaurants.
     *
     * @param limit number of results (default: 10)
     * @return list of top dishes
     */
    List<AdminDashboardResponse.TopDish> getTopDishes(int limit);

    // ──────────────────────────────────────────────────────────
    // User Management
    // ──────────────────────────────────────────────────────────

    /**
     * Get paginated list of all users with filters.
     *
     * @param role optional role filter (CUSTOMER, RESTAURANT_OWNER, DELIVERY_AGENT, ADMIN)
     * @param isActive optional active status filter
     * @param searchQuery optional search by name or email
     * @param pageable pagination info
     * @return page of users with stats
     */
    Page<UserManagementResponse> getAllUsers(UserRole role, Boolean isActive, String searchQuery, Pageable pageable);

    /**
     * Get detailed user info by ID.
     *
     * @param userId the user ID
     * @return user details with stats
     */
    UserManagementResponse getUserDetails(Long userId);

    /**
     * Activate a user account.
     *
     * @param userId the user ID
     * @return updated user details
     */
    UserManagementResponse activateUser(Long userId);

    /**
     * Deactivate a user account (soft delete).
     *
     * @param userId the user ID
     * @return updated user details
     */
    UserManagementResponse deactivateUser(Long userId);

    /**
     * Change a user's role.
     *
     * @param userId the user ID
     * @param newRole the new role
     * @return updated user details
     */
    UserManagementResponse changeUserRole(Long userId, UserRole newRole);

    /**
     * Delete a user permanently (hard delete).
     * Use with caution - only for spam/abuse cases.
     *
     * @param userId the user ID
     */
    void deleteUserPermanently(Long userId);

    // ──────────────────────────────────────────────────────────
    // Platform Health Metrics
    // ──────────────────────────────────────────────────────────

    /**
     * Get platform health overview (active users, pending orders, etc.).
     *
     * @return KPI overview object
     */
    AdminDashboardResponse.KPIOverview getPlatformHealth();
}

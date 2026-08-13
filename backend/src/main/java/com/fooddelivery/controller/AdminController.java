package com.fooddelivery.controller;

import com.fooddelivery.dto.response.AdminDashboardResponse;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.UserManagementResponse;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin-only endpoints for platform management, analytics, and user administration.
 *
 * All endpoints require ADMIN role.
 *
 * GET    /api/admin/dashboard               → comprehensive dashboard data
 * GET    /api/admin/users                  → user management with filters
 * PUT    /api/admin/users/{id}/activate    → activate user
 * PUT    /api/admin/users/{id}/deactivate  → deactivate user
 * PUT    /api/admin/users/{id}/role        → change user role
 * DELETE /api/admin/users/{id}             → permanently delete user
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "10. Admin Dashboard")
public class AdminController {

    private final AdminService adminService;

    // ── Request DTOs ──────────────────────────────────────────
    @Data
    static class ChangeRoleRequest {
        @NotNull(message = "Role is required")
        UserRole role;
    }

    // ── GET /api/admin/dashboard ──────────────────────────────
    /**
     * Get comprehensive dashboard data with KPIs, charts, and rankings.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AdminDashboardResponse dashboard = adminService.getDashboardData(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard data retrieved successfully"));
    }

    // ── GET /api/admin/kpis ───────────────────────────────────
    /**
     * Get platform health KPIs only (lighter endpoint).
     */
    @GetMapping("/kpis")
    @Operation(summary = "Get platform KPIs")
    public ResponseEntity<ApiResponse<AdminDashboardResponse.KPIOverview>> getKPIs() {
        AdminDashboardResponse.KPIOverview kpis = adminService.getPlatformHealth();
        return ResponseEntity.ok(ApiResponse.success(kpis, "KPIs retrieved successfully"));
    }

    // ── GET /api/admin/revenue-chart ──────────────────────────
    /**
     * Get revenue chart data for a date range.
     */
    @GetMapping("/revenue-chart")
    @Operation(summary = "Get revenue chart data")
    public ResponseEntity<ApiResponse<List<AdminDashboardResponse.RevenueDataPoint>>> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AdminDashboardResponse.RevenueDataPoint> chartData = adminService.getRevenueChart(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(chartData, "Revenue chart data retrieved successfully"));
    }

    // ── GET /api/admin/order-volume-chart ─────────────────────
    /**
     * Get order volume chart data for a date range.
     */
    @GetMapping("/order-volume-chart")
    @Operation(summary = "Get order volume chart data")
    public ResponseEntity<ApiResponse<List<AdminDashboardResponse.OrderVolumeDataPoint>>> getOrderVolumeChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AdminDashboardResponse.OrderVolumeDataPoint> chartData = adminService.getOrderVolumeChart(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(chartData, "Order volume chart data retrieved successfully"));
    }

    // ── GET /api/admin/top-restaurants ────────────────────────
    /**
     * Get top performing restaurants.
     */
    @GetMapping("/top-restaurants")
    @Operation(summary = "Get top restaurants")
    public ResponseEntity<ApiResponse<List<AdminDashboardResponse.TopRestaurant>>> getTopRestaurants(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        List<AdminDashboardResponse.TopRestaurant> topRestaurants = adminService.getTopRestaurants(limit, sortBy);
        return ResponseEntity.ok(ApiResponse.success(topRestaurants, "Top restaurants retrieved successfully"));
    }

    // ── GET /api/admin/top-dishes ─────────────────────────────
    /**
     * Get top selling dishes.
     */
    @GetMapping("/top-dishes")
    @Operation(summary = "Get top dishes")
    public ResponseEntity<ApiResponse<List<AdminDashboardResponse.TopDish>>> getTopDishes(
            @RequestParam(defaultValue = "10") int limit) {
        List<AdminDashboardResponse.TopDish> topDishes = adminService.getTopDishes(limit);
        return ResponseEntity.ok(ApiResponse.success(topDishes, "Top dishes retrieved successfully"));
    }

    // ──────────────────────────────────────────────────────────
    // User Management
    // ──────────────────────────────────────────────────────────

    // ── GET /api/admin/users ──────────────────────────────────
    /**
     * Get paginated list of users with filters.
     */
    @GetMapping("/users")
    @Operation(summary = "Get users with filters")
    public ResponseEntity<ApiResponse<Page<UserManagementResponse>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserManagementResponse> users = adminService.getAllUsers(role, isActive, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    // ── GET /api/admin/users/{userId} ─────────────────────────
    /**
     * Get detailed user info.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details")
    public ResponseEntity<ApiResponse<UserManagementResponse>> getUserDetails(@PathVariable Long userId) {
        UserManagementResponse user = adminService.getUserDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully"));
    }

    // ── PUT /api/admin/users/{userId}/activate ───────────────
    /**
     * Activate a user account.
     */
    @PutMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user")
    public ResponseEntity<ApiResponse<UserManagementResponse>> activateUser(@PathVariable Long userId) {
        UserManagementResponse user = adminService.activateUser(userId);
        log.info("Admin activated user {}", userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User activated successfully"));
    }

    // ── PUT /api/admin/users/{userId}/deactivate ─────────────
    /**
     * Deactivate a user account.
     */
    @PutMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<ApiResponse<UserManagementResponse>> deactivateUser(@PathVariable Long userId) {
        UserManagementResponse user = adminService.deactivateUser(userId);
        log.info("Admin deactivated user {}", userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User deactivated successfully"));
    }

    // ── PUT /api/admin/users/{userId}/role ───────────────────
    /**
     * Change a user's role.
     */
    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<UserManagementResponse>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody ChangeRoleRequest req) {
        UserManagementResponse user = adminService.changeUserRole(userId, req.role);
        log.info("Admin changed user {} role to {}", userId, req.role);
        return ResponseEntity.ok(ApiResponse.success(user, "User role changed successfully"));
    }

    // ── DELETE /api/admin/users/{userId} ──────────────────────
    /**
     * Permanently delete a user (use with extreme caution).
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Permanently delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUserPermanently(userId);
        log.warn("Admin permanently deleted user {}", userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted permanently"));
    }
}
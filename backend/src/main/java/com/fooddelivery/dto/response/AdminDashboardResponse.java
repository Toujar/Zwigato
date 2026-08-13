package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive response for admin dashboard view.
 * Includes KPIs, charts, and analytics data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // ── Overview KPIs ──────────────────────────────────────────
    private KPIOverview overview;

    // ── Time-series Data for Charts ────────────────────────────
    private List<RevenueDataPoint> revenueChart;
    private List<OrderVolumeDataPoint> orderVolumeChart;

    // ── Rankings ───────────────────────────────────────────────
    private List<TopRestaurant> topRestaurants;
    private List<TopDish> topDishes;

    // ── User Growth ────────────────────────────────────────────
    private List<UserGrowthDataPoint> userGrowth;

    // ── Recent Activity ────────────────────────────────────────
    private List<RecentOrder> recentOrders;

    // ══════════════════════════════════════════════════════════
    // Nested DTOs
    // ══════════════════════════════════════════════════════════

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPIOverview {
        // Revenue metrics
        private BigDecimal totalRevenue;
        private BigDecimal todayRevenue;
        private BigDecimal monthRevenue;
        private Double revenueGrowthPercent; // vs. last month

        // Order metrics
        private Long totalOrders;
        private Long todayOrders;
        private Long monthOrders;
        private Double orderGrowthPercent; // vs. last month

        // User metrics
        private Long totalUsers;
        private Long totalCustomers;
        private Long totalRestaurants;
        private Long totalAgents;
        private Long newUsersThisMonth;

        // Platform health
        private Long activeRestaurants;
        private Long pendingOrders;
        private BigDecimal averageOrderValue;
        private Double platformCommissionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueDataPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderVolumeDataPoint {
        private LocalDate date;
        private Long totalOrders;
        private Long deliveredOrders;
        private Long cancelledOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRestaurant {
        private Long restaurantId;
        private String name;
        private String imageUrl;
        private BigDecimal revenue;
        private Long orderCount;
        private BigDecimal rating;
        private Integer reviewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDish {
        private Long foodItemId;
        private String name;
        private String restaurantName;
        private String imageUrl;
        private Long orderCount;
        private BigDecimal revenue;
        private BigDecimal rating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthDataPoint {
        private LocalDate date;
        private Long newCustomers;
        private Long newRestaurants;
        private Long newAgents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        private Long orderId;
        private String customerName;
        private String restaurantName;
        private BigDecimal totalAmount;
        private String status;
        private String placedAt;
    }
}

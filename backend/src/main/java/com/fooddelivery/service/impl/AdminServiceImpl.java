package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.AdminDashboardResponse;
import com.fooddelivery.dto.response.UserManagementResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin operations for dashboard, analytics, and user management.
 *
 * Provides comprehensive platform insights:
 *  - Revenue and order volume metrics
 *  - Top performers (restaurants, dishes)
 *  - User growth tracking
 *  - User account management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final PaymentRepository paymentRepository;

    // ──────────────────────────────────────────────────────────
    // Dashboard Analytics
    // ──────────────────────────────────────────────────────────

    @Override
    public AdminDashboardResponse getDashboardData(LocalDate startDate, LocalDate endDate) {
        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        return AdminDashboardResponse.builder()
            .overview(getPlatformHealth())
            .revenueChart(getRevenueChart(startDate, endDate))
            .orderVolumeChart(getOrderVolumeChart(startDate, endDate))
            .topRestaurants(getTopRestaurants(10, "revenue"))
            .topDishes(getTopDishes(10))
            .userGrowth(getUserGrowthChart(startDate, endDate))
            .recentOrders(getRecentOrders(10))
            .build();
    }

    @Override
    public AdminDashboardResponse.KPIOverview getPlatformHealth() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);

        // Revenue metrics
        BigDecimal totalRevenue = calculateTotalRevenue(null, null);
        BigDecimal todayRevenue = calculateTotalRevenue(today, today);
        BigDecimal monthRevenue = calculateTotalRevenue(monthStart, today);
        BigDecimal lastMonthRevenue = calculateTotalRevenue(lastMonthStart, lastMonthEnd);

        Double revenueGrowth = calculateGrowthPercent(monthRevenue, lastMonthRevenue);

        // Order metrics
        Long totalOrders = orderRepository.count();
        Long todayOrders = orderRepository.countByPlacedAtBetween(
            today.atStartOfDay(), today.plusDays(1).atStartOfDay()
        );
        Long monthOrders = orderRepository.countByPlacedAtBetween(
            monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay()
        );
        Long lastMonthOrders = orderRepository.countByPlacedAtBetween(
            lastMonthStart.atStartOfDay(), lastMonthEnd.plusDays(1).atStartOfDay()
        );

        Double orderGrowth = calculateGrowthPercent(
            BigDecimal.valueOf(monthOrders),
            BigDecimal.valueOf(lastMonthOrders)
        );

        // User metrics
        Long totalUsers = userRepository.count();
        Long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        Long totalRestaurants = restaurantRepository.count();
        Long totalAgents = userRepository.countByRole(UserRole.DELIVERY_AGENT);
        Long newUsersThisMonth = userRepository.countByCreatedAtBetween(
            monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay()
        );

        // Platform health
        Long activeRestaurants = restaurantRepository.countByIsActiveAndIsOpen(true, true);
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.PLACED)
            + orderRepository.countByStatus(OrderStatus.CONFIRMED);

        BigDecimal averageOrderValue = totalOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return AdminDashboardResponse.KPIOverview.builder()
            .totalRevenue(totalRevenue)
            .todayRevenue(todayRevenue)
            .monthRevenue(monthRevenue)
            .revenueGrowthPercent(revenueGrowth)
            .totalOrders(totalOrders)
            .todayOrders(todayOrders)
            .monthOrders(monthOrders)
            .orderGrowthPercent(orderGrowth)
            .totalUsers(totalUsers)
            .totalCustomers(totalCustomers)
            .totalRestaurants(totalRestaurants)
            .totalAgents(totalAgents)
            .newUsersThisMonth(newUsersThisMonth)
            .activeRestaurants(activeRestaurants)
            .pendingOrders(pendingOrders)
            .averageOrderValue(averageOrderValue)
            .platformCommissionRate(10.0) // Configurable commission rate
            .build();
    }

    @Override
    public List<AdminDashboardResponse.RevenueDataPoint> getRevenueChart(LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardResponse.RevenueDataPoint> dataPoints = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal revenue = calculateTotalRevenue(date, date);
            Long orderCount = orderRepository.countByPlacedAtBetween(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
            );

            dataPoints.add(AdminDashboardResponse.RevenueDataPoint.builder()
                .date(date)
                .revenue(revenue)
                .orderCount(orderCount)
                .build());
        }

        return dataPoints;
    }

    @Override
    public List<AdminDashboardResponse.OrderVolumeDataPoint> getOrderVolumeChart(LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardResponse.OrderVolumeDataPoint> dataPoints = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            Long totalOrders = orderRepository.countByPlacedAtBetween(dayStart, dayEnd);
            Long deliveredOrders = orderRepository.countByStatusAndPlacedAtBetween(
                OrderStatus.DELIVERED, dayStart, dayEnd
            );
            Long cancelledOrders = orderRepository.countByStatusAndPlacedAtBetween(
                OrderStatus.CANCELLED, dayStart, dayEnd
            );

            dataPoints.add(AdminDashboardResponse.OrderVolumeDataPoint.builder()
                .date(date)
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .build());
        }

        return dataPoints;
    }

    @Override
    public List<AdminDashboardResponse.TopRestaurant> getTopRestaurants(int limit, String sortBy) {
        // Get all delivered orders grouped by restaurant
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);

        // Group by restaurant and calculate metrics
        return deliveredOrders.stream()
            .collect(Collectors.groupingBy(Order::getRestaurant))
            .entrySet().stream()
            .map(entry -> {
                var restaurant = entry.getKey();
                var orders = entry.getValue();

                BigDecimal revenue = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return AdminDashboardResponse.TopRestaurant.builder()
                    .restaurantId(restaurant.getId())
                    .name(restaurant.getName())
                    .imageUrl(restaurant.getImageUrl())
                    .revenue(revenue)
                    .orderCount((long) orders.size())
                    .rating(restaurant.getRating())
                    .reviewCount(restaurant.getReviewCount())
                    .build();
            })
            .sorted((a, b) -> {
                if ("orders".equalsIgnoreCase(sortBy)) {
                    return b.getOrderCount().compareTo(a.getOrderCount());
                }
                return b.getRevenue().compareTo(a.getRevenue()); // default: revenue
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<AdminDashboardResponse.TopDish> getTopDishes(int limit) {
        // This would ideally be a custom query, but for simplicity we'll aggregate in code
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);

        // Flatten all order items and group by food item
        return deliveredOrders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .collect(Collectors.groupingBy(
                orderItem -> orderItem.getFoodItem(),
                Collectors.summingLong(orderItem -> orderItem.getQuantity())
            ))
            .entrySet().stream()
            .map(entry -> {
                var foodItem = entry.getKey();
                Long orderCount = entry.getValue();

                BigDecimal revenue = deliveredOrders.stream()
                    .flatMap(order -> order.getOrderItems().stream())
                    .filter(oi -> oi.getFoodItem().getId().equals(foodItem.getId()))
                    .map(oi -> oi.getSubtotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return AdminDashboardResponse.TopDish.builder()
                    .foodItemId(foodItem.getId())
                    .name(foodItem.getName())
                    .restaurantName(foodItem.getRestaurant().getName())
                    .imageUrl(foodItem.getImageUrl())
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .rating(foodItem.getRating())
                    .build();
            })
            .sorted((a, b) -> b.getOrderCount().compareTo(a.getOrderCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────
    // User Management
    // ──────────────────────────────────────────────────────────

    @Override
    public Page<UserManagementResponse> getAllUsers(UserRole role, Boolean isActive, String searchQuery, Pageable pageable) {
        Page<User> users;

        if (role != null && isActive != null && searchQuery != null) {
            users = userRepository.findByRoleAndIsActiveAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                role, isActive, searchQuery, searchQuery, pageable
            );
        } else if (role != null && isActive != null) {
            users = userRepository.findByRoleAndIsActive(role, isActive, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive, pageable);
        } else if (searchQuery != null && !searchQuery.isEmpty()) {
            users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchQuery, searchQuery, pageable
            );
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::toUserManagementResponse);
    }

    @Override
    public UserManagementResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        return toUserManagementResponse(user);
    }

    @Override
    @Transactional
    public UserManagementResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        user.setIsActive(true);
        User saved = userRepository.save(user);
        log.info("User {} activated", userId);
        return toUserManagementResponse(saved);
    }

    @Override
    @Transactional
    public UserManagementResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        user.setIsActive(false);
        User saved = userRepository.save(user);
        log.info("User {} deactivated", userId);
        return toUserManagementResponse(saved);
    }

    @Override
    @Transactional
    public UserManagementResponse changeUserRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        user.setRole(newRole);
        User saved = userRepository.save(user);
        log.info("User {} role changed to {}", userId, newRole);
        return toUserManagementResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUserPermanently(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        userRepository.delete(user);
        log.warn("User {} permanently deleted", userId);
    }

    // ──────────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────────

    private BigDecimal calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
        List<Order> orders;

        if (startDate != null && endDate != null) {
            orders = orderRepository.findByStatusAndPlacedAtBetween(
                OrderStatus.DELIVERED,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
            );
        } else {
            orders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        }

        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateGrowthPercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    private List<AdminDashboardResponse.UserGrowthDataPoint> getUserGrowthChart(LocalDate startDate, LocalDate endDate) {
        List<AdminDashboardResponse.UserGrowthDataPoint> dataPoints = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            Long newCustomers = userRepository.countByRoleAndCreatedAtBetween(
                UserRole.CUSTOMER, dayStart, dayEnd
            );
            Long newRestaurants = userRepository.countByRoleAndCreatedAtBetween(
                UserRole.RESTAURANT_OWNER, dayStart, dayEnd
            );
            Long newAgents = userRepository.countByRoleAndCreatedAtBetween(
                UserRole.DELIVERY_AGENT, dayStart, dayEnd
            );

            dataPoints.add(AdminDashboardResponse.UserGrowthDataPoint.builder()
                .date(date)
                .newCustomers(newCustomers)
                .newRestaurants(newRestaurants)
                .newAgents(newAgents)
                .build());
        }

        return dataPoints;
    }

    private List<AdminDashboardResponse.RecentOrder> getRecentOrders(int limit) {
        List<Order> orders = orderRepository.findTop10ByOrderByPlacedAtDesc();

        return orders.stream()
            .limit(limit)
            .map(order -> AdminDashboardResponse.RecentOrder.builder()
                .orderId(order.getId())
                .customerName(order.getUser().getName())
                .restaurantName(order.getRestaurant().getName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .placedAt(order.getPlacedAt().toString())
                .build())
            .collect(Collectors.toList());
    }

    private UserManagementResponse toUserManagementResponse(User user) {
        // Calculate user-specific stats based on role
        Long totalOrders = 0L;
        Long totalRestaurants = 0L;
        Long totalDeliveries = 0L;

        if (user.getRole() == UserRole.CUSTOMER) {
            totalOrders = orderRepository.countByUserId(user.getId());
        } else if (user.getRole() == UserRole.RESTAURANT_OWNER) {
            totalRestaurants = restaurantRepository.countByOwnerId(user.getId());
        } else if (user.getRole() == UserRole.DELIVERY_AGENT) {
            totalDeliveries = orderRepository.countByDeliveryAgentId(user.getId());
        }

        return UserManagementResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .address(user.getAddress())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
            .totalOrders(totalOrders)
            .totalRestaurants(totalRestaurants)
            .totalDeliveries(totalDeliveries)
            .build();
    }
}

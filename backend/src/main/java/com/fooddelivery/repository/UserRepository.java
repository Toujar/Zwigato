package com.fooddelivery.repository;

import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : UserRepository
 *  Entity     : User
 *  Table      : users
 * ============================================================
 *
 *  Extends JpaRepository which already provides:
 *   save(), findById(), findAll(), deleteById(), count(), etc.
 *
 *  Custom methods below cover:
 *   1. Login / authentication lookups
 *   2. Duplicate-check guards before registration
 *   3. Admin user-management queries
 *   4. Soft-delete activation toggle
 * ============================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ----------------------------------------------------------
    // 1. Authentication Lookups
    // ----------------------------------------------------------

    /**
     * Finds a user by their email address.
     *
     * Used by: Spring Security's UserDetailsService to load a user
     * during authentication (login).
     *
     * Returns Optional — email may not match any user (wrong credentials).
     * The service layer should throw UsernameNotFoundException if empty.
     *
     * SQL: SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their phone number.
     *
     * Used by: OTP-based login flows or phone-number lookup APIs.
     * Also useful when a support agent needs to look up a user
     * by the number they called from.
     *
     * SQL: SELECT * FROM users WHERE phone = ?
     */
    Optional<User> findByPhone(String phone);

    // ----------------------------------------------------------
    // 2. Duplicate-Check Guards (used before INSERT)
    // ----------------------------------------------------------

    /**
     * Checks whether an email address is already registered.
     *
     * Used by: AuthService.register() BEFORE creating the new user.
     * Cheaper than findByEmail() — returns a boolean, no User object loaded.
     * Prevents duplicate account registration.
     *
     * SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether a phone number is already registered.
     *
     * Used by: AuthService.register() to enforce the unique phone constraint
     * at the application level before hitting the DB unique index.
     *
     * SQL: SELECT COUNT(*) > 0 FROM users WHERE phone = ?
     */
    boolean existsByPhone(String phone);

    // ----------------------------------------------------------
    // 3. Role-Based Queries (Admin / management)
    // ----------------------------------------------------------

    /**
     * Returns all users that hold the given role.
     *
     * Used by: Admin dashboard to list all CUSTOMERS, RESTAURANT_OWNERs,
     * DELIVERY_AGENTs, or ADMINs.
     * Also used by the order-assignment service to find available
     * DELIVERY_AGENTs.
     *
     * SQL: SELECT * FROM users WHERE role = ?
     */
    List<User> findByRole(UserRole role);

    /**
     * Returns a paginated list of users with the given role.
     *
     * Used by: Admin dashboard with server-side pagination to avoid
     * loading all users of a role into memory at once.
     *
     * Example: findByRole(DELIVERY_AGENT, PageRequest.of(0, 20))
     *
     * SQL: SELECT * FROM users WHERE role = ? LIMIT ? OFFSET ?
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * Returns all users matching the given active/inactive status.
     *
     * Used by: Admin dashboard to list active vs. deactivated accounts.
     * findByIsActive(true)  → active users
     * findByIsActive(false) → soft-deleted / suspended users
     *
     * SQL: SELECT * FROM users WHERE is_active = ?
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * Returns a paginated list of all users, regardless of status.
     *
     * Used by: Admin user management table with pagination + sorting.
     *
     * SQL: SELECT * FROM users LIMIT ? OFFSET ?
     */
    Page<User> findAll(Pageable pageable);

    // ----------------------------------------------------------
    // 4. Search
    // ----------------------------------------------------------

    /**
     * Full case-insensitive search across name, email, and phone.
     *
     * Used by: Admin search bar — single keyword searched against
     * all three identifying fields so admins don't need to know
     * which field the value is in.
     *
     * @param keyword  the search term (partial match supported)
     * @param pageable pagination and sort parameters
     *
     * JPQL: WHERE LOWER(name) LIKE %keyword%
     *          OR LOWER(email) LIKE %keyword%
     *          OR phone LIKE %keyword%
     */
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR u.phone        LIKE CONCAT('%', :keyword, '%')
        """)
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ----------------------------------------------------------
    // 5. Soft-Delete Toggle (Admin action)
    // ----------------------------------------------------------

    /**
     * Toggles the is_active flag for a single user without loading
     * the full entity into memory.
     *
     * Used by: Admin "Activate / Deactivate" action.
     * More efficient than load → set → save for a single boolean column.
     *
     * @Modifying   — required for UPDATE/DELETE JPQL queries.
     * @Transactional — each write operation must run inside a transaction.
     *
     * JPQL: UPDATE users SET is_active = :isActive WHERE id = :id
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.id = :id")
    int updateIsActive(@Param("id") Long id, @Param("isActive") Boolean isActive);

    // ----------------------------------------------------------
    // 6. Date-Range Queries (Reporting)
    // ----------------------------------------------------------

    /**
     * Returns all users registered within a specific date range.
     *
     * Used by: Admin reporting dashboard — "new signups this week / month".
     * Both bounds are inclusive.
     *
     * @param from  start of date range (inclusive)
     * @param to    end of date range (inclusive)
     *
     * JPQL: WHERE created_at BETWEEN :from AND :to
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :from AND :to ORDER BY u.createdAt DESC")
    List<User> findUsersRegisteredBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Counts the total number of active users (quick dashboard stat).
     *
     * SQL: SELECT COUNT(*) FROM users WHERE is_active = true
     */
    long countByIsActiveTrue();

    /**
     * Counts the total number of users with a specific role.
     *
     * Used by: Admin dashboard stat cards — "Total customers: 1,240"
     *
     * SQL: SELECT COUNT(*) FROM users WHERE role = ?
     */
    long countByRole(UserRole role);

    // ----------------------------------------------------------
    // 7. Admin Dashboard Queries
    // ----------------------------------------------------------

    /**
     * Find users by role and active status (paginated).
     */
    Page<User> findByRoleAndIsActive(UserRole role, Boolean isActive, Pageable pageable);

    /**
     * Search users by name or email (paginated).
     */
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String name, String email, Pageable pageable
    );

    /**
     * Complex filter: role + active + search.
     */
    Page<User> findByRoleAndIsActiveAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        UserRole role, Boolean isActive, String name, String email, Pageable pageable
    );

    /**
     * Find users by active status only (paginated).
     */
    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Count users created within date range.
     */
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count users by role created within date range.
     */
    Long countByRoleAndCreatedAtBetween(UserRole role, LocalDateTime start, LocalDateTime end);
}

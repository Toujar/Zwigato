package com.fooddelivery.entity;

import com.fooddelivery.entity.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  Entity  : User
 *  Table   : users
 * ============================================================
 *
 *  Central identity for every actor in the system.
 *  A single User row can be a CUSTOMER, RESTAURANT_OWNER,
 *  DELIVERY_AGENT, or ADMIN — controlled by the `role` field.
 *
 *  Relationships (all lazy to avoid N+1 problems):
 *   - One User OWNS many Restaurants  (Restaurant.owner)
 *   - One User HAS one Cart           (Cart.user)
 *   - One User PLACES many Orders     (Order.user)
 *   - One User DELIVERS many Orders   (Order.deliveryAgent)
 *
 *  Auditing:
 *   - createdAt is set once on INSERT  (updatable = false)
 *   - updatedAt is refreshed on every UPDATE
 *   Both are managed by Spring Data JPA's @EnableJpaAuditing.
 * ============================================================
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
    },
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_role",  columnList = "role")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})          // Never print password in logs
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    // ----------------------------------------------------------
    // Primary Key
    // ----------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ----------------------------------------------------------
    // Core Fields
    // ----------------------------------------------------------

    /** Full display name of the user. */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Login identifier — must be globally unique.
     * Used as the Spring Security username.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * BCrypt-hashed password — NEVER stored or returned in plain text.
     * Excluded from @ToString to prevent leaking into logs.
     */
    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Mobile number — unique per user.
     * Accepts formats: 9876543210  |  +919876543210
     */
    @NotBlank(message = "Phone is required")
    @Pattern(
        regexp  = "^[+]?[0-9]{10,15}$",
        message = "Phone must be 10–15 digits, optionally prefixed with +"
    )
    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    /** Optional delivery / billing address stored as free text. */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // ----------------------------------------------------------
    // Role
    // ----------------------------------------------------------

    /**
     * Determines what the user can do in the system.
     * Stored as a VARCHAR string (EnumType.STRING) so the column
     * remains readable in MySQL without needing enum introspection.
     */
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "role",
        nullable         = false,
        length           = 20,
        columnDefinition = "VARCHAR(20) DEFAULT 'CUSTOMER'"
    )
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    // ----------------------------------------------------------
    // Status
    // ----------------------------------------------------------

    /**
     * Soft-delete flag.
     * Set to false to deactivate the account without losing data.
     * Hard deletes are avoided to preserve FK integrity on Orders.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ----------------------------------------------------------
    // Auditing — managed by Spring Data JPA
    // ----------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    /** Returns true if this user holds the ADMIN role. */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /** Returns true if this user holds the RESTAURANT_OWNER role. */
    public boolean isRestaurantOwner() {
        return UserRole.RESTAURANT_OWNER.equals(this.role);
    }

    /** Returns true if this user holds the DELIVERY_AGENT role. */
    public boolean isDeliveryAgent() {
        return UserRole.DELIVERY_AGENT.equals(this.role);
    }
}

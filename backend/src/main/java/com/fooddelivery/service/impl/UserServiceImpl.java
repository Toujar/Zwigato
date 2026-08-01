package com.fooddelivery.service.impl;

import com.fooddelivery.config.CacheConstants;
import com.fooddelivery.dto.request.UpdateProfileRequest;
import com.fooddelivery.dto.response.UserResponse;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.UserService;
import com.fooddelivery.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================
 *  UserServiceImpl — with Redis caching
 * ============================================================
 *
 *  Cache design for user profiles:
 *
 *  Cache name : CacheConstants.USER_PROFILES  ("userProfiles")
 *  TTL        : 15 minutes (CacheConstants.TTL_USER_PROFILES)
 *  Key space  : user ID (Long)
 *
 *  Key: zwigato:userProfiles::<userId>
 *
 *  Operations:
 *    getCurrentUser()   → @Cacheable by #root.target.securityUtils.getCurrentUser().id
 *                         We resolve the key from the returned object (#result.id)
 *                         — but @Cacheable needs the key BEFORE the DB call.
 *                         Solution: use a fixed SpEL expression that calls
 *                         the security principal. Simpler: delegate to
 *                         getUserById(currentUser.getId()) so caching is in
 *                         one place (getUserById).
 *
 *    getUserById()      → @Cacheable key = #id
 *
 *    updateProfile()    → @CachePut  key = #result.id
 *                         Refreshes the cache entry after the DB write.
 *
 *    deleteUser()       → @CacheEvict key = #id
 *                         Removes the stale entry for the deactivated user.
 *
 *  Note on getCurrentUser():
 *  ─────────────────────────
 *  getCurrentUser() fetches the authenticated user's own profile.
 *  Rather than duplicating the DB call and cache logic, it delegates
 *  to getUserById(). This keeps caching centralised in getUserById()
 *  and avoids a second @Cacheable with a different key pattern.
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SecurityUtils  securityUtils;

    // ---------------------------------------------------------------
    // getCurrentUser — delegates to getUserById for cache reuse
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        // Load the user entity from the security context (no DB hit — SecurityUtils
        // uses the JPA session), then delegate to the cached getUserById().
        User user = securityUtils.getCurrentUser();
        return getUserById(user.getId());
    }

    // ---------------------------------------------------------------
    // getUserById
    // ---------------------------------------------------------------

    /**
     * Cache the UserResponse under the user's ID.
     * Key: zwigato:userProfiles::<id>
     * TTL: 15 minutes.
     *
     * On a cache hit the DB is not touched — profile reads are very
     * frequent (every authenticated page load) so this delivers
     * meaningful latency savings.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.USER_PROFILES, key = "#id")
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toUserResponse(user);
    }

    // ---------------------------------------------------------------
    // updateProfile
    // ---------------------------------------------------------------

    /**
     * After a successful profile update:
     *   @CachePut writes the fresh UserResponse back into the cache
     *   under the updated user's ID so subsequent reads see the new
     *   name/phone/address without waiting for TTL expiry.
     *
     * key = "#result.id"  — Spring evaluates #result after the method
     * returns, so we get the ID from the saved UserResponse object.
     */
    @Override
    @Transactional
    @CachePut(value = CacheConstants.USER_PROFILES, key = "#result.id")
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();

        // Phone uniqueness guard — only if the phone is actually changing
        if (request.getPhone() != null
                && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException(
                    "Phone number is already in use by another account");
        }

        // Partial update — only apply fields that were sent
        if (request.getName()    != null) user.setName(request.getName());
        if (request.getPhone()   != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());

        User updated = userRepository.save(user);
        log.info("Profile updated for user: {}", updated.getEmail());
        return toUserResponse(updated);
    }

    // ---------------------------------------------------------------
    // deleteUser (ADMIN only — enforced at controller level)
    // ---------------------------------------------------------------

    /**
     * Soft-delete: sets is_active = false.
     * @CacheEvict removes the cached profile so subsequent lookups
     * reload from DB and see is_active = false.
     *
     * key = "#id" — the ID is available as a method parameter,
     * so no #result reference is needed.
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.USER_PROFILES, key = "#id")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getIsActive()) {
            throw new BadRequestException("User account is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);
        log.info("User account deactivated: {} ({})", user.getEmail(), id);
    }

    // ---------------------------------------------------------------
    // Mapper
    // ---------------------------------------------------------------

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

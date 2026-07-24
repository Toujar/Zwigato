package com.fooddelivery.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages user profile retrieval, updates, and soft-deletion.
 *
 * Key decisions:
 *  - updateProfile() only allows changing name, phone, and address.
 *    Email/password changes are security-sensitive and need their
 *    own endpoints with password-confirmation or OTP verification.
 *  - deleteUser() is a soft-delete — sets is_active = false.
 *    Hard deletion would orphan historical orders.
 *  - All writes are @Transactional; reads use readOnly = true
 *    to avoid unnecessary flush operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SecurityUtils  securityUtils;

    // ---------------------------------------------------------------
    // getCurrentUser
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return toUserResponse(user);
    }

    // ---------------------------------------------------------------
    // getUserById  (ADMIN only — enforced at controller level)
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toUserResponse(user);
    }

    // ---------------------------------------------------------------
    // updateProfile
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();

        // Phone uniqueness guard — only if the phone is actually changing
        if (request.getPhone() != null
                && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number is already in use by another account");
        }

        // Apply only the fields that were sent (partial update)
        if (request.getName()    != null) user.setName(request.getName());
        if (request.getPhone()   != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());

        User updated = userRepository.save(user);
        log.info("Profile updated for user: {}", updated.getEmail());
        return toUserResponse(updated);
    }

    // ---------------------------------------------------------------
    // deleteUser  (ADMIN only — enforced at controller level)
    // ---------------------------------------------------------------

    @Override
    @Transactional
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

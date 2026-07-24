package com.fooddelivery.service;

import com.fooddelivery.dto.request.UpdateProfileRequest;
import com.fooddelivery.dto.response.UserResponse;

/**
 * Contract for user profile operations.
 */
public interface UserService {

    /** Returns the profile of the currently authenticated user. */
    UserResponse getCurrentUser();

    /** Returns any user by ID — ADMIN only. */
    UserResponse getUserById(Long id);

    /** Updates the authenticated user's own profile fields. */
    UserResponse updateProfile(UpdateProfileRequest request);

    /** Soft-deletes a user account — ADMIN only. */
    void deleteUser(Long id);
}

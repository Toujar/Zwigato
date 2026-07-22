package com.fooddelivery.service;

import com.fooddelivery.dto.response.UserResponse;

/**
 * Contract for user profile operations.
 */
public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(Long id);

    void deleteUser(Long id);
}

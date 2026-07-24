package com.fooddelivery.util;

import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Shared utility for resolving the currently authenticated User entity.
 *
 * Every service that needs the logged-in user injects this bean
 * instead of copy-pasting the SecurityContextHolder lookup.
 *
 * Why a @Component and not a static method?
 * — Static methods cannot be easily mocked in unit tests.
 * — Spring-managed bean lets us swap the implementation in tests.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the full User entity for the currently authenticated principal.
     *
     * Reads the email (username) from the Spring Security context,
     * then loads the User row from the database.
     *
     * @throws ResourceNotFoundException if the authenticated email
     *         does not match any user (should never happen in normal flow)
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Returns only the email string of the current principal.
     * Use when you need the email but do not need the full User entity.
     */
    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Returns true if the current user holds the ADMIN role.
     * Checks the Spring Security authority string — does not hit the DB.
     */
    public boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

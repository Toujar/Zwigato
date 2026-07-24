package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.LoginRequest;
import com.fooddelivery.dto.request.RegisterRequest;
import com.fooddelivery.dto.response.AuthResponse;
import com.fooddelivery.dto.response.UserResponse;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.JwtTokenProvider;
import com.fooddelivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration, login, and JWT token refresh.
 *
 * Key decisions:
 *  - register() saves the user first, then authenticates to generate tokens
 *    in one round-trip (avoids a separate login call after registration).
 *  - login() delegates credential validation to Spring Security's
 *    AuthenticationManager so password checking and account-status
 *    checks (disabled, locked) are handled uniformly.
 *  - refreshToken() validates the JWT signature, loads the user from DB
 *    to confirm they are still active, then generates a fresh token pair
 *    using the UserDetails authorities (not a null-authority Authentication).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserDetailsService    userDetailsService;   // CustomUserDetailsService

    // ---------------------------------------------------------------
    // Register
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Duplicate guards — fail fast before any DB write
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number is already registered");
        }

        // Persist the new user with a hashed password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} ({})", savedUser.getEmail(), savedUser.getId());

        // Authenticate immediately so we can generate tokens
        Authentication authentication = authenticateUser(request.getEmail(), request.getPassword());
        return buildAuthResponse(authentication, savedUser);
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticateUser(request.getEmail(), request.getPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(authentication, user);
    }

    // ---------------------------------------------------------------
    // Refresh Token
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {

        // 1. Validate the token signature and expiry
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token is invalid or has expired");
        }

        // 2. Extract email and load user from DB (confirms account is still active)
        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User account not found"));

        if (!user.getIsActive()) {
            throw new BadRequestException("User account has been deactivated");
        }

        // 3. Load full UserDetails so the new token contains the correct authorities
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        log.info("Token refreshed for user: {}", email);
        return buildAuthResponse(authentication, user);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Delegates credential validation to Spring Security's AuthenticationManager.
     * Translates Spring Security exceptions to application exceptions for
     * consistent JSON error responses via GlobalExceptionHandler.
     */
    private Authentication authenticateUser(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        } catch (DisabledException ex) {
            throw new BadRequestException("User account is deactivated");
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid email or password");
        }
    }

    /**
     * Builds the AuthResponse with both tokens and the user profile.
     */
    private AuthResponse buildAuthResponse(Authentication authentication, User user) {
        String accessToken  = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

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

package com.fooddelivery.config;

import org.springframework.context.annotation.Configuration;

/**
 * General application-level beans.
 * PasswordEncoder is defined in SecurityConfig to keep all
 * security-related beans in one place.
 */
@Configuration
public class AppConfig {
    // SecurityConfig owns PasswordEncoder and AuthenticationManager.
    // Add any non-security application beans here as the project grows.
}

package com.fooddelivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General application-level beans.
 * Security-related beans (AuthenticationManager, SecurityFilterChain)
 * will be configured in SecurityConfig once JWT auth is added.
 */
@Configuration
public class AppConfig {

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Injected wherever passwords need to be hashed or verified.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

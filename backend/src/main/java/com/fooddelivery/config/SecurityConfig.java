package com.fooddelivery.config;

import com.fooddelivery.security.JwtAuthEntryPoint;
import com.fooddelivery.security.JwtAuthenticationFilter;
import com.fooddelivery.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================
 *  Spring Security Configuration
 * ============================================================
 *
 *  Access matrix — what is permitted at the filter-chain level:
 *
 *  PUBLIC (no JWT required)
 *  ─────────────────────────────────────────────────────────
 *   POST   /api/auth/register
 *   POST   /api/auth/login
 *   POST   /api/auth/refresh-token
 *   GET    /api/restaurants          (browse restaurants)
 *   GET    /api/restaurants/{id}     (restaurant detail)
 *   GET    /api/categories           (category filter chips)
 *   GET    /api/categories/{id}      (single category)
 *   GET    /api/food-items/restaurant/{id}  (public menu)
 *   GET    /api/food-items/{id}      (single item detail)
 *   GET    /v3/api-docs/**           (OpenAPI spec)
 *   GET    /swagger-ui/**            (Swagger UI)
 *   GET    /swagger-ui.html
 *
 *  AUTHENTICATED (valid JWT required — role refined by @PreAuthorize)
 *  ─────────────────────────────────────────────────────────
 *   Everything else
 *
 *  Role-specific access is enforced at the method level via
 *  @PreAuthorize in the controllers (enabled by @EnableMethodSecurity).
 *
 *  Why split between filter-chain and @PreAuthorize?
 *   - Filter-chain handles coarse authentication (is there a valid JWT?).
 *   - @PreAuthorize handles fine role authorization (does the JWT holder
 *     have the right role for this endpoint?).
 *   - This two-layer approach keeps SecurityConfig readable and puts
 *     role logic where it is most visible: next to the endpoint itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthEntryPoint          jwtAuthEntryPoint;
    private final JwtAuthenticationFilter    jwtAuthenticationFilter;
    private final CustomUserDetailsService   customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Stateless REST API — disable CSRF and sessions ──────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 401 handler — returns JSON instead of redirect ───────────
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint))

            // ── Route-level access rules ─────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ---------- Swagger / OpenAPI (fully public) ----------
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // ---------- Auth endpoints (no token needed) ----------
                .requestMatchers("/api/auth/**").permitAll()

                // ---------- Public read — Restaurants -----------------
                // Customers (and anonymous visitors) must be able to
                // browse restaurants without logging in (like Swiggy).
                .requestMatchers(HttpMethod.GET, "/api/restaurants").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/restaurants/{id}").permitAll()

                // ---------- Public read — Categories ------------------
                // Category chips on the home page are shown to everyone.
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/{id}").permitAll()

                // ---------- Public read — Food Items (Menu) -----------
                // Restaurant menu page loads without login.
                .requestMatchers(HttpMethod.GET,
                    "/api/food-items/restaurant/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/food-items/{id}").permitAll()

                // ---------- Everything else — require authentication ---
                .anyRequest().authenticated()
            )

            // ── JWT filter runs before Spring's UsernamePasswordFilter ───
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

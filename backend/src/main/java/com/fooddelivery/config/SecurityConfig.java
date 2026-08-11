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
 *  NOTE: Paths here are relative to the servlet context (/api),
 *  so Spring Security sees them WITHOUT the /api prefix.
 *
 *   POST   /auth/register
 *   POST   /auth/login
 *   POST   /auth/refresh-token
 *   GET    /restaurants          (browse restaurants)
 *   GET    /restaurants/{id}     (restaurant detail)
 *   GET    /categories           (category filter chips)
 *   GET    /categories/{id}      (single category)
 *   GET    /food-items/restaurant/{id}  (public menu)
 *   GET    /food-items/{id}      (single item detail)
 *   GET    /v3/api-docs/**       (OpenAPI spec)
 *   GET    /swagger-ui/**        (Swagger UI)
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
                // NOTE: Paths here are relative to the servlet context (/api),
                // so Spring Security sees them WITHOUT the /api prefix.
                .requestMatchers("/auth/**").permitAll()

                // ---------- Public read — Restaurants -----------------
                .requestMatchers(HttpMethod.GET, "/restaurants").permitAll()
                .requestMatchers(HttpMethod.GET, "/restaurants/{id}").permitAll()

                // ---------- Public read — Categories ------------------
                .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories/{id}").permitAll()

                // ---------- Public read — Food Items (Menu) -----------
                .requestMatchers(HttpMethod.GET, "/food-items/restaurant/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/food-items/{id}").permitAll()

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

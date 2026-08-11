package com.fooddelivery.security;

import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserRole;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.EmailService;
import com.fooddelivery.service.TokenStoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.io.IOException;
import java.util.UUID;

/**
 * Called after successful Google OAuth2 login.
 *
 * Flow:
 *  1. Extract email + name from Google profile
 *  2. Find existing user OR auto-create one (auto-register)
 *  3. Generate JWT token pair
 *  4. Redirect to frontend /oauth2/callback?token=...&refresh=...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository    userRepository;
    private final JwtTokenProvider  jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name  = oauth2User.getAttribute("name");

        if (email == null) {
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/login?error=no_email");
            return;
        }

        // Find or create the user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .name(name != null ? name : "Google User")
                    .email(email)
                    .password(UUID.randomUUID().toString()) // random, unusable password
                    .phone("0000000000")                    // placeholder — user should update
                    .role(UserRole.CUSTOMER)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(newUser);
            log.info("Auto-registered Google user: {}", email);
            return saved;
        });

        if (!user.getIsActive()) {
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/login?error=account_disabled");
            return;
        }

        // Build an Authentication with proper authorities
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String accessToken  = jwtTokenProvider.generateToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(auth);

        log.info("OAuth2 login success for {}", email);

        // Redirect back to frontend with tokens in URL fragment (not query — safer)
        String redirectUrl = frontendUrl + "/oauth2/callback"
                + "?token="   + accessToken
                + "&refresh=" + refreshToken
                + "&role="    + user.getRole().name();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

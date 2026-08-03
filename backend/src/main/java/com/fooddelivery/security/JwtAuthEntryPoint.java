package com.fooddelivery.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a clean JSON 401 response instead of the default redirect.
 *
 * Uses Spring's auto-configured ObjectMapper (injected via constructor)
 * which already has JavaTimeModule registered — fixes the
 * "LocalDateTime not supported by default" error that occurs when
 * ApiResponse.timestamp is serialized by a raw new ObjectMapper().
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    // Inject Spring Boot's pre-configured ObjectMapper
    // (registered in application.properties via spring.jackson.*)
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Object> body = ApiResponse.error(
                "Unauthorized: " + authException.getMessage());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

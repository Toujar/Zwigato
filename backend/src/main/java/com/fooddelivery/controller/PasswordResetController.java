package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.EmailService;
import com.fooddelivery.service.TokenStoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Handles forgot-password and reset-password flows.
 *
 * POST /auth/forgot-password  { email }          → sends reset link
 * POST /auth/reset-password   { token, newPassword } → sets new password
 * POST /auth/verify-reset-token { token }        → validates token (for frontend to check)
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication")
public class PasswordResetController {

    private final UserRepository    userRepository;
    private final TokenStoreService tokenStore;
    private final EmailService      emailService;
    private final PasswordEncoder   passwordEncoder;

    // ── Request bodies ────────────────────────────────────────────
    @Data static class ForgotRequest  { @Email @NotBlank String email; }
    @Data static class ResetRequest   { @NotBlank String token; @NotBlank @Size(min=8) String newPassword; }
    @Data static class VerifyRequest  { @NotBlank String token; }

    // ── POST /auth/forgot-password ────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgot(@RequestBody ForgotRequest req) {
        // Always return 200 — don't leak whether the email exists
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            String token = tokenStore.createResetToken(req.getEmail());
            emailService.sendPasswordResetEmail(req.getEmail(), token, user.getName());
            log.info("Password reset token created for {}", req.getEmail());
        });
        return ResponseEntity.ok(ApiResponse.success(
            "If that email is registered you will receive a reset link shortly.",
            "Check your inbox"));
    }

    // ── POST /auth/verify-reset-token ─────────────────────────────
    @PostMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> verify(@RequestBody VerifyRequest req) {
        String email = tokenStore.validateResetToken(req.getToken());
        if (email == null) throw new BadRequestException("Reset link is invalid or has expired");
        return ResponseEntity.ok(ApiResponse.success(true, "Token is valid"));
    }

    // ── POST /auth/reset-password ─────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> reset(@RequestBody ResetRequest req) {
        String email = tokenStore.validateResetToken(req.getToken());
        if (email == null) throw new BadRequestException("Reset link is invalid or has expired");

        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        tokenStore.consumeResetToken(req.getToken());

        log.info("Password reset successful for {}", email);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", "Done"));
    }
}

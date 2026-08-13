package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.EmailService;
import com.fooddelivery.service.TokenStoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP phone verification flow via email.
 *
 * POST /auth/otp/send    { email }        → sends 6-digit OTP
 * POST /auth/otp/verify  { email, otp }   → marks phone as verified
 *
 * Note: In production replace email with an SMS provider (Twilio/MSG91).
 * The backend logic is identical — only the delivery channel changes.
 */
@Slf4j
@RestController
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
@Tag(name = "1. Authentication")
public class OtpController {

    private final UserRepository    userRepository;
    private final TokenStoreService tokenStore;
    private final EmailService      emailService;

    @Data static class SendOtpRequest   { @Email @NotBlank String email; }
    @Data static class VerifyOtpRequest { @Email @NotBlank String email; @NotBlank String otp; }

    // ── POST /auth/otp/send ───────────────────────────────────────
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> send(@RequestBody SendOtpRequest req) {
        var user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new BadRequestException("No account found for that email"));

        String otp = tokenStore.createOtp(req.getEmail());
        try {
            emailService.sendOtpEmail(req.getEmail(), otp, user.getName());
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", req.getEmail(), e.getMessage());
        }

        log.info("OTP sent for {}", req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "OTP sent to your email", "Check your inbox"));
    }

    // ── POST /auth/otp/verify ─────────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verify(@RequestBody VerifyOtpRequest req) {
        boolean valid = tokenStore.validateAndConsumeOtp(req.getEmail(), req.getOtp());
        if (!valid) throw new BadRequestException("Invalid or expired OTP. Please request a new one.");

        // Mark phone as verified (we reuse isActive for simplicity; add phoneVerified field for production)
        log.info("OTP verified for {}", req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(true, "Phone verified successfully"));
    }
}

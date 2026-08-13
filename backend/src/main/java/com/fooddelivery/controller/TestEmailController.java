package com.fooddelivery.controller;

import com.fooddelivery.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller for email functionality.
 * Use this to test if your Gmail configuration is working.
 * 
 * DELETE this file after email is working properly.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailService emailService;

    /**
     * Test email sending functionality using OTP method.
     * 
     * GET /api/test/email?to=your-email@gmail.com
     */
    @GetMapping("/email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            // Use the existing OTP email method for testing
            emailService.sendOtpEmail(to, "123456", "Test User");
            
            log.info("Test OTP email sent successfully to: {}", to);
            return ResponseEntity.ok("✅ Test OTP email sent successfully to: " + to + 
                                   "\nCheck your inbox for OTP: 123456");
            
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", to, e);
            return ResponseEntity.badRequest()
                .body("❌ Email failed: " + e.getMessage() + 
                      "\n\nCheck:\n1. Gmail App Password in application.properties\n2. 2FA enabled on Gmail\n3. Internet connection");
        }
    }

    /**
     * Test password reset email.
     * 
     * GET /api/test/password-reset?to=your-email@gmail.com
     */
    @GetMapping("/password-reset")
    public ResponseEntity<String> testPasswordReset(@RequestParam String to) {
        try {
            String resetLink = "http://localhost:5173/reset-password?token=test123";
            emailService.sendPasswordResetEmail(to, resetLink, "Test User");
            
            log.info("Test password reset email sent successfully to: {}", to);
            return ResponseEntity.ok("✅ Test password reset email sent successfully to: " + to);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            return ResponseEntity.badRequest()
                .body("❌ Password reset email failed: " + e.getMessage());
        }
    }
}
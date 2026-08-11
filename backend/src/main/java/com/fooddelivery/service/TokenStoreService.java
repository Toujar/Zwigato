package com.fooddelivery.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for short-lived tokens (password reset + OTP).
 *
 * Production note: replace this with Redis for multi-instance deployments.
 * For now, ConcurrentHashMap is sufficient for a single-node app.
 *
 * Two token types:
 *  - RESET  : 32-char hex token, 15-minute expiry, used in email link
 *  - OTP    : 6-digit numeric code, 10-minute expiry, verified in UI
 */
@Slf4j
@Service
public class TokenStoreService {

    private record TokenEntry(String value, String email, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    private final Map<String, TokenEntry> resetTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenEntry> otpTokens   = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();

    // ── Password reset ────────────────────────────────────────────

    /** Generates a 32-byte hex reset token, stores it, returns it. */
    public String createResetToken(String email) {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        String token = sb.toString();

        resetTokens.put(token, new TokenEntry(token, email,
                Instant.now().plusSeconds(900))); // 15 min
        return token;
    }

    /** Returns the email for a valid reset token, or null if invalid/expired. */
    public String validateResetToken(String token) {
        TokenEntry entry = resetTokens.get(token);
        if (entry == null || entry.isExpired()) {
            resetTokens.remove(token);
            return null;
        }
        return entry.email();
    }

    /** Invalidates the reset token after use. */
    public void consumeResetToken(String token) {
        resetTokens.remove(token);
    }

    // ── OTP ──────────────────────────────────────────────────────

    /** Generates a 6-digit OTP for the given email, stores it, returns it. */
    public String createOtp(String email) {
        int code = 100_000 + rng.nextInt(900_000); // 100000–999999
        String otp = String.valueOf(code);

        // Key by email so one OTP per user at a time
        otpTokens.put(email, new TokenEntry(otp, email,
                Instant.now().plusSeconds(600))); // 10 min
        return otp;
    }

    /** Returns true if the OTP matches and is not expired. Consumes on success. */
    public boolean validateAndConsumeOtp(String email, String otp) {
        TokenEntry entry = otpTokens.get(email);
        if (entry == null || entry.isExpired()) {
            otpTokens.remove(email);
            return false;
        }
        if (entry.value().equals(otp)) {
            otpTokens.remove(email);
            return true;
        }
        return false;
    }
}

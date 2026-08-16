package com.fooddelivery.config;

import com.razorpay.RazorpayClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay Payment Gateway Configuration.
 *
 * Initializes the Razorpay SDK client with API credentials.
 * Credentials must be provided via environment variables or application.properties:
 *  - razorpay.enabled    : Set to true to enable Razorpay integration (default: false)
 *  - razorpay.key_id     : Razorpay API Key
 *  - razorpay.key_secret : Razorpay API Secret
 *
 * These should be stored in a secure vault (AWS Secrets Manager, HashiCorp Vault, etc.)
 * in production.
 *
 * When razorpay.enabled=false or credentials are missing, the bean is not created
 * and payment operations will gracefully fail with appropriate error messages.
 */
@Configuration
@Getter
public class RazorpayConfig {

    @Value("${razorpay.enabled:false}")
    private boolean enabled;

    @Value("${razorpay.key_id:}")
    private String keyId;

    @Value("${razorpay.key_secret:}")
    private String keySecret;

    /**
     * Creates and provides a configured RazorpayClient bean.
     * Only created when razorpay.enabled=true and credentials are provided.
     *
     * @return RazorpayClient configured with API credentials
     * @throws Exception if initialization fails
     */
    @Bean
    @ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
    public RazorpayClient razorpayClient() throws Exception {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay credentials not configured. "
                    + "Set razorpay.key_id and razorpay.key_secret in application.properties");
        }
        return new RazorpayClient(keyId, keySecret);
    }

    /**
     * Provides the Razorpay Key ID for client-side Checkout form.
     * This is public and safe to expose to the frontend.
     */
    public String getPublicKeyId() {
        return keyId;
    }

    /**
     * Checks if Razorpay integration is enabled and configured.
     *
     * @return true if Razorpay is enabled and credentials are present
     */
    public boolean isConfigured() {
        return enabled 
            && keyId != null && !keyId.isBlank() 
            && keySecret != null && !keySecret.isBlank();
    }
}

package com.fooddelivery.service.impl;

import com.fooddelivery.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Manages email delivery via Spring Mail (JavaMailSender).
 *
 * Configuration is in application.properties:
 *   spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password
 *   spring.mail.properties.mail.smtp.auth, mail.smtp.starttls.enable
 *
 * Supports attachment (for invoice PDFs) and HTML templates.
 * Errors are logged but not propagated — emails are non-critical to order flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ---------------------------------------------------------------
    // Invoice Email
    // ---------------------------------------------------------------

    @Override
    public void sendInvoiceEmail(String customerEmail, String invoiceNumber, String pdfFilePath) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Tap Food Delivery");
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(customerEmail);
        helper.setSubject("Your Invoice: " + invoiceNumber + " - Tap Food Delivery");

        String htmlContent = buildInvoiceEmailBody(invoiceNumber);
        helper.setText(htmlContent, true);

        // Attach PDF file
        try {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                FileSystemResource fileResource = new FileSystemResource(pdfFile);
                helper.addAttachment(invoiceNumber + ".pdf", fileResource);
                log.info("Invoice PDF attached: {}", pdfFilePath);
            } else {
                log.warn("Invoice PDF file not found: {}", pdfFilePath);
            }
        } catch (Exception e) {
            log.error("Failed to attach invoice PDF: {}", e.getMessage());
            // Continue — still send email without attachment
        }

        try {
            javaMailSender.send(mimeMessage);
            log.info("Invoice email sent successfully to: {} (Invoice: {})", customerEmail, invoiceNumber);
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", customerEmail, e.getMessage());
            throw new MessagingException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Password Reset Email
    // ---------------------------------------------------------------

    @Override
    public void sendPasswordResetEmail(String customerEmail, String resetLink, String userName) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Tap Food Delivery");
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(customerEmail);
        helper.setSubject("Password Reset Request - Tap Food Delivery");

        String htmlContent = buildPasswordResetEmailBody(resetLink);
        helper.setText(htmlContent, true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("Password reset email sent to: {}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", customerEmail, e.getMessage());
            throw new MessagingException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // OTP Email
    // ---------------------------------------------------------------

    @Override
    public void sendOtpEmail(String customerEmail, String otp, String userName) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Tap Food Delivery");
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(customerEmail);
        helper.setSubject("Your OTP Verification Code - Tap Food Delivery");

        String htmlContent = buildOtpEmailBody(otp);
        helper.setText(htmlContent, true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("OTP email sent to: {}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", customerEmail, e.getMessage());
            throw new MessagingException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Order Confirmation Email
    // ---------------------------------------------------------------

    @Override
    public void sendOrderConfirmationEmail(String customerEmail, Long orderId) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Tap Food Delivery");
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(customerEmail);
        helper.setSubject("Order Confirmed - Order #" + orderId + " - Tap Food Delivery");

        String htmlContent = buildOrderConfirmationEmailBody(orderId);
        helper.setText(htmlContent, true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("Order confirmation email sent to: {} (Order: {})", customerEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", customerEmail, e.getMessage());
            throw new MessagingException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Order Delivery Email
    // ---------------------------------------------------------------

    @Override
    public void sendOrderDeliveryEmail(String customerEmail, Long orderId) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Tap Food Delivery");
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(customerEmail);
        helper.setSubject("Your Order Has Been Delivered! - Order #" + orderId + " - Tap Food Delivery");

        String htmlContent = buildOrderDeliveryEmailBody(orderId);
        helper.setText(htmlContent, true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("Order delivery email sent to: {} (Order: {})", customerEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send order delivery email to {}: {}", customerEmail, e.getMessage());
            throw new MessagingException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Email Body Templates
    // ---------------------------------------------------------------

    private String buildInvoiceEmailBody(String invoiceNumber) {
        return String.format("""
            <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #ff6b35; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .button { display: inline-block; background-color: #ff6b35; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Tap Food Delivery</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Your invoice <strong>%s</strong> is now ready. Please find it attached to this email.</p>
                            <p>If you have any questions regarding your order, feel free to contact us.</p>
                            <br/>
                            <p>Thank you for ordering with Tap Food Delivery!</p>
                            <br/>
                            <p>Best regards,<br/>The Tap Team</p>
                        </div>
                    </div>
                </body>
            </html>
            """, invoiceNumber);
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return String.format("""
            <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #ff6b35; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .button { display: inline-block; background-color: #ff6b35; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Tap Food Delivery</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>We received a request to reset your password. Click the link below to set a new password:</p>
                            <p><a href="%s" class="button">Reset Password</a></p>
                            <p>This link will expire in 30 minutes.</p>
                            <p>If you did not request this, please ignore this email.</p>
                            <br/>
                            <p>Best regards,<br/>The Tap Team</p>
                        </div>
                    </div>
                </body>
            </html>
            """, resetLink);
    }

    private String buildOtpEmailBody(String otp) {
        return String.format("""
            <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #ff6b35; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .otp-box { background-color: #fffbea; border: 2px solid #ff6b35; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Tap Food Delivery</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Your OTP verification code is:</p>
                            <div class="otp-box">%s</div>
                            <p>This code will expire in 10 minutes.</p>
                            <p>Do not share this code with anyone.</p>
                            <br/>
                            <p>Best regards,<br/>The Tap Team</p>
                        </div>
                    </div>
                </body>
            </html>
            """, otp);
    }

    private String buildOrderConfirmationEmailBody(Long orderId) {
        return String.format("""
            <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #ff6b35; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .button { display: inline-block; background-color: #ff6b35; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Tap Food Delivery</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Thank you for your order! Your order <strong>#%d</strong> has been confirmed.</p>
                            <p>We will prepare your food shortly. You will receive a notification once your order is being delivered.</p>
                            <p><a href="%s/orders/%d" class="button">Track Your Order</a></p>
                            <br/>
                            <p>Best regards,<br/>The Tap Team</p>
                        </div>
                    </div>
                </body>
            </html>
            """, orderId, frontendUrl, orderId);
    }

    private String buildOrderDeliveryEmailBody(Long orderId) {
        return String.format("""
            <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #ff6b35; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .button { display: inline-block; background-color: #ff6b35; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Tap Food Delivery</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Great news! Your order <strong>#%d</strong> has been delivered.</p>
                            <p>We hope you enjoyed your meal! Please rate your experience and let us know how we can improve.</p>
                            <p><a href="%s/orders/%d/feedback" class="button">Leave Feedback</a></p>
                            <br/>
                            <p>Thank you for ordering with Tap Food Delivery!</p>
                            <p>Best regards,<br/>The Tap Team</p>
                        </div>
                    </div>
                </body>
            </html>
            """, orderId, frontendUrl, orderId);
    }
}

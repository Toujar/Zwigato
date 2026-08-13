package com.fooddelivery.service;

import jakarta.mail.MessagingException;

/**
 * Contract for email delivery operations.
 *
 * Used by: Invoice service, Password reset, OTP verification, Order notifications.
 * Abstracts email sending to support different providers (SMTP, SendGrid, AWS SES).
 */
public interface EmailService {

    /**
     * Sends invoice PDF via email to customer.
     *
     * @param customerEmail recipient email address
     * @param invoiceNumber invoice number for subject/reference
     * @param pdfFilePath path or URL to PDF file
     * @throws MessagingException if email sending fails
     */
    void sendInvoiceEmail(String customerEmail, String invoiceNumber, String pdfFilePath) throws MessagingException;

    /**
     * Sends password reset email.
     *
     * @param customerEmail recipient email address
     * @param resetLink password reset link
     * @param userName user's name for personalization
     * @throws MessagingException if email sending fails
     */
    void sendPasswordResetEmail(String customerEmail, String resetLink, String userName) throws MessagingException;

    /**
     * Sends OTP verification email.
     *
     * @param customerEmail recipient email address
     * @param otp one-time password
     * @param userName user's name for personalization
     * @throws MessagingException if email sending fails
     */
    void sendOtpEmail(String customerEmail, String otp, String userName) throws MessagingException;

    /**
     * Sends order confirmation email.
     *
     * @param customerEmail recipient email address
     * @param orderId order ID for reference
     * @throws MessagingException if email sending fails
     */
    void sendOrderConfirmationEmail(String customerEmail, Long orderId) throws MessagingException;

    /**
     * Sends order delivery email.
     *
     * @param customerEmail recipient email address
     * @param orderId order ID for reference
     * @throws MessagingException if email sending fails
     */
    void sendOrderDeliveryEmail(String customerEmail, Long orderId) throws MessagingException;
}

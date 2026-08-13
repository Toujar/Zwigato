package com.fooddelivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  Entity  : Invoice
 *  Table   : invoices
 * ============================================================
 *
 *  Represents a generated invoice/receipt for an order.
 *  Invoices are generated after order delivery is confirmed.
 *
 *  Relationships:
 *   - Many-to-One → Order  (the order this invoice is for)
 *   - Many-to-One → User   (the customer)
 *
 *  PDF Storage:
 *   - pdfFilePath: Path where the PDF is stored on disk
 *   - invoiceNumber: Unique invoice ID (e.g., INV-2024-001234)
 *
 *  Email Tracking:
 *   - emailSentAt: Timestamp when invoice was emailed (null if not sent)
 *   - emailRetryCount: Number of times email was retried (if failed)
 *
 *  Auditing:
 *   - generatedAt: When the invoice was created
 * ============================================================
 */
@Entity
@Table(
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoices_order_id", columnList = "order_id"),
        @Index(name = "idx_invoices_user_id", columnList = "user_id"),
        @Index(name = "idx_invoices_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoices_generated_at", columnList = "generated_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    // ----------------------------------------------------------
    // Primary Key
    // ----------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ----------------------------------------------------------
    // Relationships
    // ----------------------------------------------------------

    /**
     * The order this invoice is for.
     * ON DELETE CASCADE — deleting an order also removes its invoice.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "order_id",
        nullable   = false,
        unique     = true,
        foreignKey = @ForeignKey(name = "fk_invoice_order")
    )
    private Order order;

    /**
     * The customer who received this invoice.
     * ON DELETE CASCADE — deleting a user removes their invoices.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "user_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_invoice_user")
    )
    private User user;

    // ----------------------------------------------------------
    // Invoice Details
    // ----------------------------------------------------------

    /**
     * Unique invoice number (e.g., INV-2024-001234).
     * Used for both file naming and human-readable reference.
     */
    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    /**
     * File system path where the PDF is stored.
     * Format: /invoices/INV-2024-001234.pdf
     */
    @NotBlank(message = "PDF file path is required")
    @Column(name = "pdf_file_path", nullable = false, columnDefinition = "TEXT")
    private String pdfFilePath;

    // ----------------------------------------------------------
    // Email Tracking
    // ----------------------------------------------------------

    /**
     * Timestamp when the invoice was successfully emailed to the customer.
     * Null if not yet sent or if sending failed permanently.
     */
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    /**
     * Number of times the email delivery was retried.
     * Incremented each time a send attempt fails.
     * Used to avoid infinite retry loops.
     */
    @Builder.Default
    @Column(name = "email_retry_count", nullable = false)
    private Integer emailRetryCount = 0;

    // ----------------------------------------------------------
    // Auditing
    // ----------------------------------------------------------

    /** When the invoice PDF was generated. */
    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    // ----------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------

    /** Returns true if the invoice has been successfully emailed. */
    public boolean isEmailSent() {
        return emailSentAt != null;
    }

    /** Returns true if the email delivery has exceeded max retries (default: 3). */
    public boolean hasExhaustedRetries() {
        return emailRetryCount >= 3;
    }
}

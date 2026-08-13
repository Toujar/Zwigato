package com.fooddelivery.repository;

import com.fooddelivery.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Invoice entities.
 *
 * Supports:
 *  - Finding invoices by order ID
 *  - Finding invoices by invoice number
 *  - Finding all invoices for a customer
 *  - Finding invoices pending email delivery
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by order ID (unique one-to-one relationship).
     *
     * @param orderId the order ID
     * @return the invoice for that order, if it exists
     */
    Optional<Invoice> findByOrderId(Long orderId);

    /**
     * Find invoice by invoice number.
     *
     * @param invoiceNumber the invoice number (e.g., INV-2024-001234)
     * @return the invoice with that number, if it exists
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find all invoices for a specific customer.
     *
     * @param userId the customer user ID
     * @return list of invoices for that customer
     */
    List<Invoice> findByUserId(Long userId);

    /**
     * Find invoices that have not been emailed yet (emailSentAt is null)
     * and have not exhausted their retry count.
     *
     * Used by a scheduled job to retry email delivery.
     *
     * @return list of invoices pending email delivery
     */
    List<Invoice> findByEmailSentAtIsNullAndEmailRetryCountLessThan(Integer maxRetries);
}

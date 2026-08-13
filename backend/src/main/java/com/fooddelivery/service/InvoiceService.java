package com.fooddelivery.service;

import com.fooddelivery.entity.Invoice;
import com.fooddelivery.entity.Order;

import java.io.IOException;
import java.util.List;

/**
 * Contract for invoice generation and management.
 *
 * Responsibilities:
 *  - Generate invoice PDF after order delivery
 *  - Store invoice metadata in database
 *  - Email invoices to customers
 *  - Retrieve invoice history for customers
 *
 * Triggered by: OrderService when order transitions to DELIVERED
 */
public interface InvoiceService {

    /**
     * Generate an invoice PDF for a delivered order.
     *
     * This method:
     * 1. Creates an Invoice entity with a unique invoice number
     * 2. Generates a PDF with order details, pricing breakdown, and payment method
     * 3. Stores the PDF to disk
     * 4. Saves Invoice metadata to the database
     * 5. Emails the invoice to the customer
     *
     * @param order the order to invoice (should be in DELIVERED status)
     * @return the created Invoice entity
     * @throws IOException if PDF generation or file I/O fails
     * @throws IllegalArgumentException if order is not DELIVERED
     */
    Invoice generateInvoice(Order order) throws IOException;

    /**
     * Retrieve an invoice by its ID.
     *
     * @param invoiceId the invoice ID
     * @return the invoice, or throw exception if not found
     */
    Invoice getInvoiceById(Long invoiceId);

    /**
     * Retrieve an invoice by order ID (one-to-one relationship).
     *
     * @param orderId the order ID
     * @return the invoice for that order, or throw exception if not found
     */
    Invoice getInvoiceByOrderId(Long orderId);

    /**
     * Retrieve all invoices for a customer (for invoice/receipt history).
     *
     * @param userId the customer user ID
     * @return list of invoices for that customer
     */
    List<Invoice> getInvoicesByUserId(Long userId);

    /**
     * Download invoice PDF file path.
     *
     * Used by frontend to fetch and display the PDF.
     *
     * @param invoiceId the invoice ID
     * @return the file system path to the PDF
     */
    String getInvoicePdfPath(Long invoiceId);

    /**
     * Retry email delivery for invoices that failed.
     *
     * Called by a scheduled job to re-attempt email sending
     * for invoices that haven't been delivered yet.
     *
     * Max 3 retries per invoice (configurable).
     *
     * @param invoiceId the invoice ID
     * @return true if email was sent successfully, false if retry limit exceeded
     */
    boolean retryEmailDelivery(Long invoiceId);
}

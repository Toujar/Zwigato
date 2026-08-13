package com.fooddelivery.service.impl;

import com.fooddelivery.entity.*;
import com.fooddelivery.entity.enums.OrderStatus;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.InvoiceRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.service.EmailService;
import com.fooddelivery.service.InvoiceService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages invoice generation and delivery.
 *
 * Workflow:
 * 1. After order is marked DELIVERED, OrderService calls generateInvoice()
 * 2. We create a unique invoice number (INV-YYYY-XXXXXX)
 * 3. Generate PDF with order details and pricing breakdown
 * 4. Save PDF to disk
 * 5. Store Invoice metadata in database
 * 6. Email PDF to customer
 *
 * Configuration:
 *  - invoice.storage.path: Directory where PDFs are stored
 *  - email delivery via EmailService (already configured)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Value("${invoice.storage.path:/invoices}")
    private String invoiceStoragePath;

    @Value("${app.name:Tap Food Delivery}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ---------------------------------------------------------------
    // Invoice Generation
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public Invoice generateInvoice(Order order) throws IOException {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                "Invoice can only be generated for DELIVERED orders. Current status: " + order.getStatus());
        }

        // 1. Create invoice number (INV-YYYY-XXXXXX)
        String invoiceNumber = generateInvoiceNumber();

        // 2. Create Invoice entity
        Invoice invoice = Invoice.builder()
            .order(order)
            .user(order.getUser())
            .invoiceNumber(invoiceNumber)
            .pdfFilePath("")  // Set after PDF generation
            .build();

        try {
            // 3. Generate PDF
            String pdfFilePath = generatePdfFile(order, invoiceNumber);
            invoice.setPdfFilePath(pdfFilePath);

            // 4. Save invoice to database
            invoice = invoiceRepository.save(invoice);
            log.info("Invoice created successfully: {} for order: {}", invoiceNumber, order.getId());

            // 5. Email invoice to customer (non-blocking)
            try {
                emailService.sendInvoiceEmail(
                    order.getUser().getEmail(),
                    invoiceNumber,
                    pdfFilePath
                );
                invoice.setEmailSentAt(java.time.LocalDateTime.now());
                invoice = invoiceRepository.save(invoice);
                log.info("Invoice emailed successfully to: {}", order.getUser().getEmail());
            } catch (MessagingException e) {
                log.error("Failed to email invoice {}: {}", invoiceNumber, e.getMessage());
                // Invoice is still saved even if email fails — can retry later
            }

            return invoice;
        } catch (IOException e) {
            log.error("Failed to generate invoice PDF for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }

    // ---------------------------------------------------------------
    // Invoice Retrieval
    // ---------------------------------------------------------------

    @Override
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId.toString()));
    }

    @Override
    public Invoice getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "orderId", orderId.toString()));
    }

    @Override
    public List<Invoice> getInvoicesByUserId(Long userId) {
        return invoiceRepository.findByUserId(userId);
    }

    @Override
    public String getInvoicePdfPath(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        return invoice.getPdfFilePath();
    }

    // ---------------------------------------------------------------
    // Email Retry
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public boolean retryEmailDelivery(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);

        if (invoice.isEmailSent()) {
            log.info("Invoice {} already emailed, skipping retry", invoice.getInvoiceNumber());
            return true;
        }

        if (invoice.hasExhaustedRetries()) {
            log.warn("Invoice {} has exceeded max retry attempts ({})", 
                invoice.getInvoiceNumber(), invoice.getEmailRetryCount());
            return false;
        }

        try {
            emailService.sendInvoiceEmail(
                invoice.getUser().getEmail(),
                invoice.getInvoiceNumber(),
                invoice.getPdfFilePath()
            );
            invoice.setEmailSentAt(java.time.LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Invoice {} emailed successfully (retry)", invoice.getInvoiceNumber());
            return true;
        } catch (MessagingException e) {
            invoice.setEmailRetryCount(invoice.getEmailRetryCount() + 1);
            invoiceRepository.save(invoice);
            log.error("Failed to email invoice {} (attempt {}): {}", 
                invoice.getInvoiceNumber(), invoice.getEmailRetryCount(), e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // PDF Generation
    // ---------------------------------------------------------------

    /**
     * Generate a professional invoice PDF with:
     * - Header with company name
     * - Invoice number and date
     * - Customer and restaurant details
     * - Order items with customizations
     * - Pricing breakdown (subtotal, tax, delivery fee, total)
     * - Payment method and transaction ID
     */
    private String generatePdfFile(Order order, String invoiceNumber) throws IOException {
        // Ensure storage directory exists
        Files.createDirectories(Paths.get(invoiceStoragePath));

        String fileName = invoiceNumber + ".pdf";
        String filePath = Paths.get(invoiceStoragePath, fileName).toString();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Add invoice content
            addInvoiceHeader(document, order, invoiceNumber);
            addCustomerSection(document, order);
            addItemsSection(document, order);
            addPricingSection(document, order);
            addPaymentSection(document, order);
            addFooter(document);

            document.close();
            log.info("PDF generated successfully: {}", filePath);
            return filePath;
        } catch (FileNotFoundException | DocumentException e) {
            log.error("Failed to generate invoice PDF: {}", e.getMessage());
            throw new IOException("PDF generation failed", e);
        }
    }

    private void addInvoiceHeader(Document document, Order order, String invoiceNumber) throws DocumentException {
        // Company name and title
        Paragraph title = new Paragraph(appName, new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("INVOICE", new Font(Font.FontFamily.HELVETICA, 16));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(5);
        document.add(subtitle);

        // Invoice number and dates
        Paragraph details = new Paragraph();
        details.add(new Chunk("Invoice #: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        details.add(new Chunk(invoiceNumber + "\n", new Font(Font.FontFamily.HELVETICA, 10)));
        details.add(new Chunk("Date: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        details.add(new Chunk(order.getPlacedAt().format(DATE_FORMATTER) + "\n", new Font(Font.FontFamily.HELVETICA, 10)));
        details.add(new Chunk("Delivered: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        details.add(new Chunk(order.getUpdatedAt().format(DATE_FORMATTER), new Font(Font.FontFamily.HELVETICA, 10)));
        details.setSpacingAfter(10);
        document.add(details);
    }

    private void addCustomerSection(Document document, Order order) throws DocumentException {
        Paragraph section = new Paragraph("BILLING & DELIVERY DETAILS", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        // Headers
        PdfPCell customerHeader = createHeaderCell("CUSTOMER");
        PdfPCell restaurantHeader = createHeaderCell("RESTAURANT");
        table.addCell(customerHeader);
        table.addCell(restaurantHeader);

        // Content
        table.addCell(createContentCell(order.getUser().getName()));
        table.addCell(createContentCell(order.getRestaurant().getName()));

        table.addCell(createContentCell(order.getUser().getEmail()));
        table.addCell(createContentCell(order.getRestaurant().getAddress()));

        document.add(table);

        Paragraph address = new Paragraph();
        address.add(new Chunk("DELIVERY TO: \n", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        address.add(new Chunk(order.getDeliveryAddress(), new Font(Font.FontFamily.HELVETICA, 10)));
        document.add(address);
    }

    private void addItemsSection(Document document, Order order) throws DocumentException {
        Paragraph section = new Paragraph("ORDER ITEMS", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{40, 10, 20, 20});
        table.setWidthPercentage(100);

        // Headers
        table.addCell(createHeaderCell("Item"));
        table.addCell(createHeaderCell("Qty"));
        table.addCell(createHeaderCell("Price"));
        table.addCell(createHeaderCell("Total"));

        // Items
        for (OrderItem item : order.getOrderItems()) {
            String itemName = item.getFoodItem().getName();
            
            // Add customizations
            if (item.getSize() != null || item.getSpiceLevel() != null) {
                itemName += "\n(";
                if (item.getSize() != null) itemName += item.getSize();
                if (item.getSpiceLevel() != null) itemName += ", " + item.getSpiceLevel();
                itemName += ")";
            }
            
            if (item.getAddOns() != null && !item.getAddOns().isEmpty()) {
                itemName += "\nAdd-ons: " + item.getAddOns();
            }

            table.addCell(createContentCell(itemName));
            table.addCell(createContentCell(item.getQuantity().toString()));
            table.addCell(createContentCell("₹" + item.getFoodItem().getPrice().setScale(2, BigDecimal.ROUND_HALF_UP)));
            table.addCell(createContentCell("₹" + item.getSubtotal().setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        document.add(table);
    }

    private void addPricingSection(Document document, Order order) throws DocumentException {
        Paragraph section = new Paragraph("PRICING BREAKDOWN", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{60, 40});
        table.setWidthPercentage(100);

        // Subtotal
        table.addCell(createContentCell("Subtotal"));
        table.addCell(createPriceCell("₹" + order.getSubtotal().setScale(2, BigDecimal.ROUND_HALF_UP)));

        // Tax
        if (order.getTax().compareTo(BigDecimal.ZERO) > 0) {
            table.addCell(createContentCell("Tax"));
            table.addCell(createPriceCell("₹" + order.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        // Delivery Fee
        if (order.getDeliveryFee().compareTo(BigDecimal.ZERO) > 0) {
            table.addCell(createContentCell("Delivery Fee"));
            table.addCell(createPriceCell("₹" + order.getDeliveryFee().setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        // Total
        PdfPCell totalLabel = createContentCell("TOTAL");
        totalLabel.setPhrase(new Phrase("TOTAL", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        table.addCell(totalLabel);
        
        PdfPCell totalAmount = createPriceCell("₹" + order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
        totalAmount.setPhrase(new Phrase("₹" + order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP), new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        table.addCell(totalAmount);

        document.add(table);
    }

    private void addPaymentSection(Document document, Order order) throws DocumentException {
        if (order.getPayment() != null) {
            Paragraph section = new Paragraph("PAYMENT INFORMATION", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
            section.setSpacingBefore(10);
            document.add(section);

            Payment payment = order.getPayment();
            Paragraph info = new Paragraph();
            info.add(new Chunk("Method: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
            info.add(new Chunk((payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Not specified") + "\n", new Font(Font.FontFamily.HELVETICA, 10)));
            
            info.add(new Chunk("Status: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
            info.add(new Chunk((payment.getStatus() != null ? payment.getStatus().toString() : "Unknown") + "\n", new Font(Font.FontFamily.HELVETICA, 10)));

            if (payment.getTransactionId() != null) {
                info.add(new Chunk("Transaction ID: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
                info.add(new Chunk(payment.getTransactionId() + "\n", new Font(Font.FontFamily.HELVETICA, 10)));
            }

            if (payment.getRazorpayPaymentId() != null) {
                info.add(new Chunk("Payment ID: ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
                info.add(new Chunk(payment.getRazorpayPaymentId() + "\n", new Font(Font.FontFamily.HELVETICA, 10)));
            }

            document.add(info);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("\n\nThank you for your order! Enjoy your meal!", new Font(Font.FontFamily.HELVETICA, 10));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    // ---------------------------------------------------------------
    // Helper Methods for PDF Cells
    // ---------------------------------------------------------------

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createContentCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10)));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createPriceCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(5);
        return cell;
    }

    // ---------------------------------------------------------------
    // Utility Methods
    // ---------------------------------------------------------------

    /**
     * Generate a unique invoice number: INV-YYYY-XXXXXX
     * where YYYY is the year and XXXXXX is a sequence number.
     */
    private String generateInvoiceNumber() {
        int year = java.time.Year.now().getValue();
        long sequenceNumber = invoiceRepository.count() + 1;
        return String.format("INV-%d-%06d", year, sequenceNumber);
    }
}

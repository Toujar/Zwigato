package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.InvoiceResponse;
import com.fooddelivery.entity.Invoice;
import com.fooddelivery.entity.User;
import com.fooddelivery.service.InvoiceService;
import com.fooddelivery.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles invoice/receipt endpoints.
 *
 * GET  /api/invoices/{invoiceId}              → Download specific invoice PDF
 * GET  /api/invoices/order/{orderId}          → Get invoice for an order
 * GET  /api/invoices/my-invoices              → List all invoices for current customer
 */
@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "8. Invoices & Receipts")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final SecurityUtils securityUtils;

    // ── GET /api/invoices/{invoiceId} ─────────────────────────────
    /**
     * Retrieve a specific invoice by ID.
     * Customer can only view their own invoices.
     */
    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable Long invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        
        // Verify ownership
        User currentUser = securityUtils.getCurrentUser();
        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                .body(ApiResponse.<InvoiceResponse>error("You do not have permission to view this invoice"));
        }

        InvoiceResponse response = toResponse(invoice);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice retrieved successfully"));
    }

    // ── GET /api/invoices/order/{orderId} ──────────────────────────
    /**
     * Retrieve invoice for a specific order.
     * Customer can only view invoices for their own orders.
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get invoice by order ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrder(@PathVariable Long orderId) {
        Invoice invoice = invoiceService.getInvoiceByOrderId(orderId);
        
        // Verify ownership
        User currentUser = securityUtils.getCurrentUser();
        if (!invoice.getOrder().getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                .body(ApiResponse.<InvoiceResponse>error("You do not have permission to view this invoice"));
        }

        InvoiceResponse response = toResponse(invoice);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice retrieved successfully"));
    }

    // ── GET /api/invoices/my-invoices ──────────────────────────────
    /**
     * List all invoices for the current customer.
     * Sorted by most recent first.
     */
    @GetMapping("/my-invoices")
    @Operation(summary = "Get my invoice history")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices() {
        User currentUser = securityUtils.getCurrentUser();
        
        List<Invoice> invoices = invoiceService.getInvoicesByUserId(currentUser.getId());
        List<InvoiceResponse> responses = invoices.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "Invoices retrieved successfully"));
    }

    // ── GET /api/invoices/{invoiceId}/download ─────────────────────
    /**
     * Download invoice PDF file.
     * Used by frontend to fetch and display PDF.
     * Customer can only download their own invoices.
     */
    @GetMapping("/{invoiceId}/download")
    @Operation(summary = "Download invoice PDF")
    public ResponseEntity<Resource> downloadInvoicePdf(@PathVariable Long invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        
        // Verify ownership
        User currentUser = securityUtils.getCurrentUser();
        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Resource resource = new FileSystemResource(invoice.getPdfFilePath());
            
            if (!resource.exists()) {
                log.warn("Invoice PDF file not found: {}", invoice.getPdfFilePath());
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(resource);
        } catch (Exception e) {
            log.error("Failed to download invoice {}: {}", invoiceId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────────────

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
            .id(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .orderId(invoice.getOrder().getId())
            .generatedAt(invoice.getGeneratedAt())
            .emailSentAt(invoice.getEmailSentAt())
            .isEmailSent(invoice.isEmailSent())
            .build();
    }
}

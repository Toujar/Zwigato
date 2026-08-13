package com.fooddelivery.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload representing an Invoice record.
 *
 * Fields:
 *  - id: Invoice database ID
 *  - invoiceNumber: Unique invoice identifier (INV-YYYY-XXXXXX)
 *  - orderId: Associated order ID
 *  - generatedAt: When the invoice was created
 *  - emailSentAt: When the invoice was emailed (null if not sent)
 *  - isEmailSent: Boolean flag for easy frontend checking
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceResponse {
    private Long id;
    private Long orderId;
    private String invoiceNumber;
    private LocalDateTime generatedAt;
    private LocalDateTime emailSentAt;
    private Boolean isEmailSent;
}

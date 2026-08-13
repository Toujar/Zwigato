-- ============================================================
-- Invoice/Receipt Entity Schema
-- ============================================================
-- Automatically generated when OrderStatus transitions to DELIVERED
-- Stores invoice metadata and PDF path for customer downloads

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    pdf_file_path TEXT NOT NULL,
    email_sent_at DATETIME NULL,
    email_retry_count INT NOT NULL DEFAULT 0,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_invoices_order_id (order_id),
    INDEX idx_invoices_user_id (user_id),
    INDEX idx_invoices_invoice_number (invoice_number),
    INDEX idx_invoices_generated_at (generated_at),
    
    -- Foreign Keys
    CONSTRAINT fk_invoice_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

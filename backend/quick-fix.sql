-- Quick fix for ENUM to VARCHAR conversion (run these one by one)
-- This fixes the Hibernate schema validation error on Render

-- Critical fix (this is the one causing the crash)
ALTER TABLE payments MODIFY COLUMN payment_method VARCHAR(20) NOT NULL;

-- Additional fixes for consistency
ALTER TABLE payments MODIFY COLUMN status VARCHAR(10) NOT NULL DEFAULT 'PENDING';
ALTER TABLE payments MODIFY COLUMN refund_status VARCHAR(20);
ALTER TABLE orders MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLACED';
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

-- Verify (should show varchar types, not enum)
DESCRIBE payments;
DESCRIBE orders;
DESCRIBE users;

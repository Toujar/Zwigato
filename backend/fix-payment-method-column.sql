-- ============================================================
-- Fix ENUM vs VARCHAR column type mismatches
-- ============================================================
-- Problem: Database has ENUM types but Hibernate expects VARCHAR
-- Solution: Convert ENUM columns to VARCHAR to match entity definitions
-- ============================================================
-- Root Cause from Render log:
-- Schema-validation: wrong column type encountered in column [payment_method]
-- in table [payments]; found [enum (Types#CHAR)], but expecting [varchar(20) (Types#ENUM)]
-- ============================================================

-- ============================================================
-- STEP 1: Check all current enum columns
-- ============================================================

-- Check all columns that might be ENUM type
SHOW COLUMNS FROM payments WHERE Type LIKE 'enum%';
SHOW COLUMNS FROM orders WHERE Type LIKE 'enum%';
SHOW COLUMNS FROM users WHERE Type LIKE 'enum%';

-- ============================================================
-- STEP 2: Check current values (verify data integrity)
-- ============================================================

-- Check payments table enum values
SELECT 
    COUNT(*) as total_payments,
    payment_method,
    status,
    refund_status
FROM payments 
GROUP BY payment_method, status, refund_status;

-- Check orders table enum values
SELECT 
    COUNT(*) as total_orders,
    status
FROM orders 
GROUP BY status;

-- Check users table enum values
SELECT 
    COUNT(*) as total_users,
    role
FROM users 
GROUP BY role;

-- ============================================================
-- STEP 3: Fix payments table
-- ============================================================

-- Fix payment_method column (THIS IS THE MAIN ISSUE)
ALTER TABLE payments
MODIFY COLUMN payment_method VARCHAR(20) NOT NULL;

-- Fix status column
ALTER TABLE payments
MODIFY COLUMN status VARCHAR(10) NOT NULL DEFAULT 'PENDING';

-- Fix refund_status column (can be NULL)
ALTER TABLE payments
MODIFY COLUMN refund_status VARCHAR(20);

-- ============================================================
-- STEP 4: Fix orders table
-- ============================================================

-- Fix status column
ALTER TABLE orders
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLACED';

-- ============================================================
-- STEP 5: Fix users table
-- ============================================================

-- Fix role column
ALTER TABLE users
MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

-- ============================================================
-- STEP 6: Verify all changes
-- ============================================================

-- Verify payments table
DESCRIBE payments;

-- Verify orders table
DESCRIBE orders;

-- Verify users table
DESCRIBE users;

-- Check for any remaining ENUM columns (should be empty)
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_TYPE LIKE 'enum%'
  AND TABLE_NAME IN ('payments', 'orders', 'users');

-- ============================================================
-- Expected results after running this script:
-- ============================================================
-- payments.payment_method → varchar(20) NOT NULL
-- payments.status         → varchar(10) NOT NULL DEFAULT 'PENDING'
-- payments.refund_status  → varchar(20) NULL
-- orders.status           → varchar(20) NOT NULL DEFAULT 'PLACED'
-- users.role              → varchar(20) NOT NULL DEFAULT 'CUSTOMER'
-- ============================================================

-- ============================================================
-- Notes:
-- ============================================================
-- 1. MySQL automatically converts ENUM to VARCHAR - existing data is preserved
-- 2. Java enum constants (e.g., CASH, CARD, UPI) are stored as strings
-- 3. After running this script, redeploy your Render application
-- 4. The schema validation should now pass successfully
-- 5. This fixes the root cause: Schema-validation: wrong column type encountered
-- ============================================================

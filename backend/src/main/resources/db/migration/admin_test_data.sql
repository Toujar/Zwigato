-- ============================================================
-- Admin Dashboard Test Data
-- ============================================================
-- Generates realistic test orders, payments, and reviews for dashboard analytics
-- Run after main data.sql to create historical data for charts and KPIs

-- Create some test orders with various statuses and dates over the past 30 days
INSERT INTO orders (user_id, restaurant_id, delivery_address, status, subtotal, delivery_fee, tax, total_amount, special_instructions, placed_at, updated_at) VALUES

-- Recent orders (last 7 days) - mix of statuses
(6, 1, '5 Indiranagar, Bengaluru', 'DELIVERED', 450.00, 40.00, 22.50, 512.50, 'Extra spicy please', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 2, '12 Jayanagar, Bengaluru', 'DELIVERED', 680.00, 50.00, 34.00, 764.00, null, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 3, '5 Indiranagar, Bengaluru', 'OUT_FOR_DELIVERY', 320.00, 40.00, 16.00, 376.00, null, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(7, 4, '12 Jayanagar, Bengaluru', 'PREPARING', 180.00, 30.00, 9.00, 219.00, 'No onions', NOW(), NOW()),

-- Last week orders
(6, 5, '5 Indiranagar, Bengaluru', 'DELIVERED', 280.00, 45.00, 14.00, 339.00, null, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 6, '12 Jayanagar, Bengaluru', 'DELIVERED', 220.00, 35.00, 11.00, 266.00, 'Less sugar', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 7, '5 Indiranagar, Bengaluru', 'DELIVERED', 390.00, 50.00, 19.50, 459.50, null, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),

-- 2-3 weeks ago
(7, 8, '12 Jayanagar, Bengaluru', 'DELIVERED', 520.00, 70.00, 26.00, 616.00, null, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 9, '5 Indiranagar, Bengaluru', 'DELIVERED', 150.00, 40.00, 7.50, 197.50, null, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(7, 10, '12 Jayanagar, Bengaluru', 'DELIVERED', 340.00, 45.00, 17.00, 402.00, null, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),

-- 3-4 weeks ago
(6, 1, '5 Indiranagar, Bengaluru', 'DELIVERED', 280.00, 50.00, 14.00, 344.00, null, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
(7, 2, '12 Jayanagar, Bengaluru', 'DELIVERED', 650.00, 60.00, 32.50, 742.50, null, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(6, 3, '5 Indiranagar, Bengaluru', 'DELIVERED', 200.00, 40.00, 10.00, 250.00, null, DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),

-- Last month (for growth comparison)
(7, 4, '12 Jayanagar, Bengaluru', 'DELIVERED', 160.00, 30.00, 8.00, 198.00, null, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY)),
(6, 5, '5 Indiranagar, Bengaluru', 'DELIVERED', 310.00, 45.00, 15.50, 370.50, null, DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY)),
(7, 6, '12 Jayanagar, Bengaluru', 'DELIVERED', 180.00, 35.00, 9.00, 224.00, null, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY)),

-- Some cancelled orders for volume metrics
(6, 7, '5 Indiranagar, Bengaluru', 'CANCELLED', 240.00, 50.00, 12.00, 302.00, null, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 8, '12 Jayanagar, Bengaluru', 'CANCELLED', 180.00, 40.00, 9.00, 229.00, null, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

-- Create order items for the test orders (linking to existing food items)
-- Order 1 (Recent delivered) - Biryani House
INSERT INTO order_items (order_id, food_item_id, quantity, unit_price, subtotal, size, spice_level, add_ons, special_instructions) VALUES
(1, 1, 1, 280.00, 280.00, 'Large', 'Spicy', null, null),
(1, 4, 2, 60.00, 120.00, null, null, null, null),
(1, 5, 1, 50.00, 50.00, null, null, null, null);

-- Order 2 (Recent delivered) - Pizza Paradise  
INSERT INTO order_items (order_id, food_item_id, quantity, unit_price, subtotal, size, spice_level, add_ons, special_instructions) VALUES
(2, 7, 1, 299.00, 299.00, 'Medium', null, 'Extra cheese', null),
(2, 8, 1, 349.00, 349.00, 'Large', null, null, null),
(2, 11, 1, 32.00, 32.00, null, null, null, null);

-- Continue with a few more order items for testing
INSERT INTO order_items (order_id, food_item_id, quantity, unit_price, subtotal) VALUES
(3, 13, 1, 249.00, 249.00),
(3, 16, 1, 71.00, 71.00),
(4, 17, 1, 89.00, 89.00),
(4, 18, 1, 91.00, 91.00),
(5, 23, 1, 180.00, 180.00),
(5, 24, 2, 50.00, 100.00),
(6, 31, 1, 149.00, 149.00),
(6, 32, 1, 71.00, 71.00),
(7, 37, 1, 270.00, 270.00),
(7, 40, 2, 60.00, 120.00),
(8, 43, 2, 160.00, 320.00),
(8, 46, 1, 200.00, 200.00),
(9, 49, 1, 90.00, 90.00),
(9, 52, 1, 60.00, 60.00),
(10, 55, 1, 300.00, 300.00),
(10, 58, 1, 40.00, 40.00);

-- Create payment records for delivered orders
INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, razorpay_order_id, razorpay_payment_id, paid_at, created_at, updated_at) VALUES
(1, 'UPI', 512.50, 'SUCCESS', 'TXN001', 'order_001', 'pay_001', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'Credit Card', 764.00, 'SUCCESS', 'TXN002', 'order_002', 'pay_002', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 'UPI', 339.00, 'SUCCESS', 'TXN005', 'order_005', 'pay_005', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6, 'Debit Card', 266.00, 'SUCCESS', 'TXN006', 'order_006', 'pay_006', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7, 'UPI', 459.50, 'SUCCESS', 'TXN007', 'order_007', 'pay_007', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, 'Credit Card', 616.00, 'SUCCESS', 'TXN008', 'order_008', 'pay_008', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(9, 'UPI', 197.50, 'SUCCESS', 'TXN009', 'order_009', 'pay_009', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(10, 'UPI', 402.00, 'SUCCESS', 'TXN010', 'order_010', 'pay_010', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY));

-- Add some sample reviews for restaurants (for rating analytics)
INSERT INTO reviews (user_id, order_id, restaurant_id, rating, comment, created_at) VALUES
(6, 1, 1, 5, 'Excellent biryani! Best in town.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 2, 2, 4, 'Great pizzas, fast delivery.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 5, 5, 4, 'Good Chinese food, will order again.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 6, 6, 5, 'Amazing desserts! Perfect sweetness.', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 7, 7, 4, 'Tasty North Indian curry.', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(7, 8, 8, 5, 'Fresh sushi, excellent quality.', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 9, 9, 3, 'Coffee was good but service slow.', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(7, 10, 10, 4, 'Delicious biryani, authentic taste.', DATE_SUB(NOW(), INTERVAL 14 DAY));

-- Add some food item reviews
INSERT INTO reviews (user_id, order_id, food_item_id, rating, comment, created_at) VALUES
(6, 1, 1, 5, 'Perfect Hyderabadi biryani! Authentic flavors.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 2, 7, 4, 'Great margherita, could use more cheese.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 5, 23, 4, 'Hakka noodles were delicious.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 6, 31, 5, 'Best waffle I have had in Bangalore!', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 7, 37, 4, 'Butter chicken was creamy and tasty.', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- Update restaurant ratings based on reviews (simulate the recalculation)
UPDATE restaurants SET rating = 5.0, review_count = 1 WHERE id = 1;
UPDATE restaurants SET rating = 4.0, review_count = 1 WHERE id = 2;
UPDATE restaurants SET rating = 4.0, review_count = 1 WHERE id = 5;
UPDATE restaurants SET rating = 5.0, review_count = 1 WHERE id = 6;
UPDATE restaurants SET rating = 4.0, review_count = 1 WHERE id = 7;
UPDATE restaurants SET rating = 5.0, review_count = 1 WHERE id = 8;
UPDATE restaurants SET rating = 3.0, review_count = 1 WHERE id = 9;
UPDATE restaurants SET rating = 4.0, review_count = 1 WHERE id = 10;

-- Update food item ratings based on reviews
UPDATE food_items SET rating = 5.0, review_count = 1 WHERE id = 1;
UPDATE food_items SET rating = 4.0, review_count = 1 WHERE id = 7;
UPDATE food_items SET rating = 4.0, review_count = 1 WHERE id = 23;
UPDATE food_items SET rating = 5.0, review_count = 1 WHERE id = 31;
UPDATE food_items SET rating = 4.0, review_count = 1 WHERE id = 37;
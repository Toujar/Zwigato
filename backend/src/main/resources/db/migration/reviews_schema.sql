-- ============================================================
-- Reviews Schema Migration
-- ============================================================
-- Adds support for customer reviews on restaurants and food items
-- Rating and review_count fields added to both restaurants and food_items tables

-- Add columns to restaurants table if they don't exist
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;

-- Add columns to food_items table
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS rating DECIMAL(2,1) NOT NULL DEFAULT 0.0;
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;

-- Create reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    food_item_id BIGINT,
    rating INT NOT NULL,
    comment TEXT,
    helpful_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for fast queries
    INDEX idx_reviews_user_id (user_id),
    INDEX idx_reviews_order_id (order_id),
    INDEX idx_reviews_restaurant_id (restaurant_id),
    INDEX idx_reviews_food_item_id (food_item_id),
    INDEX idx_reviews_created_at (created_at),
    
    -- Foreign Keys
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_food_item FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON DELETE CASCADE,
    
    -- Constraint: exactly one of restaurant_id or food_item_id must be set
    CONSTRAINT check_review_target CHECK (
        (restaurant_id IS NOT NULL AND food_item_id IS NULL) OR
        (restaurant_id IS NULL AND food_item_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Prevent duplicate reviews for same order by same user
CREATE UNIQUE INDEX idx_reviews_user_order ON reviews(user_id, order_id);

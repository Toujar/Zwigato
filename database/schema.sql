-- ============================================================
--  Food Delivery Application - MySQL Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS food_delivery_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE food_delivery_db;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)    NOT NULL,
    email         VARCHAR(150)    NOT NULL UNIQUE,
    password      VARCHAR(255)    NOT NULL,
    phone         VARCHAR(15)     NOT NULL UNIQUE,
    address       TEXT,
    role          ENUM(
                    'CUSTOMER',
                    'RESTAURANT_OWNER',
                    'DELIVERY_AGENT',
                    'ADMIN'
                  )               NOT NULL DEFAULT 'CUSTOMER',
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

-- ============================================================
-- 2. CATEGORIES
-- ============================================================
CREATE TABLE categories (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)    NOT NULL UNIQUE,
    description   VARCHAR(255),
    image_url     VARCHAR(500),
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

-- ============================================================
-- 3. RESTAURANTS
-- ============================================================
CREATE TABLE restaurants (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT          NOT NULL,               -- FK -> users.id
    name            VARCHAR(150)    NOT NULL,
    description     TEXT,
    address         TEXT            NOT NULL,
    city            VARCHAR(100)    NOT NULL,
    phone           VARCHAR(15)     NOT NULL,
    email           VARCHAR(150),
    image_url       VARCHAR(500),
    rating          DECIMAL(2,1)    NOT NULL DEFAULT 0.0,
    delivery_time   INT             NOT NULL DEFAULT 30,    -- minutes
    min_order_amount DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    is_open         BOOLEAN         NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_restaurant_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================
-- 4. FOOD ITEMS
-- ============================================================
CREATE TABLE food_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    restaurant_id   BIGINT          NOT NULL,               -- FK -> restaurants.id
    category_id     BIGINT          NOT NULL,               -- FK -> categories.id
    name            VARCHAR(150)    NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2)   NOT NULL,
    image_url       VARCHAR(500),
    is_vegetarian   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_food_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_food_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================
-- 5. CART
-- ============================================================
CREATE TABLE carts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL UNIQUE,        -- FK -> users.id  (1 cart per user)
    restaurant_id   BIGINT,                                 -- FK -> restaurants.id (locked to one restaurant)
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- ============================================================
-- 6. CART ITEMS
-- ============================================================
CREATE TABLE cart_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    cart_id         BIGINT          NOT NULL,               -- FK -> carts.id
    food_item_id    BIGINT          NOT NULL,               -- FK -> food_items.id
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(10,2)   NOT NULL,               -- snapshot of price at add time
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_cart_food (cart_id, food_item_id),        -- prevent duplicate rows per item
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_item_food
        FOREIGN KEY (food_item_id) REFERENCES food_items (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- 7. ORDERS
-- ============================================================
CREATE TABLE orders (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,           -- FK -> users.id
    restaurant_id       BIGINT          NOT NULL,           -- FK -> restaurants.id
    delivery_agent_id   BIGINT,                             -- FK -> users.id (assigned later)
    delivery_address    TEXT            NOT NULL,
    status              ENUM(
                          'PLACED',
                          'CONFIRMED',
                          'PREPARING',
                          'OUT_FOR_DELIVERY',
                          'DELIVERED',
                          'CANCELLED'
                        )               NOT NULL DEFAULT 'PLACED',
    subtotal            DECIMAL(10,2)   NOT NULL,
    delivery_fee        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    tax                 DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(10,2)   NOT NULL,
    special_instructions TEXT,
    placed_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_agent
        FOREIGN KEY (delivery_agent_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- ============================================================
-- 8. ORDER ITEMS
-- ============================================================
CREATE TABLE order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,               -- FK -> orders.id
    food_item_id    BIGINT          NOT NULL,               -- FK -> food_items.id
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(10,2)   NOT NULL,               -- price snapshot at order time
    subtotal        DECIMAL(10,2)   NOT NULL,               -- quantity * unit_price

    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_item_food
        FOREIGN KEY (food_item_id) REFERENCES food_items (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================
-- 9. PAYMENTS
-- ============================================================
CREATE TABLE payments (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_id            BIGINT          NOT NULL UNIQUE,    -- FK -> orders.id (1 payment per order)
    amount              DECIMAL(10,2)   NOT NULL,
    payment_method      ENUM(
                          'CREDIT_CARD',
                          'DEBIT_CARD',
                          'UPI',
                          'NET_BANKING',
                          'WALLET',
                          'CASH_ON_DELIVERY'
                        )               NOT NULL,
    status              ENUM(
                          'PENDING',
                          'SUCCESS',
                          'FAILED',
                          'REFUNDED'
                        )               NOT NULL DEFAULT 'PENDING',
    transaction_id      VARCHAR(255)    UNIQUE,             -- from payment gateway
    gateway_response    TEXT,                               -- raw JSON from gateway
    paid_at             DATETIME,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================
-- INDEXES (for query performance)
-- ============================================================
CREATE INDEX idx_food_items_restaurant   ON food_items  (restaurant_id);
CREATE INDEX idx_food_items_category     ON food_items  (category_id);
CREATE INDEX idx_orders_user             ON orders      (user_id);
CREATE INDEX idx_orders_restaurant       ON orders      (restaurant_id);
CREATE INDEX idx_orders_status           ON orders      (status);
CREATE INDEX idx_orders_agent            ON orders      (delivery_agent_id);
CREATE INDEX idx_order_items_order       ON order_items (order_id);
CREATE INDEX idx_cart_items_cart         ON cart_items  (cart_id);
CREATE INDEX idx_restaurants_city        ON restaurants (city);
CREATE INDEX idx_restaurants_owner       ON restaurants (owner_id);

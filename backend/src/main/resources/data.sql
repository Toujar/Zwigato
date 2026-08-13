-- ============================================================
--  Food Delivery App — Bootstrap Seed Data
--  Run AFTER schema.sql  |  Passwords are BCrypt of "Password@1"
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE payments;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE carts;
TRUNCATE TABLE food_items;
TRUNCATE TABLE restaurants;
TRUNCATE TABLE categories;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. USERS
-- ============================================================
-- Password for all users: Password@1
INSERT INTO users (name, email, password, phone, address, role, is_active) VALUES
('Admin User',    'admin@zwigato.com',   '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000001', '1 Admin HQ, Bengaluru',        'ADMIN',             TRUE),
('Raj Sharma',    'raj@owner.com',       '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000002', '42 MG Road, Bengaluru',        'RESTAURANT_OWNER',  TRUE),
('Priya Menon',   'priya@owner.com',     '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000003', '15 Koramangala, Bengaluru',    'RESTAURANT_OWNER',  TRUE),
('Arjun Kumar',   'agent1@zwigato.com',  '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000004', '88 HSR Layout, Bengaluru',     'DELIVERY_AGENT',    TRUE),
('Sneha Nair',    'sneha@zwigato.com',   '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000005', '22 Whitefield, Bengaluru',     'DELIVERY_AGENT',    TRUE),
('Rahul Verma',   'rahul@customer.com',  '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000006', '5 Indiranagar, Bengaluru',     'CUSTOMER',          TRUE),
('Anjali Singh',  'anjali@customer.com', '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000007', '12 Jayanagar, Bengaluru',      'CUSTOMER',          TRUE),
('Vikram Patel',  'vikram@owner.com',    '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000008', '8 Commercial Street, Bengaluru', 'RESTAURANT_OWNER',  TRUE),
('Meera Gupta',   'meera@owner.com',     '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000009', '25 Banaswadi, Bengaluru',      'RESTAURANT_OWNER',  TRUE);

-- ============================================================
-- 2. CATEGORIES
-- ============================================================
INSERT INTO categories (name, description, image_url, is_active) VALUES
('Biryani',      'Fragrant rice dishes',                  'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=400&h=300&fit=crop', TRUE),
('Pizza',        'Wood-fired and pan pizzas',             'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop', TRUE),
('Burgers',      'Juicy stacked burgers',                 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=300&fit=crop', TRUE),
('South Indian', 'Dosas, idlis, vadas & more',           'https://images.unsplash.com/photo-1630383249896-424e482df921?w=400&h=300&fit=crop', TRUE),
('Chinese',      'Indo-Chinese street flavours',         'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=400&h=300&fit=crop', TRUE),
('Desserts',     'Ice creams, cakes & sweets',           'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=400&h=300&fit=crop', TRUE),
('Salads',       'Fresh & healthy greens',               'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop', TRUE),
('Pasta',        'Italian comfort classics',             'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=400&h=300&fit=crop', TRUE),
('North Indian', 'Curries and breads',                   'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop', TRUE),
('Drinks',       'Juices, shakes & beverages',           'https://images.unsplash.com/photo-1575470543650-f59c947ab581?w=400&h=300&fit=crop', TRUE);

-- ============================================================
-- 3. RESTAURANTS
--    With latitude/longitude, operating_hours (JSON), delivery_fee, delivery_radius
--    Bengaluru coordinates: 12.9716°N, 77.5946°E (city center)
--    owner_id: 2=Raj, 3=Priya, 8=Vikram, 9=Meera
-- ============================================================
INSERT INTO restaurants (owner_id, name, description, address, city, phone, email, image_url, rating, delivery_time, min_order_amount, delivery_fee, delivery_radius, latitude, longitude, operating_hours, is_open, is_active) VALUES

(2, 'Biryani House',   'Best Hyderabadi biryani in town',        'MG Road, Bengaluru, Karnataka 560001',                   'Bengaluru', '9811000001', 'info@biryanihouse.com',  'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=800&h=400&fit=crop', 4.5, 35, 150.00, 50.00, 5, 12.9716, 77.5946, '{"monday":"11:00-23:00","tuesday":"11:00-23:00","wednesday":"11:00-23:00","thursday":"11:00-23:00","friday":"10:00-00:00","saturday":"10:00-00:00","sunday":"10:00-23:00"}', TRUE, TRUE),

(2, 'Pizza Paradise',  'Authentic Italian wood-fired pizzas',    'Koramangala 5th Block, Bengaluru, Karnataka 560095',     'Bengaluru', '9811000002', 'info@pizzaparadise.com', 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&h=400&fit=crop', 4.3, 40, 200.00, 60.00, 6, 12.9352, 77.6245, '{"monday":"12:00-23:00","tuesday":"12:00-23:00","wednesday":"12:00-23:00","thursday":"12:00-23:00","friday":"11:00-00:00","saturday":"11:00-00:00","sunday":"12:00-23:00"}', TRUE, TRUE),

(3, 'The Burger Lab',  'Craft burgers with premium ingredients', '100 Feet Road, Indiranagar, Bengaluru, Karnataka 560038','Bengaluru', '9811000003', 'hello@burgerlab.com',    'https://images.unsplash.com/photo-1550547660-d9450f859349?w=800&h=400&fit=crop', 4.4, 30, 120.00, 40.00, 5, 12.9716, 77.6412, '{"monday":"09:00-23:00","tuesday":"09:00-23:00","wednesday":"09:00-23:00","thursday":"09:00-23:00","friday":"08:00-00:00","saturday":"08:00-00:00","sunday":"09:00-23:00"}', TRUE, TRUE),

(3, 'Dosa Corner',     'Traditional South Indian breakfasts',   'Gandhi Bazaar, Basavanagudi, Bengaluru, Karnataka 560004','Bengaluru', '9811000004', 'dosa@corner.com',        'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&h=400&fit=crop', 4.6, 25, 80.00,  30.00, 4, 12.9489, 77.5700, '{"monday":"06:00-22:00","tuesday":"06:00-22:00","wednesday":"06:00-22:00","thursday":"06:00-22:00","friday":"06:00-23:00","saturday":"06:00-23:00","sunday":"07:00-22:00"}', TRUE, TRUE),

(2, 'Dragon Wok',      'Indo-Chinese street-style wok cooking',  'Jayanagar 4th Block, Bengaluru, Karnataka 560011',       'Bengaluru', '9811000005', 'contact@dragonwok.com',  'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&h=400&fit=crop', 4.1, 30, 100.00, 45.00, 5, 12.9334, 77.5965, '{"monday":"11:00-23:00","tuesday":"11:00-23:00","wednesday":"11:00-23:00","thursday":"11:00-23:00","friday":"10:00-00:00","saturday":"10:00-00:00","sunday":"11:00-23:00"}', TRUE, TRUE),

(3, 'Sweet Surrender', 'Artisan desserts and ice cream parlour', 'Lavelle Road, Bengaluru, Karnataka 560001',              'Bengaluru', '9811000006', 'sweets@surrender.com',   'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=800&h=400&fit=crop', 4.7, 20, 60.00,  35.00, 4, 12.9716, 77.5946, '{"monday":"11:00-23:00","tuesday":"11:00-23:00","wednesday":"11:00-23:00","thursday":"11:00-23:00","friday":"10:00-00:00","saturday":"10:00-00:00","sunday":"10:00-23:00"}', TRUE, TRUE),

(8, 'Spice Route',     'North Indian curries and breads',       'Commercial Street, Bengaluru, Karnataka 560001',         'Bengaluru', '9811000007', 'hello@spiceroute.com',   'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&h=400&fit=crop', 4.2, 35, 130.00, 50.00, 5, 12.9716, 77.5850, '{"monday":"12:00-23:00","tuesday":"12:00-23:00","wednesday":"12:00-23:00","thursday":"12:00-23:00","friday":"11:00-23:30","saturday":"11:00-23:30","sunday":"12:00-23:00"}', TRUE, TRUE),

(8, 'Sushi Central',   'Fresh sushi and Japanese cuisine',      'UB City, Bangalore, Karnataka 560001',                   'Bengaluru', '9811000008', 'info@sushicentral.com',  'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=800&h=400&fit=crop', 4.6, 40, 300.00, 70.00, 6, 12.9716, 77.6046, '{"monday":"12:00-22:00","tuesday":"12:00-22:00","wednesday":"12:00-22:00","thursday":"12:00-22:00","friday":"11:00-23:00","saturday":"11:00-23:00","sunday":"12:00-22:00"}', TRUE, TRUE),

(9, 'Café Aurora',     'Coffee, pastries & light meals',        'Indiranagar, Bengaluru, Karnataka 560038',               'Bengaluru', '9811000009', 'contact@cafeaurora.com', 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800&h=400&fit=crop', 4.3, 25, 100.00, 40.00, 4, 12.9716, 77.6412, '{"monday":"07:00-21:00","tuesday":"07:00-21:00","wednesday":"07:00-21:00","thursday":"07:00-21:00","friday":"07:00-22:00","saturday":"08:00-22:00","sunday":"08:00-21:00"}', TRUE, TRUE),

(9, 'Biryani Delights', 'Lucknowi & Karachi biryani speciality', 'Banaswadi, Bengaluru, Karnataka 560033',                 'Bengaluru', '9811000010', 'info@biryanidelights.com', 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=800&h=400&fit=crop', 4.4, 35, 140.00, 45.00, 5, 12.9600, 77.6100, '{"monday":"11:00-23:30","tuesday":"11:00-23:30","wednesday":"11:00-23:30","thursday":"11:00-23:30","friday":"10:00-00:00","saturday":"10:00-00:00","sunday":"11:00-23:30"}', TRUE, TRUE);

-- ============================================================
-- 4. FOOD ITEMS (Extended with more dishes)
--    restaurant_id: 1=BiryaniHouse 2=PizzaParadise 3=BurgerLab 4=DosaCorner 5=DragonWok 6=SweetSurrender 7=SpiceRoute 8=SushiCentral 9=CafeAurora 10=BiryaniDelights
--    category_id:   1=Biryani 2=Pizza 3=Burgers 4=SouthIndian 5=Chinese 6=Desserts 7=Salads 8=Pasta 9=NorthIndian 10=Drinks
-- ============================================================

-- Biryani House (id=1)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(1, 1, 'Hyderabadi Chicken Biryani', 'Slow-cooked dum biryani with tender chicken pieces', 280.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', FALSE, TRUE),
(1, 1, 'Mutton Biryani',             'Rich mutton dum biryani with caramelised onions',   340.00, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&h=400&fit=crop', FALSE, TRUE),
(1, 1, 'Veg Biryani',                'Seasonal vegetables with fragrant basmati rice',    200.00, 'https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=500&h=400&fit=crop', TRUE,  TRUE),
(1, 4, 'Raita',                      'Cool cucumber and boondi yoghurt side',              60.00, 'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=500&h=400&fit=crop', TRUE,  TRUE),
(1, 6, 'Double Ka Meetha',           'Traditional Hyderabadi bread pudding dessert',       90.00, 'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=500&h=400&fit=crop', TRUE,  TRUE),
(1, 1, 'Prawn Biryani',              'Aromatic rice with succulent prawns',               320.00, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&h=400&fit=crop', FALSE, TRUE);

-- Pizza Paradise (id=2)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(2, 2, 'Margherita Pizza',            'San Marzano tomatoes, buffalo mozzarella, fresh basil', 299.00, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 2, 'Pepperoni Feast',             'Double pepperoni with cheddar on sourdough base',        349.00, 'https://images.unsplash.com/photo-1628840042765-356cda07504e?w=500&h=400&fit=crop', FALSE, TRUE),
(2, 2, 'BBQ Chicken Pizza',           'Smoky BBQ base, grilled chicken, red onion',             369.00, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=500&h=400&fit=crop', FALSE, TRUE),
(2, 8, 'Penne Arrabbiata',            'Penne in spicy tomato-chilli sauce',                     220.00, 'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 6, 'Tiramisu',                    'Classic Italian coffee-mascarpone dessert',              180.00, 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 2, 'Quattro Formaggi',            'Four cheese blend pizza with truffle oil',               399.00, 'https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 7, 'Caesar Salad',                'Romaine lettuce, parmesan, croutons',                    149.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE);

-- The Burger Lab (id=3)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(3, 3, 'Classic Smash Burger',        'Double smashed patty, American cheese, pickles',         249.00, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&h=400&fit=crop', FALSE, TRUE),
(3, 3, 'Crispy Chicken Burger',       'Southern-fried chicken thigh, slaw, sriracha mayo',      229.00, 'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=500&h=400&fit=crop', FALSE, TRUE),
(3, 3, 'Veg Patty Burger',            'Black-bean patty, avocado spread, lettuce, tomato',      199.00, 'https://images.unsplash.com/photo-1550547660-d9450f859349?w=500&h=400&fit=crop', TRUE,  TRUE),
(3, 7, 'Garden Salad',                'Mixed greens, cherry tomatoes, balsamic vinaigrette',    149.00, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&h=400&fit=crop', TRUE,  TRUE),
(3, 6, 'Brownie Sundae',              'Warm brownie with vanilla ice cream and caramel',        159.00, 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=500&h=400&fit=crop', TRUE,  TRUE),
(3, 3, 'Bacon Cheeseburger',          'Crispy bacon, melted cheddar, special sauce',            259.00, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&h=400&fit=crop', FALSE, TRUE);

-- Dosa Corner (id=4)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(4, 4, 'Masala Dosa',                 'Crisp rice crepe stuffed with spiced potato filling',     89.00, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Rava Idli Combo (3 pcs)',     'Semolina idlis with sambar and two chutneys',             79.00, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Medu Vada (2 pcs)',           'Crunchy lentil doughnuts with coconut chutney',           65.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Uttapam (Onion-Tomato)',      'Thick rice pancake topped with onion and tomato',         95.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 10, 'Filter Coffee',              'Authentic South Indian drip coffee with milk froth',      40.00, 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Cheese Dosa',                 'Crisp dosa filled with potato and melted cheese',         120.00, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Dragon Wok (id=5)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(5, 5, 'Chicken Hakka Noodles',       'Wok-tossed noodles with chicken and vegetables',         180.00, 'https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500&h=400&fit=crop', FALSE, TRUE),
(5, 5, 'Veg Fried Rice',              'Classic Indo-Chinese fried rice with seasonal veggies',  150.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&h=400&fit=crop', TRUE,  TRUE),
(5, 5, 'Chilli Paneer (Dry)',         'Crispy paneer cubes tossed in fiery chilli sauce',       190.00, 'https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=500&h=400&fit=crop', TRUE,  TRUE),
(5, 5, 'Chicken Manchurian Gravy',    'Tender chicken balls in thick Manchurian gravy',         210.00, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500&h=400&fit=crop', FALSE, TRUE),
(5, 5, 'Dimsums (Steamed, 6 pcs)',    'Steamed vegetable and prawn dumplings',                  160.00, 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&h=400&fit=crop', FALSE, TRUE),
(5, 5, 'Biryani Fried Rice',          'Fragrant biryani-spiced fried rice with chicken',        190.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&h=400&fit=crop', FALSE, TRUE);

-- Sweet Surrender (id=6)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(6, 6, 'Belgian Waffle',              'Crisp waffle with strawberries and whipped cream',       149.00, 'https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Chocolate Lava Cake',         'Warm cake with molten dark-chocolate centre',            169.00, 'https://images.unsplash.com/photo-1564355808539-22fda35bed7e?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Mango Kulfi',                 'Traditional frozen dessert with alphonso mango',          79.00, 'https://images.unsplash.com/photo-1501443762994-82bd5dace89a?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Cheesecake Slice',            'New York-style cheesecake with berry compote',           189.00, 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Gulab Jamun (4 pcs)',         'Soft milk-solid balls soaked in rose sugar syrup',        60.00, 'https://images.unsplash.com/photo-1606471191009-63994c53433b?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Pistachio Ice Cream',         'Creamy pistachio gelato',                                89.00, 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Spice Route (id=7)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(7, 9, 'Butter Chicken',              'Tender chicken in creamy tomato butter sauce',           270.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', FALSE, TRUE),
(7, 9, 'Dal Makhani',                 'Slow-cooked black lentils with cream and butter',        180.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE),
(7, 9, 'Paneer Tikka Masala',         'Grilled paneer in spiced tomato gravy',                  240.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE),
(7, 9, 'Naan (Plain)',                'Freshly baked tandoori bread',                           40.00,  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE),
(7, 9, 'Garlic Naan',                 'Naan brushed with garlic butter',                        50.00,  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE),
(7, 10, 'Mango Lassi',                'Yoghurt-based mango smoothie',                           60.00,  'https://images.unsplash.com/photo-1553530666-ba2a8e36cd12?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Sushi Central (id=8)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(8, 1, 'Salmon Nigiri (6 pcs)',       'Fresh salmon atop seasoned sushi rice',                  280.00, 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=500&h=400&fit=crop', FALSE, TRUE),
(8, 1, 'California Roll (8 pcs)',     'Crab, avocado, cucumber in sushi rice',                  220.00, 'https://images.unsplash.com/photo-1553621042-f6e147245754?w=500&h=400&fit=crop', FALSE, TRUE),
(8, 1, 'Vegetable Roll (8 pcs)',      'Fresh cucumber, avocado, and carrot roll',               160.00, 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=500&h=400&fit=crop', TRUE,  TRUE),
(8, 5, 'Edamame',                     'Steamed young soy beans with sea salt',                   120.00, 'https://images.unsplash.com/photo-1599599810694-b5ac4dd11b10?w=500&h=400&fit=crop', TRUE,  TRUE),
(8, 10, 'Sake (250ml)',               'Premium Japanese rice wine',                              250.00, 'https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=500&h=400&fit=crop', TRUE,  TRUE),
(8, 6, 'Green Tea Cheesecake',        'Light green tea cheesecake',                              199.00, 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Café Aurora (id=9)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(9, 10, 'Espresso',                   'Strong single or double shot',                            90.00,  'https://images.unsplash.com/photo-1559056199-641a0ac8b3f4?w=500&h=400&fit=crop', TRUE,  TRUE),
(9, 10, 'Cappuccino',                 'Coffee with steamed milk and foam',                       120.00, 'https://images.unsplash.com/photo-1559056199-641a0ac8b3f4?w=500&h=400&fit=crop', TRUE,  TRUE),
(9, 6, 'Croissant',                   'Butter croissant pastry',                                 80.00,  'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500&h=400&fit=crop', TRUE,  TRUE),
(9, 6, 'Chocolate Muffin',            'Rich chocolate muffin',                                   70.00,  'https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&h=400&fit=crop', TRUE,  TRUE),
(9, 4, 'Avocado Toast',               'Toasted bread with mashed avocado',                       140.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', TRUE,  TRUE),
(9, 10, 'Iced Mocha',                 'Iced coffee with chocolate and milk',                     130.00, 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Biryani Delights (id=10)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(10, 1, 'Lucknowi Biryani',           'Royal Lucknowi style layered rice and meat',             300.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', FALSE, TRUE),
(10, 1, 'Karachi Biryani',            'Traditional Karachi biryani with potatoes',              320.00, 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=500&h=400&fit=crop', FALSE, TRUE),
(10, 1, 'Coloured Veg Biryani',       'Multi-coloured biryani with mixed vegetables',           240.00, 'https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=500&h=400&fit=crop', TRUE,  TRUE),
(10, 9, 'Haleem',                     'Slow-cooked meat and lentil stew',                        220.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', FALSE, TRUE),
(10, 9, 'Nihari',                     'Spiced slow-cooked meat curry',                           250.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop', FALSE, TRUE),
(10, 9, 'Papadum (Pack of 4)',        'Crispy lentil wafers',                                    30.00,  'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', TRUE,  TRUE);

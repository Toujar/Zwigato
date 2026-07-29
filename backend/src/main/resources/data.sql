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
('Anjali Singh',  'anjali@customer.com', '$2a$12$KmnKLPkgsISxXdwn9q/N3OwqazdMqhjWPvpQlM48C0TvGaQhDNQFO', '9000000007', '12 Jayanagar, Bengaluru',      'CUSTOMER',          TRUE);

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
('Pasta',        'Italian comfort classics',             'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=400&h=300&fit=crop', TRUE);

-- ============================================================
-- 3. RESTAURANTS  (owner_id 2 = Raj, 3 = Priya)
-- ============================================================
INSERT INTO restaurants (owner_id, name, description, address, city, phone, email, image_url, rating, delivery_time, min_order_amount, is_open, is_active) VALUES
(2, 'Biryani House',        'Best Hyderabadi biryani in town',          '12 Church St, MG Road',      'Bengaluru', '9811000001', 'info@biryanihouse.com',  'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=800&h=400&fit=crop', 4.5, 35, 150.00, TRUE, TRUE),
(2, 'Pizza Paradise',       'Authentic Italian wood-fired pizzas',      '34 Koramangala 5th Block',   'Bengaluru', '9811000002', 'info@pizzaparadise.com', 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&h=400&fit=crop', 4.3, 40, 200.00, TRUE, TRUE),
(3, 'The Burger Lab',       'Craft burgers with premium ingredients',   '7 Indiranagar 100ft Road',   'Bengaluru', '9811000003', 'hello@burgerlab.com',    'https://images.unsplash.com/photo-1550547660-d9450f859349?w=800&h=400&fit=crop', 4.4, 30, 120.00, TRUE, TRUE),
(3, 'Dosa Corner',          'Traditional South Indian breakfasts',      '22 Basavanagudi, Gandhi Bazar','Bengaluru','9811000004', 'dosa@corner.com',        'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&h=400&fit=crop', 4.6, 25, 80.00,  TRUE, TRUE),
(2, 'Dragon Wok',           'Indo-Chinese street-style wok cooking',    '55 Jayanagar 4th Block',     'Bengaluru', '9811000005', 'contact@dragonwok.com',  'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&h=400&fit=crop', 4.1, 30, 100.00, TRUE, TRUE),
(3, 'Sweet Surrender',      'Artisan desserts and ice cream parlour',   '3 Lavelle Road, Central',    'Bengaluru', '9811000006', 'sweets@surrender.com',   'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=800&h=400&fit=crop', 4.7, 20, 60.00,  TRUE, TRUE);

-- ============================================================
-- 4. FOOD ITEMS
--    restaurant_id: 1=BiryaniHouse 2=PizzaParadise 3=BurgerLab 4=DosaCorner 5=DragonWok 6=SweetSurrender
--    category_id:   1=Biryani 2=Pizza 3=Burgers 4=SouthIndian 5=Chinese 6=Desserts 7=Salads 8=Pasta
-- ============================================================

-- Biryani House (id=1)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(1, 1, 'Hyderabadi Chicken Biryani', 'Slow-cooked dum biryani with tender chicken pieces', 280.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', FALSE, TRUE),
(1, 1, 'Mutton Biryani',             'Rich mutton dum biryani with caramelised onions',   340.00, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&h=400&fit=crop', FALSE, TRUE),
(1, 1, 'Veg Biryani',                'Seasonal vegetables with fragrant basmati rice',    200.00, 'https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=500&h=400&fit=crop', TRUE,  TRUE),
(1, 4, 'Raita',                      'Cool cucumber and boondi yoghurt side',              60.00, 'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=500&h=400&fit=crop', TRUE,  TRUE),
(1, 6, 'Double Ka Meetha',           'Traditional Hyderabadi bread pudding dessert',       90.00, 'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Pizza Paradise (id=2)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(2, 2, 'Margherita Pizza',            'San Marzano tomatoes, buffalo mozzarella, fresh basil', 299.00, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 2, 'Pepperoni Feast',             'Double pepperoni with cheddar on sourdough base',        349.00, 'https://images.unsplash.com/photo-1628840042765-356cda07504e?w=500&h=400&fit=crop', FALSE, TRUE),
(2, 2, 'BBQ Chicken Pizza',           'Smoky BBQ base, grilled chicken, red onion',             369.00, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=500&h=400&fit=crop', FALSE, TRUE),
(2, 8, 'Penne Arrabbiata',            'Penne in spicy tomato-chilli sauce',                     220.00, 'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=500&h=400&fit=crop', TRUE,  TRUE),
(2, 6, 'Tiramisu',                    'Classic Italian coffee-mascarpone dessert',              180.00, 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Burger Lab (id=3)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(3, 3, 'Classic Smash Burger',        'Double smashed patty, American cheese, pickles',         249.00, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&h=400&fit=crop', FALSE, TRUE),
(3, 3, 'Crispy Chicken Burger',       'Southern-fried chicken thigh, slaw, sriracha mayo',      229.00, 'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=500&h=400&fit=crop', FALSE, TRUE),
(3, 3, 'Veg Patty Burger',            'Black-bean patty, avocado spread, lettuce, tomato',      199.00, 'https://images.unsplash.com/photo-1550547660-d9450f859349?w=500&h=400&fit=crop', TRUE,  TRUE),
(3, 7, 'Garden Salad',                'Mixed greens, cherry tomatoes, balsamic vinaigrette',    149.00, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&h=400&fit=crop', TRUE,  TRUE),
(3, 6, 'Brownie Sundae',              'Warm brownie with vanilla ice cream and caramel',        159.00, 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Dosa Corner (id=4)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(4, 4, 'Masala Dosa',                 'Crisp rice crepe stuffed with spiced potato filling',     89.00, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Rava Idli Combo (3 pcs)',     'Semolina idlis with sambar and two chutneys',             79.00, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Medu Vada (2 pcs)',           'Crunchy lentil doughnuts with coconut chutney',           65.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Uttapam (Onion-Tomato)',      'Thick rice pancake topped with onion and tomato',         95.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500&h=400&fit=crop', TRUE,  TRUE),
(4, 4, 'Filter Coffee',               'Authentic South Indian drip coffee with milk froth',      40.00, 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&h=400&fit=crop', TRUE,  TRUE);

-- Dragon Wok (id=5)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(5, 5, 'Chicken Hakka Noodles',       'Wok-tossed noodles with chicken and vegetables',         180.00, 'https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500&h=400&fit=crop', FALSE, TRUE),
(5, 5, 'Veg Fried Rice',              'Classic Indo-Chinese fried rice with seasonal veggies',  150.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&h=400&fit=crop', TRUE,  TRUE),
(5, 5, 'Chilli Paneer (Dry)',         'Crispy paneer cubes tossed in fiery chilli sauce',       190.00, 'https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=500&h=400&fit=crop', TRUE,  TRUE),
(5, 5, 'Chicken Manchurian Gravy',    'Tender chicken balls in thick Manchurian gravy',         210.00, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500&h=400&fit=crop', FALSE, TRUE),
(5, 5, 'Dimsums (Steamed, 6 pcs)',    'Steamed vegetable and prawn dumplings',                  160.00, 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&h=400&fit=crop', FALSE, TRUE);

-- Sweet Surrender (id=6)
INSERT INTO food_items (restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available) VALUES
(6, 6, 'Belgian Waffle',              'Crisp waffle with strawberries and whipped cream',       149.00, 'https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Chocolate Lava Cake',         'Warm cake with molten dark-chocolate centre',            169.00, 'https://images.unsplash.com/photo-1564355808539-22fda35bed7e?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Mango Kulfi',                 'Traditional frozen dessert with alphonso mango',          79.00, 'https://images.unsplash.com/photo-1501443762994-82bd5dace89a?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Cheesecake Slice',            'New York-style cheesecake with berry compote',           189.00, 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=500&h=400&fit=crop', TRUE,  TRUE),
(6, 6, 'Gulab Jamun (4 pcs)',         'Soft milk-solid balls soaked in rose sugar syrup',        60.00, 'https://images.unsplash.com/photo-1606471191009-63994c53433b?w=500&h=400&fit=crop', TRUE,  TRUE);

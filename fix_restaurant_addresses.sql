-- ============================================================
--  Fix Restaurant Addresses for Nominatim Geocoding
--  Run this in MySQL Workbench or mysql CLI:
--    mysql -u root -p food_delivery_db < fix_restaurant_addresses.sql
-- ============================================================

USE food_delivery_db;

-- These addresses use well-known Bengaluru area names + pincode
-- that Nominatim (OpenStreetMap) resolves accurately.

UPDATE restaurants
SET address = 'MG Road, Bengaluru, Karnataka 560001'
WHERE name = 'Biryani House';

UPDATE restaurants
SET address = 'Koramangala 5th Block, Bengaluru, Karnataka 560095'
WHERE name = 'Pizza Paradise';

UPDATE restaurants
SET address = '100 Feet Road, Indiranagar, Bengaluru, Karnataka 560038'
WHERE name = 'The Burger Lab';

UPDATE restaurants
SET address = 'Gandhi Bazaar, Basavanagudi, Bengaluru, Karnataka 560004'
WHERE name = 'Dosa Corner';

UPDATE restaurants
SET address = 'Jayanagar 4th Block, Bengaluru, Karnataka 560011'
WHERE name = 'Dragon Wok';

UPDATE restaurants
SET address = 'Lavelle Road, Bengaluru, Karnataka 560001'
WHERE name = 'Sweet Surrender';

-- Verify
SELECT id, name, address, city FROM restaurants ORDER BY id;

-- =====================================================================
-- Hotel Room Reservation System - MySQL Workbench helper script
-- Open in MySQL Workbench (File > Open SQL Script) and run the parts you need
-- with the lightning-bolt button. The application creates the TABLES itself
-- (src/main/resources/schema.sql); this script is optional.
-- =====================================================================

-- 1) Create the database (the app also does this automatically)
CREATE DATABASE IF NOT EXISTS hotel_reservation
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 2) OPTIONAL: a dedicated login for the app instead of root.
--    Choose your own password, then put the same username/password in
--    application-local.properties.
-- CREATE USER IF NOT EXISTS 'hotel_app'@'localhost' IDENTIFIED BY 'choose-a-password';
-- GRANT ALL PRIVILEGES ON hotel_reservation.* TO 'hotel_app'@'localhost';
-- FLUSH PRIVILEGES;

-- 3) After starting the application once, look at ALL the data.
USE hotel_reservation;
SHOW FULL TABLES;                       -- 6 tables + 2 views

-- 3a) Complete booking details (reservation + guest + room) - one row per booking
SELECT * FROM v_booking_details ORDER BY check_in;

-- 3b) Complete room details (type-specific columns + amenities + number of bookings)
SELECT * FROM v_room_details ORDER BY room_number;

-- 3c) Logins (passwords are BCrypt hashes, never plain text)
SELECT id, username, full_name, created_at FROM admins;
SELECT g.id AS customer_id, c.full_name, c.email, g.created_at FROM guest_accounts g JOIN customers c ON c.id = g.id;

-- 3d) The raw tables
SELECT * FROM rooms;
SELECT * FROM room_amenities ORDER BY room_id, sort_order;
SELECT * FROM customers;
SELECT * FROM reservations ORDER BY created_at DESC;

-- 3e) Useful filters
SELECT * FROM v_booking_details WHERE status = 'CONFIRMED' AND check_out > CURDATE();   -- current & upcoming stays
SELECT * FROM v_booking_details WHERE guest_email = 'nimal.perera@example.com';         -- one guest's bookings
SELECT room_type, COUNT(*) AS bookings, SUM(total_price) AS revenue
FROM v_booking_details WHERE status = 'CONFIRMED' GROUP BY room_type;                    -- revenue per room type

-- 4) Start again from the sample data (DELETES ALL DATA!):
-- DROP DATABASE hotel_reservation;
-- ...then restart the application.

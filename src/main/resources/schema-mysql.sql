-- =====================================================================
-- MySQL-only additions, run AFTER schema.sql
-- (spring.sql.init.platform=mysql makes Spring load schema-mysql.sql too).
-- Kept separate because the in-memory test database (H2) does not support
-- MySQL functions such as DATEDIFF(date1, date2) and GROUP_CONCAT ... SEPARATOR.
-- =====================================================================

-- Read-only VIEWS for MySQL Workbench / reports.
-- They combine the tables so ONE query shows complete details:
--   SELECT * FROM v_booking_details;
--   SELECT * FROM v_room_details;
-- The Java application itself does not use them.
-- =====================================================================

-- Every reservation with its guest, room and stay details
CREATE OR REPLACE VIEW v_booking_details AS
SELECT r.id                                   AS booking_reference,
       r.status                               AS status,
       c.id                                   AS customer_id,
       c.full_name                            AS guest_name,
       c.email                                AS guest_email,
       c.phone                                AS guest_phone,
       c.address                              AS guest_address,
       rm.id                                  AS room_id,
       rm.room_number                         AS room_number,
       rm.name                                AS room_name,
       rm.room_type                           AS room_type,
       r.check_in                             AS check_in,
       r.check_out                            AS check_out,
       DATEDIFF(r.check_out, r.check_in)      AS nights,
       r.guests                               AS guests,
       rm.base_price                          AS room_base_price,
       r.total_price                          AS total_price,
       r.created_at                           AS booked_at
FROM reservations r
JOIN customers c ON c.id = r.customer_id
JOIN rooms rm    ON rm.id = r.room_id;

-- Every room with its type-specific details and all amenities in one row
CREATE OR REPLACE VIEW v_room_details AS
SELECT rm.id                AS room_id,
       rm.room_number       AS room_number,
       rm.name              AS room_name,
       rm.room_type         AS room_type,
       rm.base_price        AS price_per_night,
       rm.capacity          AS capacity,
       rm.under_maintenance AS under_maintenance,
       rm.bed_type          AS bed_type,
       rm.view_name         AS view_name,
       rm.balcony           AS balcony,
       rm.bedrooms          AS bedrooms,
       rm.butler_service    AS butler_service,
       (SELECT GROUP_CONCAT(a.amenity ORDER BY a.sort_order SEPARATOR ', ')
          FROM room_amenities a
         WHERE a.room_id = rm.id) AS amenities,
       (SELECT COUNT(*) FROM reservations r
         WHERE r.room_id = rm.id AND r.status = 'CONFIRMED') AS confirmed_bookings,
       rm.description       AS description,
       rm.image_url         AS image_url
FROM rooms rm;

-- =====================================================================
-- Hotel Room Reservation System - MySQL schema
-- Run automatically at start-up (spring.sql.init.mode=always).
-- "IF NOT EXISTS" means existing tables and their data are never touched.
-- =====================================================================

-- All three room subclasses are stored in ONE table ("single table inheritance").
-- room_type tells the repository which Java subclass to create;
-- the type-specific columns are NULL for the other types.
CREATE TABLE IF NOT EXISTS rooms (
    id                VARCHAR(10)    NOT NULL,
    room_type         VARCHAR(20)    NOT NULL,
    room_number       VARCHAR(10)    NOT NULL,
    name              VARCHAR(80)    NOT NULL,
    description       VARCHAR(1000)  NOT NULL,
    base_price        DECIMAL(12, 2) NOT NULL,
    capacity          INT            NOT NULL,
    image_url         VARCHAR(300)   NOT NULL,
    under_maintenance BOOLEAN        NOT NULL DEFAULT FALSE,
    bed_type          VARCHAR(30)    NULL,     -- STANDARD only
    view_name         VARCHAR(40)    NULL,     -- DELUXE only ("view" is a reserved word in SQL)
    balcony           BOOLEAN        NULL,     -- DELUXE only
    bedrooms          INT            NULL,     -- SUITE only
    butler_service    BOOLEAN        NULL,     -- SUITE only
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uq_rooms_number UNIQUE (room_number),
    CONSTRAINT ck_rooms_type CHECK (room_type IN ('STANDARD', 'DELUXE', 'SUITE')),
    CONSTRAINT ck_rooms_price CHECK (base_price > 0),
    CONSTRAINT ck_rooms_capacity CHECK (capacity BETWEEN 1 AND 10)
);

-- A room's amenity list (composition: amenities are deleted together with their room).
CREATE TABLE IF NOT EXISTS room_amenities (
    room_id  VARCHAR(10) NOT NULL,
    sort_order INT       NOT NULL,
    amenity  VARCHAR(60) NOT NULL,
    CONSTRAINT pk_room_amenities PRIMARY KEY (room_id, sort_order),
    CONSTRAINT fk_amenities_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customers (
    id        VARCHAR(10)  NOT NULL,
    full_name VARCHAR(80)  NOT NULL,
    email     VARCHAR(120) NOT NULL,
    phone     VARCHAR(30)  NOT NULL,
    address   VARCHAR(200) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

-- Foreign keys stop a room or customer that still has reservations from being deleted.
CREATE TABLE IF NOT EXISTS reservations (
    id          VARCHAR(12)    NOT NULL,
    customer_id VARCHAR(10)    NOT NULL,
    room_id     VARCHAR(10)    NOT NULL,
    check_in    DATE           NOT NULL,
    check_out   DATE           NOT NULL,
    guests      INT            NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    created_at  DATETIME       NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_reservations_dates CHECK (check_out > check_in),
    CONSTRAINT ck_reservations_guests CHECK (guests >= 1),
    CONSTRAINT ck_reservations_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);


-- ---------------------------------------------------------------------
-- Logins. Passwords are stored ONLY as BCrypt hashes, never in plain text.
-- ---------------------------------------------------------------------

-- Hotel staff who can open the admin dashboard (separate from guests).
CREATE TABLE IF NOT EXISTS admins (
    id            VARCHAR(10)  NOT NULL,
    username      VARCHAR(40)  NOT NULL,
    full_name     VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT pk_admins PRIMARY KEY (id),
    CONSTRAINT uq_admins_username UNIQUE (username)
);

-- Online accounts of registered guests: one per customer.
-- The id IS the customer's id; the guest signs in with the customer's email.
CREATE TABLE IF NOT EXISTS guest_accounts (
    id            VARCHAR(10)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT pk_guest_accounts PRIMARY KEY (id),
    CONSTRAINT fk_guest_accounts_customer FOREIGN KEY (id) REFERENCES customers (id) ON DELETE CASCADE
);

-- =====================================================================
-- Smart EV Charging Network Planning and Management System
-- ABC Green Energy Solutions Pvt. Ltd.
-- Script 01 : Database schema (MySQL 8.0+)
-- Normalised to 3NF. Creates the database from scratch.
-- =====================================================================

DROP DATABASE IF EXISTS ev_charging_db;
CREATE DATABASE ev_charging_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_charging_db;

-- ---------------------------------------------------------------------
-- 1. USERS : every login account (Admin, Operator, Customer)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,           -- PBKDF2 "iterations:salt:hash"
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL,
    phone          VARCHAR(15),
    role           ENUM('ADMIN','OPERATOR','CUSTOMER') NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     DATETIME NULL,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. CUSTOMERS : EV owners (1:1 with a CUSTOMER user account)
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    customer_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    city           VARCHAR(50)  NOT NULL,
    address        VARCHAR(255),
    registered_on  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_phone UNIQUE (phone),
    CONSTRAINT uq_customers_user  UNIQUE (user_id),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. VEHICLES : electric vehicles owned by customers
-- ---------------------------------------------------------------------
CREATE TABLE vehicles (
    vehicle_id           INT AUTO_INCREMENT PRIMARY KEY,
    customer_id          INT NOT NULL,
    registration_no      VARCHAR(15) NOT NULL,
    make                 VARCHAR(50) NOT NULL,
    model                VARCHAR(50) NOT NULL,
    vehicle_type         ENUM('CAR','BIKE','SCOOTER','AUTO','BUS') NOT NULL,
    battery_capacity_kwh DECIMAL(6,2) NOT NULL,
    connector_type       ENUM('CCS2','TYPE2','CHADEMO','BHARAT_AC001','BHARAT_DC001') NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vehicles_regno UNIQUE (registration_no),
    CONSTRAINT chk_vehicles_battery CHECK (battery_capacity_kwh > 0),
    CONSTRAINT fk_vehicles_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. STATIONS : charging stations (lat/long kept for future GIS module)
-- ---------------------------------------------------------------------
CREATE TABLE stations (
    station_id      INT AUTO_INCREMENT PRIMARY KEY,
    station_code    VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    city            VARCHAR(50)  NOT NULL,
    address         VARCHAR(255) NOT NULL,
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    operating_hours VARCHAR(30) NOT NULL DEFAULT '00:00-23:59',
    contact_phone   VARCHAR(15),
    status          ENUM('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    opened_on       DATE NOT NULL,
    CONSTRAINT uq_stations_code UNIQUE (station_code)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. CHARGERS : fast (DC) and slow (AC) chargers at each station
-- ---------------------------------------------------------------------
CREATE TABLE chargers (
    charger_id     INT AUTO_INCREMENT PRIMARY KEY,
    station_id     INT NOT NULL,
    charger_code   VARCHAR(25) NOT NULL,
    charger_type   ENUM('FAST','SLOW') NOT NULL,
    connector_type ENUM('CCS2','TYPE2','CHADEMO','BHARAT_AC001','BHARAT_DC001') NOT NULL,
    power_kw       DECIMAL(6,2) NOT NULL,
    price_per_kwh  DECIMAL(6,2) NOT NULL,
    status         ENUM('AVAILABLE','OCCUPIED','FAULTY','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    installed_on   DATE NOT NULL,
    CONSTRAINT uq_chargers_code UNIQUE (charger_code),
    CONSTRAINT chk_chargers_power CHECK (power_kw > 0),
    CONSTRAINT chk_chargers_price CHECK (price_per_kwh > 0),
    CONSTRAINT fk_chargers_station FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. BOOKINGS : charging slot reservations
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT NOT NULL,
    vehicle_id   INT NOT NULL,
    station_id   INT NOT NULL,
    charger_id   INT NULL,
    slot_start   DATETIME NOT NULL,
    slot_end     DATETIME NOT NULL,
    status       ENUM('BOOKED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'BOOKED',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bookings_slot CHECK (slot_end > slot_start),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles(vehicle_id)   ON DELETE CASCADE,
    CONSTRAINT fk_bookings_station  FOREIGN KEY (station_id)  REFERENCES stations(station_id)   ON DELETE CASCADE,
    CONSTRAINT fk_bookings_charger  FOREIGN KEY (charger_id)  REFERENCES chargers(charger_id)   ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_bookings_slot ON bookings (charger_id, slot_start, slot_end);

-- ---------------------------------------------------------------------
-- 7. CHARGING SESSIONS : actual charging events (historical demand source)
-- ---------------------------------------------------------------------
CREATE TABLE charging_sessions (
    session_id       INT AUTO_INCREMENT PRIMARY KEY,
    booking_id       INT NULL,
    customer_id      INT NOT NULL,
    vehicle_id       INT NOT NULL,
    charger_id       INT NOT NULL,
    start_time       DATETIME NOT NULL,
    end_time         DATETIME NULL,
    start_soc        INT NOT NULL,                 -- battery % at start
    end_soc          INT NULL,                     -- battery % at end
    energy_kwh       DECIMAL(8,2) NULL,
    duration_minutes INT NULL,
    cost             DECIMAL(10,2) NULL,           -- energy cost before tax
    status           ENUM('ACTIVE','COMPLETED','ABORTED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_sessions_soc CHECK (start_soc BETWEEN 0 AND 100),
    CONSTRAINT fk_sessions_booking  FOREIGN KEY (booking_id)  REFERENCES bookings(booking_id)   ON DELETE SET NULL,
    CONSTRAINT fk_sessions_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles(vehicle_id)   ON DELETE CASCADE,
    CONSTRAINT fk_sessions_charger  FOREIGN KEY (charger_id)  REFERENCES chargers(charger_id)   ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_sessions_start ON charging_sessions (start_time);
CREATE INDEX idx_sessions_status ON charging_sessions (status);

-- ---------------------------------------------------------------------
-- 8. QUEUE ENTRIES : waiting customers when all chargers are occupied
-- ---------------------------------------------------------------------
CREATE TABLE queue_entries (
    queue_id             INT AUTO_INCREMENT PRIMARY KEY,
    station_id           INT NOT NULL,
    customer_id          INT NOT NULL,
    vehicle_id           INT NOT NULL,
    charger_type         ENUM('FAST','SLOW') NOT NULL,
    requested_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estimated_wait_min   INT NOT NULL DEFAULT 0,
    status               ENUM('WAITING','ALLOCATED','CANCELLED') NOT NULL DEFAULT 'WAITING',
    allocated_charger_id INT NULL,
    allocated_at         DATETIME NULL,
    CONSTRAINT fk_queue_station  FOREIGN KEY (station_id)  REFERENCES stations(station_id)   ON DELETE CASCADE,
    CONSTRAINT fk_queue_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles(vehicle_id)   ON DELETE CASCADE,
    CONSTRAINT fk_queue_charger  FOREIGN KEY (allocated_charger_id) REFERENCES chargers(charger_id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_queue_status ON queue_entries (status, requested_at);

-- ---------------------------------------------------------------------
-- 9. PAYMENTS : bill / invoice for every completed session (simulated gateway)
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    session_id      INT NOT NULL,
    invoice_no      VARCHAR(30) NOT NULL,
    energy_charge   DECIMAL(10,2) NOT NULL,
    service_fee     DECIMAL(10,2) NOT NULL,
    tax_amount      DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    method          ENUM('UPI','CARD','NETBANKING','WALLET','CASH') NULL,
    status          ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(40) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at         DATETIME NULL,
    CONSTRAINT uq_payments_session UNIQUE (session_id),
    CONSTRAINT uq_payments_invoice UNIQUE (invoice_no),
    CONSTRAINT fk_payments_session FOREIGN KEY (session_id) REFERENCES charging_sessions(session_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. MAINTENANCE SCHEDULE
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_schedule (
    maintenance_id   INT AUTO_INCREMENT PRIMARY KEY,
    charger_id       INT NOT NULL,
    scheduled_date   DATE NOT NULL,
    maintenance_type ENUM('PREVENTIVE','CORRECTIVE','INSPECTION','UPGRADE') NOT NULL,
    description      VARCHAR(255),
    technician       VARCHAR(100),
    status           ENUM('SCHEDULED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'SCHEDULED',
    completed_on     DATE NULL,
    CONSTRAINT fk_maint_charger FOREIGN KEY (charger_id) REFERENCES chargers(charger_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. DEMAND RECORDS : hourly historical demand per station
--     (input data for the future AI / ML demand forecasting module)
-- ---------------------------------------------------------------------
CREATE TABLE demand_records (
    demand_id      INT AUTO_INCREMENT PRIMARY KEY,
    station_id     INT NOT NULL,
    demand_date    DATE NOT NULL,
    hour_of_day    TINYINT NOT NULL,
    sessions_count INT NOT NULL DEFAULT 0,
    energy_kwh     DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_demand UNIQUE (station_id, demand_date, hour_of_day),
    CONSTRAINT chk_demand_hour CHECK (hour_of_day BETWEEN 0 AND 23),
    CONSTRAINT fk_demand_station FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. NOTIFICATIONS : in-app notifications for operators / admins
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    type            ENUM('QUEUE','ALLOCATION','FAULT','MAINTENANCE','PAYMENT','BOOKING','SYSTEM') NOT NULL,
    message         VARCHAR(255) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX idx_notifications_read ON notifications (is_read, created_at);

-- ---------------------------------------------------------------------
-- 13. AUDIT LOG : transaction / activity log
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    action     VARCHAR(50) NOT NULL,
    entity     VARCHAR(50) NOT NULL,
    entity_id  VARCHAR(30),
    details    VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX idx_audit_created ON audit_log (created_at);

-- ---------------------------------------------------------------------
-- 14. SYSTEM CONFIG : key/value settings editable by the administrator
-- ---------------------------------------------------------------------
CREATE TABLE system_config (
    config_key   VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(100) NOT NULL,
    description  VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Reporting views
-- ---------------------------------------------------------------------
CREATE VIEW v_station_capacity AS
SELECT s.station_id, s.station_code, s.name, s.city, s.status,
       COUNT(c.charger_id)                                  AS total_chargers,
       SUM(c.charger_type = 'FAST')                         AS fast_chargers,
       SUM(c.charger_type = 'SLOW')                         AS slow_chargers,
       SUM(c.status = 'AVAILABLE')                          AS available,
       SUM(c.status = 'OCCUPIED')                           AS occupied,
       SUM(c.status = 'FAULTY')                             AS faulty,
       SUM(c.status = 'MAINTENANCE')                        AS maintenance,
       COALESCE(SUM(c.power_kw), 0)                         AS capacity_kw
FROM stations s
LEFT JOIN chargers c ON c.station_id = s.station_id
GROUP BY s.station_id, s.station_code, s.name, s.city, s.status;

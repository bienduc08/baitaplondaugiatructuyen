-- ==========================================
-- XÓA VÀ TẠO LẠI DATABASE
-- ==========================================
DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

-- ==========================================
-- BẢNG 1: users
-- ==========================================
CREATE TABLE users (
                       id          INT AUTO_INCREMENT PRIMARY KEY,
                       fullname    VARCHAR(255) NOT NULL,
                       username    VARCHAR(100) NOT NULL UNIQUE,
                       gmail       VARCHAR(255) NOT NULL UNIQUE,
                       phonenumber VARCHAR(100) NOT NULL UNIQUE,
                       password    VARCHAR(256) NOT NULL,
                       role        VARCHAR(20)  NOT NULL,
                       balance     DOUBLE       NOT NULL DEFAULT 0.0,
                       status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG 2: products
-- ==========================================
CREATE TABLE products (
                          id               INT AUTO_INCREMENT PRIMARY KEY,
                          name             VARCHAR(255) NOT NULL,
                          description      TEXT         DEFAULT NULL,
                          starting_price   DOUBLE       NOT NULL DEFAULT 0.0,
                          current_price    DOUBLE       NOT NULL DEFAULT 0.0,
                          step_price       DOUBLE       NOT NULL DEFAULT 0.0,
                          seller_name      VARCHAR(100) DEFAULT NULL,
                          owner_name       VARCHAR(100) DEFAULT NULL,
                          start_time       DATETIME     DEFAULT NULL,
                          end_time         DATETIME     DEFAULT NULL,
                          status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                          extension_count  INT          NOT NULL DEFAULT 0,
    -- DATETIME(3): lưu millisecond, cần thiết để so sánh với bid_time (NOW(3))
    -- Nếu dùng DATETIME thường, anti-sniping sẽ bị trigger nhiều lần mỗi giây
                          last_extended_at DATETIME(3)  NULL DEFAULT NULL,
                          image_url        VARCHAR(255) DEFAULT NULL,

                          CONSTRAINT fk_products_seller FOREIGN KEY (seller_name) REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,
                          CONSTRAINT fk_products_owner  FOREIGN KEY (owner_name)  REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,

                          INDEX idx_products_status (status),
                          INDEX idx_products_seller (seller_name)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG 3: bids
-- ==========================================
CREATE TABLE bids (
                      id          INT AUTO_INCREMENT PRIMARY KEY,
                      product_id  INT          NOT NULL,
                      bidder_name VARCHAR(100) NOT NULL,
                      amount      DOUBLE       NOT NULL,
    -- DATETIME(3): millisecond để anti-sniping so sánh chính xác với last_extended_at
                      bid_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                      status      VARCHAR(20)  NOT NULL DEFAULT 'Hợp lệ',

                      CONSTRAINT fk_bids_product FOREIGN KEY (product_id)  REFERENCES products(id) ON DELETE CASCADE,
                      CONSTRAINT fk_bids_bidder  FOREIGN KEY (bidder_name) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

                      INDEX idx_bids_product_amount (product_id, amount DESC),
                      INDEX idx_bids_bid_time       (bid_time)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG 4: notifications
-- ==========================================
CREATE TABLE notifications (
                               id         INT AUTO_INCREMENT PRIMARY KEY,
                               username   VARCHAR(100) NOT NULL,
                               message    TEXT         NOT NULL,
                               type       VARCHAR(50)  NOT NULL,
                               is_read    BOOLEAN      DEFAULT FALSE,
                               created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notif_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

                               INDEX idx_notif_username (username),
                               INDEX idx_notif_is_read  (is_read)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==========================================
-- DỮ LIỆU MẪU
-- Mật khẩu mặc định tất cả tài khoản: 123456
-- ==========================================
INSERT INTO users (fullname, username, gmail, phonenumber, password, role, balance, status) VALUES
                                                                                                ('Quản trị viên', 'admin',   'admin@gmail.com',   '0111111111', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ADMIN',  0.0,          'ACTIVE'),
                                                                                                ('Người Bán Hàng', 'seller1', 'seller1@gmail.com', '0222222222', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'SELLER', 0.0,          'ACTIVE'),
                                                                                                ('Người Mua VIP 1', 'user1',  'user1@gmail.com',   '0333333333', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'USER',   50000000.0,   'ACTIVE'),
                                                                                                ('Người Mua VIP 2', 'user2',  'user2@gmail.com',   '0444444444', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'USER',   20000000.0,   'ACTIVE');
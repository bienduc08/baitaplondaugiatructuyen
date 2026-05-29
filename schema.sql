-- 1. Khởi tạo Cơ sở Dữ liệu
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 2. Bảng Người dùng (users)
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    fullname VARCHAR(255) NOT NULL ,
    username VARCHAR(100) NOT NULL UNIQUE,
    gmail VARCHAR(255) NOT NULL UNIQUE ,
    phonenumber VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    role     VARCHAR(20)  NOT NULL ,
    balance  DOUBLE NOT NULL DEFAULT 0.0,
    status   VARCHAR(20)  NOT   NULL DEFAULT 'ACTIVE'
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Bảng Sản phẩm Đấu giá (products)
CREATE TABLE IF NOT EXISTS products (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    TEXT         DEFAULT NULL,
    starting_price DOUBLE NOT NULL DEFAULT 0.0,
    current_price  DOUBLE NOT NULL DEFAULT 0.0,
    step_price     DOUBLE NOT NULL DEFAULT 0.0,
    seller_name    VARCHAR(100) DEFAULT NULL,
    owner_name     VARCHAR(100) DEFAULT NULL,
    start_time     DATETIME     DEFAULT NULL,
    end_time       DATETIME     DEFAULT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    image_url      VARCHAR(255) DEFAULT NULL,

    -- Khóa ngoại đảm bảo tính toàn vẹn dữ liệu
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_name) REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_products_owner FOREIGN KEY (owner_name) REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,

    -- Chỉ mục (Indexes) để tối ưu hiệu năng truy vấn
    INDEX idx_products_status (status),
    INDEX idx_products_seller (seller_name)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. Bảng Lịch sử Đấu giá (bids)
CREATE TABLE IF NOT EXISTS bids (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT          NOT NULL,
    bidder_name VARCHAR(100) NOT NULL,
    amount      DOUBLE NOT NULL,
    bid_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status      VARCHAR(20)  NOT NULL DEFAULT 'Hợp lệ',

-- Khóa ngoại đảm bảo tính toàn vẹn dữ liệu
    CONSTRAINT fk_bids_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_name) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

    -- Chỉ mục (Indexes) để tối ưu hiệu năng truy vấn
    INDEX idx_bids_product_amount (product_id, amount DESC)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
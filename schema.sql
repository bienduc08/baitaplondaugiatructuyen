CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

-- ==========================================
-- TẠO CẤU TRÚC CÁC BẢNG (4 BẢNG)
-- ==========================================

-- Bảng 1: Người dùng (users)
CREATE TABLE IF NOT EXISTS users (
                                     id       INT AUTO_INCREMENT PRIMARY KEY,
                                     fullname VARCHAR(255) NOT NULL,
                                     username VARCHAR(100) NOT NULL UNIQUE,
                                     gmail    VARCHAR(255) NOT NULL UNIQUE,
                                     phonenumber VARCHAR(100) NOT NULL UNIQUE,
                                     password VARCHAR(256) NOT NULL,
                                     role     VARCHAR(20)  NOT NULL,
                                     balance  DOUBLE NOT NULL DEFAULT 0.0,
                                     status   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng 2: Sản phẩm Đấu giá (products)
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
                                        extension_count INT          NOT NULL DEFAULT 0,
                                        image_url      VARCHAR(255) DEFAULT NULL,

    -- Khóa ngoại
                                        CONSTRAINT fk_products_seller FOREIGN KEY (seller_name) REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,
                                        CONSTRAINT fk_products_owner FOREIGN KEY (owner_name) REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,

    -- Chỉ mục
                                        INDEX idx_products_status (status),
                                        INDEX idx_products_seller (seller_name)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng 3: Lịch sử Đấu giá (bids)
CREATE TABLE IF NOT EXISTS bids (
                                    id          INT AUTO_INCREMENT PRIMARY KEY,
                                    product_id  INT          NOT NULL,
                                    bidder_name VARCHAR(100) NOT NULL,
                                    amount      DOUBLE NOT NULL,
                                    bid_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    status      VARCHAR(20)  NOT NULL DEFAULT 'Hợp lệ',

    -- Khóa ngoại
                                    CONSTRAINT fk_bids_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_name) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

    -- Chỉ mục
                                    INDEX idx_bids_product_amount (product_id, amount DESC)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng 4: Thông báo (notifications)
CREATE TABLE IF NOT EXISTS notifications (
                                             id         INT AUTO_INCREMENT PRIMARY KEY,
                                             username   VARCHAR(100) NOT NULL,
                                             message    TEXT NOT NULL,
                                             type       VARCHAR(50) NOT NULL,
                                             is_read    BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Khóa ngoại (Xóa tài khoản thì xóa luôn thông báo)
                                             CONSTRAINT fk_notif_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ==========================================
-- THÊM DỮ LIỆU TÀI KHOẢN MẪU
-- ==========================================

-- Lưu ý: Mật khẩu mặc định cho tất cả các tài khoản dưới đây là '123456'
-- (Giả định hệ thống của bạn lưu mật khẩu dạng plain text hoặc xử lý băm mật khẩu riêng biệt)

INSERT INTO users (fullname, username, gmail, phonenumber, password, role, balance, status) VALUES
-- 1 Tài khoản ADMIN (Để duyệt sản phẩm, quản lý user)
('Quản trị viên', 'admin', 'admin@gmail.com', '0111111111', '123456', 'ADMIN', 0.0, 'ACTIVE'),

-- 1 Tài khoản SELLER (Để đăng bán sản phẩm)
('Người Bán Hàng', 'seller1', 'seller1@gmail.com', '0222222222', '123456', 'SELLER', 0.0, 'ACTIVE'),

-- 2 Tài khoản USER (Đã được nạp sẵn tiền để test đấu giá cạnh tranh ngay lập tức)
('Người Mua VIP 1', 'user1', 'user1@gmail.com', '0333333333', '123456', 'USER', 50000000.0, 'ACTIVE'),
('Người Mua VIP 2', 'user2', 'user2@gmail.com', '0444444444', '123456', 'USER', 20000000.0, 'ACTIVE');
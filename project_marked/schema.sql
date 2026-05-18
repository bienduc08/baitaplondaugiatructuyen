USE auction_db;

CREATE TABLE IF NOT EXISTS users (
                                     id       INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
                                     password VARCHAR(256) NOT NULL,
                                     role     VARCHAR(20)  NOT NULL DEFAULT 'USER',
                                     balance  DOUBLE       NOT NULL DEFAULT 0.0   -- THÊM DÒNG NÀY
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
                                        id             INT AUTO_INCREMENT PRIMARY KEY,
                                        name           VARCHAR(255) NOT NULL,
                                        description    TEXT         DEFAULT NULL,
                                        starting_price DOUBLE       NOT NULL DEFAULT 0,
                                        current_price  DOUBLE       NOT NULL DEFAULT 0,  -- tự động = starting_price khi tạo
                                        seller_name    VARCHAR(100) DEFAULT NULL,
                                        owner_name     VARCHAR(100) DEFAULT NULL,
                                        start_time     DATETIME     DEFAULT NULL,
                                        end_time       DATETIME     DEFAULT NULL,
                                        status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bids (
                                    id          INT AUTO_INCREMENT PRIMARY KEY,
                                    product_id  INT          NOT NULL,
                                    bidder_name VARCHAR(100) NOT NULL,
                                    amount      DOUBLE       NOT NULL,
                                    bid_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    status      VARCHAR(20)  NOT NULL DEFAULT 'Hợp lệ',
                                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT IGNORE INTO users (username, password, role)
VALUES ('admin',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'ADMIN');
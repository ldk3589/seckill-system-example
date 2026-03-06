CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seckill;

DROP TABLE IF EXISTS t_seckill_order;
DROP TABLE IF EXISTS t_product_stock;
DROP TABLE IF EXISTS t_product;
DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(64) NOT NULL UNIQUE,
                        password_hash VARCHAR(128) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_product (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           name VARCHAR(128) NOT NULL,
                           price BIGINT NOT NULL,
                           status TINYINT NOT NULL DEFAULT 1,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_product_stock (
                                 product_id BIGINT PRIMARY KEY,
                                 stock INT NOT NULL,
                                 version INT NOT NULL DEFAULT 0,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES t_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_seckill_order (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 order_no VARCHAR(64) NOT NULL UNIQUE,
                                 user_id BIGINT NOT NULL,
                                 product_id BIGINT NOT NULL,
                                 status TINYINT NOT NULL DEFAULT 0,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE KEY uk_user_product (user_id, product_id),
                                 KEY idx_user (user_id),
                                 KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO t_product (id, name, price, status)
VALUES (1, 'iPhone 秒杀款', 499900, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO t_product_stock (product_id, stock, version)
VALUES (1, 50, 0)
ON DUPLICATE KEY UPDATE stock = VALUES(stock), version = VALUES(version);
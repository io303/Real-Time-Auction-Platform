-- Phase 4 migration: run this ONCE against your existing auction_platform database.
-- (spring.sql.init.mode is "never", so schema.sql doesn't auto-run — same pattern as Phase 3.)
--
-- How to run (PowerShell, from the folder containing this file):
--   Get-Content phase4_migration.sql | docker exec -i auction-mysql mysql -uroot -proot auction_platform

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT NULL,
    parent_category_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS auctions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    starting_price DECIMAL(12,2) NOT NULL,
    reserve_price DECIMAL(12,2) NULL,
    min_increment DECIMAL(12,2) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_auctions_seller_id ON auctions(seller_id);
CREATE INDEX idx_auctions_category_id ON auctions(category_id);
CREATE INDEX idx_auctions_status ON auctions(status);

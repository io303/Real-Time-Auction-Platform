-- Phase 3 migration: run this ONCE against your existing auction_platform database.
-- Since spring.sql.init.mode is set to "never" (Phase 2 fix), schema.sql no longer
-- runs automatically, so new tables/columns must be applied manually like this.
--
-- How to run (pick one):
--   A) MySQL Workbench / any GUI client: open this file, connect to auction_platform, execute.
--   B) Command line via Docker:
--        docker exec -it auction-mysql mysql -uroot -proot auction_platform < phase3_migration.sql
--      (run this from PowerShell in the folder containing this file)

ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    address_type VARCHAR(20) NOT NULL DEFAULT 'HOME',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- Phase 5+6 migration: run this ONCE against your existing auction_platform database.
--
-- How to run (PowerShell, from the folder containing this file):
--   Get-Content phase5_6_migration.sql | docker exec -i auction-mysql mysql -uroot -proot auction_platform

CREATE TABLE IF NOT EXISTS auction_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

CREATE INDEX idx_auction_images_auction_id ON auction_images(auction_id);

ALTER TABLE auctions
    ADD COLUMN current_highest_bid DECIMAL(12,2) NULL,
    ADD COLUMN current_highest_bidder_id BIGINT NULL,
    ADD CONSTRAINT fk_current_highest_bidder FOREIGN KEY (current_highest_bidder_id) REFERENCES users(id);

CREATE TABLE IF NOT EXISTS bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

CREATE INDEX idx_bids_auction_id ON bids(auction_id);
CREATE INDEX idx_bids_bidder_id ON bids(bidder_id);

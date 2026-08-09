-- Phase 7 migration: run this ONCE against your existing auction_platform database.
--
-- How to run (PowerShell, from the folder containing this file):
--   Get-Content phase7_migration.sql | docker exec -i auction-mysql mysql -uroot -proot auction_platform

CREATE TABLE IF NOT EXISTS auto_bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    max_bid DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_auction_bidder (auction_id, bidder_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

CREATE INDEX idx_auto_bids_auction_id ON auto_bids(auction_id);

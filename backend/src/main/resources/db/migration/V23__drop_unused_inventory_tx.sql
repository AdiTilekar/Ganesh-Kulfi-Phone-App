-- V23: Deprecate unused inventory_tx table and product_current_stock view
-- 
-- inventory_tx was the Day 4 ledger design, superseded by Day 6's inventory_log.
-- No Kotlin code references inventory_tx or the product_current_stock view.
-- All stock operations use inventory_log + PL/pgSQL functions.

-- Drop the view first (depends on inventory_tx)
DROP VIEW IF EXISTS product_current_stock;

-- Drop the orphaned table
DROP TABLE IF EXISTS inventory_tx;

-- Drop the orphaned inventory_reason enum type if it exists
DROP TYPE IF EXISTS inventory_reason;

-- ============================================
-- Day 4: Inventory Ledger System (Immutable)
-- ============================================

-- Inventory Transaction Table (Immutable Ledger)
CREATE TABLE IF NOT EXISTS inventory_tx (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    delta INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    actor_id UUID NOT NULL,
    order_id VARCHAR(36) NULL,
    ts TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_actor FOREIGN KEY (actor_id) REFERENCES app_user(id) ON DELETE RESTRICT
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_product_id ON inventory_tx(product_id);
CREATE INDEX IF NOT EXISTS idx_actor_id ON inventory_tx(actor_id);
CREATE INDEX IF NOT EXISTS idx_order_id ON inventory_tx(order_id);
CREATE INDEX IF NOT EXISTS idx_ts ON inventory_tx(ts);
CREATE INDEX IF NOT EXISTS idx_reason ON inventory_tx(reason);

-- Initial stock transactions for existing products
-- This seeds the ledger with current stock_quantity from products table
INSERT INTO inventory_tx (product_id, delta, reason, actor_id, ts)
SELECT 
    id,
    stock_quantity,
    'INITIAL_STOCK',
    (SELECT id FROM app_user WHERE role = 'ADMIN' LIMIT 1),
    NOW()
FROM product
WHERE stock_quantity > 0;

-- Optional: Add computed stock view for easier queries
DROP VIEW IF EXISTS product_current_stock;
CREATE VIEW product_current_stock AS
SELECT 
    p.id AS product_id,
    p.name AS product_name,
    COALESCE(SUM(it.delta), 0) AS current_stock,
    MAX(it.ts) AS last_updated
FROM product p
LEFT JOIN inventory_tx it ON p.id = it.product_id
GROUP BY p.id, p.name;

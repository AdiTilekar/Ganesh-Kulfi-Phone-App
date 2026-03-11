-- V21: Daily Sales Log Table
-- Created: 2026-03-10
-- Description: Point-of-sale daily sales recording for profit analysis and Excel export

CREATE TABLE IF NOT EXISTS daily_sales (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    product_id        VARCHAR(36) NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    product_name      VARCHAR(100) NOT NULL,
    quantity          INTEGER NOT NULL CHECK (quantity > 0),
    cost_price        NUMERIC(10, 2) NOT NULL CHECK (cost_price >= 0),
    sell_price        NUMERIC(10, 2) NOT NULL CHECK (sell_price >= 0),
    profit            NUMERIC(10, 2) NOT NULL GENERATED ALWAYS AS ((sell_price - cost_price) * quantity) STORED,
    payment_method    VARCHAR(20) NOT NULL DEFAULT 'CASH'
                          CHECK (payment_method IN ('CASH', 'UPI', 'CREDIT')),
    note              TEXT,
    sold_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idempotency_key   VARCHAR(64) UNIQUE
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_daily_sales_user     ON daily_sales(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_sales_product  ON daily_sales(product_id);
CREATE INDEX IF NOT EXISTS idx_daily_sales_sold_at  ON daily_sales(sold_at DESC);
-- Date-based lookups (date is the most frequent filter)
CREATE INDEX IF NOT EXISTS idx_daily_sales_date     ON daily_sales(DATE(sold_at));
CREATE INDEX IF NOT EXISTS idx_daily_sales_idem_key ON daily_sales(idempotency_key) WHERE idempotency_key IS NOT NULL;

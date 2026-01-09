-- V5: Orders System with Status Management
-- Created: 2025-11-15
-- Description: Complete order management system with status tracking

-- Create order status enum
DO $$ BEGIN
    CREATE TYPE order_status AS ENUM ('PENDING', 'CONFIRMED', 'REJECTED', 'COMPLETED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create payment status enum
DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM ('UNPAID', 'PARTIAL', 'PAID');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    retailer_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    retailer_email VARCHAR(255) NOT NULL,
    retailer_name VARCHAR(255) NOT NULL,
    shop_name VARCHAR(255),
    
    -- Order details
    total_items INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    subtotal NUMERIC(10, 2) NOT NULL DEFAULT 0,
    discount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    
    -- Status tracking
    status order_status NOT NULL DEFAULT 'PENDING',
    payment_status payment_status NOT NULL DEFAULT 'UNPAID',
    
    -- Notes and metadata
    retailer_notes TEXT,
    factory_notes TEXT,
    rejection_reason TEXT,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    -- Who updated
    confirmed_by UUID REFERENCES app_user(id),
    rejected_by UUID REFERENCES app_user(id)
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(36) NOT NULL REFERENCES product(id),
    product_name VARCHAR(100) NOT NULL,
    
    -- Quantity and pricing
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    discount_percent NUMERIC(5, 2) DEFAULT 0,
    discount_amount NUMERIC(10, 2) DEFAULT 0,
    line_total NUMERIC(10, 2) NOT NULL,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_orders_retailer ON orders(retailer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);

-- Create trigger for orders updated_at
DROP TRIGGER IF EXISTS update_orders_updated_at ON orders;
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to generate order number
CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    new_number VARCHAR(50);
    counter INTEGER;
BEGIN
    -- Get count of orders today
    SELECT COUNT(*) INTO counter
    FROM orders
    WHERE DATE(created_at) = CURRENT_DATE;
    
    -- Format: ORD-YYYYMMDD-XXXX
    new_number := 'ORD-' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-' || LPAD((counter + 1)::TEXT, 4, '0');
    
    RETURN new_number;
END;
$$ LANGUAGE plpgsql;

-- Verification
SELECT '✅ Orders system created successfully!' as status;
SELECT COUNT(*) as total_orders FROM orders;

-- =====================================================
-- Day 6: Inventory Management System with Stock Reservation
-- =====================================================

-- Add reserved_quantity to products table
ALTER TABLE product ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0;
ALTER TABLE product ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

-- Add CHECK constraint to ensure reserved_quantity doesn't exceed stock_quantity
ALTER TABLE product ADD CONSTRAINT check_reserved_quantity 
    CHECK (reserved_quantity >= 0 AND reserved_quantity <= stock_quantity);

-- Create inventory_logs table for tracking all stock changes
CREATE TABLE IF NOT EXISTS inventory_log (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    change_type VARCHAR(50) NOT NULL, -- ADDED, INCREASED, DECREASED, RESERVED, RELEASED, DEDUCTED, ADJUSTMENT
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    quantity_change INTEGER NOT NULL,
    reason TEXT,
    order_id CHAR(36),
    performed_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_inventory_log_product ON inventory_log(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_log_order ON inventory_log(order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_log_created ON inventory_log(created_at);
CREATE INDEX IF NOT EXISTS idx_product_status ON product(status);

-- Add reserved_by field to orders to track stock reservation
ALTER TABLE orders ADD COLUMN IF NOT EXISTS stock_reserved BOOLEAN NOT NULL DEFAULT FALSE;

-- Function to check available stock (stock_quantity - reserved_quantity)
CREATE OR REPLACE FUNCTION get_available_quantity(product_uuid UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT (stock_quantity - reserved_quantity) 
        FROM product 
        WHERE id = product_uuid
    );
END;
$$ LANGUAGE plpgsql;

-- Function to reserve stock for an order
CREATE OR REPLACE FUNCTION reserve_stock_for_order(
    p_order_id UUID,
    p_product_id UUID,
    p_quantity INTEGER,
    p_user_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_available INTEGER;
    v_current_qty INTEGER;
    v_current_reserved INTEGER;
BEGIN
    -- Get current quantities
    SELECT stock_quantity, reserved_quantity INTO v_current_qty, v_current_reserved
    FROM product
    WHERE id = p_product_id
    FOR UPDATE; -- Lock row for update
    
    -- Calculate available
    v_available := v_current_qty - v_current_reserved;
    
    -- Check if enough stock
    IF v_available < p_quantity THEN
        RETURN FALSE;
    END IF;
    
    -- Reserve the stock
    UPDATE product
    SET reserved_quantity = reserved_quantity + p_quantity
    WHERE id = p_product_id;
    
    -- Log the reservation
    INSERT INTO inventory_log (
        product_id, change_type, quantity_before, quantity_after,
        quantity_change, reason, order_id, performed_by
    ) VALUES (
        p_product_id, 'RESERVED', v_current_reserved, v_current_reserved + p_quantity,
        p_quantity, 'Stock reserved for order', p_order_id, p_user_id
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to release reserved stock (on order rejection/cancellation)
CREATE OR REPLACE FUNCTION release_reserved_stock(
    p_order_id UUID,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    order_item RECORD;
    v_current_reserved INTEGER;
BEGIN
    -- Release stock for each item in the order
    FOR order_item IN 
        SELECT product_id, quantity 
        FROM order_items 
        WHERE order_id = p_order_id
    LOOP
        -- Get current reserved quantity
        SELECT reserved_quantity INTO v_current_reserved
        FROM product
        WHERE id = order_item.product_id
        FOR UPDATE;
        
        -- Release the reservation
        UPDATE product
        SET reserved_quantity = GREATEST(0, reserved_quantity - order_item.quantity)
        WHERE id = order_item.product_id;
        
        -- Log the release
        INSERT INTO inventory_log (
            product_id, change_type, quantity_before, quantity_after,
            quantity_change, reason, order_id, performed_by
        ) VALUES (
            order_item.product_id, 'RELEASED', v_current_reserved,
            GREATEST(0, v_current_reserved - order_item.quantity),
            -order_item.quantity, 'Stock reservation released', p_order_id, p_user_id
        );
    END LOOP;
    
    -- Mark order as stock released
    UPDATE orders
    SET stock_reserved = FALSE
    WHERE id = p_order_id;
END;
$$ LANGUAGE plpgsql;

-- Function to deduct stock (on order confirmation)
CREATE OR REPLACE FUNCTION deduct_confirmed_stock(
    p_order_id UUID,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    order_item RECORD;
    v_current_qty INTEGER;
    v_current_reserved INTEGER;
BEGIN
    -- Deduct stock for each item in the order
    FOR order_item IN 
        SELECT product_id, quantity 
        FROM order_items 
        WHERE order_id = p_order_id
    LOOP
        -- Get current quantities
        SELECT stock_quantity, reserved_quantity INTO v_current_qty, v_current_reserved
        FROM product
        WHERE id = order_item.product_id
        FOR UPDATE;
        
        -- Deduct from both actual and reserved
        UPDATE product
        SET 
            stock_quantity = GREATEST(0, stock_quantity - order_item.quantity),
            reserved_quantity = GREATEST(0, reserved_quantity - order_item.quantity),
            status = CASE 
                WHEN (stock_quantity - order_item.quantity) <= 0 THEN 'OUT_OF_STOCK'
                ELSE status
            END
        WHERE id = order_item.product_id;
        
        -- Log the deduction
        INSERT INTO inventory_log (
            product_id, change_type, quantity_before, quantity_after,
            quantity_change, reason, order_id, performed_by
        ) VALUES (
            order_item.product_id, 'DEDUCTED', v_current_qty,
            GREATEST(0, v_current_qty - order_item.quantity),
            -order_item.quantity, 'Stock deducted for confirmed order', p_order_id, p_user_id
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to adjust stock (manual increase/decrease by factory)
CREATE OR REPLACE FUNCTION adjust_stock(
    p_product_id UUID,
    p_quantity_change INTEGER,
    p_reason TEXT,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    v_current_qty INTEGER;
    v_new_qty INTEGER;
    v_change_type VARCHAR(50);
BEGIN
    -- Get current quantity
    SELECT stock_quantity INTO v_current_qty
    FROM product
    WHERE id = p_product_id
    FOR UPDATE;
    
    -- Calculate new quantity
    v_new_qty := GREATEST(0, v_current_qty + p_quantity_change);
    
    -- Determine change type
    IF p_quantity_change > 0 THEN
        v_change_type := 'INCREASED';
    ELSIF p_quantity_change < 0 THEN
        v_change_type := 'DECREASED';
    ELSE
        v_change_type := 'ADJUSTMENT';
    END IF;
    
    -- Update product quantity and status
    UPDATE product
    SET 
        stock_quantity = v_new_qty,
        status = CASE 
            WHEN v_new_qty <= 0 THEN 'OUT_OF_STOCK'
            WHEN v_new_qty > 0 AND status = 'OUT_OF_STOCK' THEN 'AVAILABLE'
            ELSE status
        END
    WHERE id = p_product_id;
    
    -- Log the adjustment
    INSERT INTO inventory_log (
        product_id, change_type, quantity_before, quantity_after,
        quantity_change, reason, performed_by
    ) VALUES (
        p_product_id, v_change_type, v_current_qty, v_new_qty,
        p_quantity_change, p_reason, p_user_id
    );
END;
$$ LANGUAGE plpgsql;

-- Insert initial inventory logs for existing products (migration only)
INSERT INTO inventory_log (
    id, product_id, change_type, quantity_before, quantity_after,
    quantity_change, reason, performed_by
)
SELECT 
    gen_random_uuid()::text,
    id,
    'ADDED',
    0,
    stock_quantity,
    stock_quantity,
    'Initial stock from Day 6 migration',
    (SELECT id FROM app_user WHERE role = 'ADMIN' LIMIT 1)
FROM product;

-- Verification query
SELECT 
    'Inventory management system created!' as status,
    COUNT(*) as total_products,
    SUM(stock_quantity) as total_stock,
    SUM(reserved_quantity) as total_reserved,
    SUM(stock_quantity - reserved_quantity) as total_available
FROM product;

-- Show inventory logs count
SELECT COUNT(*) as total_inventory_logs FROM inventory_log;

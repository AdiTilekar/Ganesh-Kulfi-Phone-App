-- V22: Fix PL/pgSQL function parameter types
-- 
-- product.id is VARCHAR(36) with semantic string IDs (e.g., 'mango', 'chocolate'),
-- NOT actual UUIDs. The V6 functions declared product_id parameters as UUID,
-- which causes type mismatch errors. This migration recreates all stock functions
-- with VARCHAR(36) for product_id parameters.
--
-- order_id and user_id remain UUID since orders.id and app_user.id are real UUIDs.

-- Drop old UUID-parameter versions first (V6 created these with UUID product_id)
DROP FUNCTION IF EXISTS get_available_quantity(UUID);
DROP FUNCTION IF EXISTS reserve_stock_for_order(UUID, UUID, INTEGER, UUID);
DROP FUNCTION IF EXISTS adjust_stock(UUID, INTEGER, TEXT, UUID);
-- These two keep the same param types so CREATE OR REPLACE works, but drop for safety
DROP FUNCTION IF EXISTS release_reserved_stock(UUID, UUID);
DROP FUNCTION IF EXISTS deduct_confirmed_stock(UUID, UUID);
-- Drop generate_order_number (return type changes from VARCHAR to TEXT)
DROP FUNCTION IF EXISTS generate_order_number();

-- Fix get_available_quantity: product_id should be VARCHAR, not UUID
CREATE OR REPLACE FUNCTION get_available_quantity(p_product_id VARCHAR(36))
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT (stock_quantity - reserved_quantity) 
        FROM product 
        WHERE id = p_product_id
    );
END;
$$ LANGUAGE plpgsql;

-- Fix reserve_stock_for_order: product_id should be VARCHAR
CREATE OR REPLACE FUNCTION reserve_stock_for_order(
    p_order_id UUID,
    p_product_id VARCHAR(36),
    p_quantity INTEGER,
    p_user_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_available INTEGER;
    v_current_qty INTEGER;
    v_current_reserved INTEGER;
BEGIN
    SELECT stock_quantity, reserved_quantity INTO v_current_qty, v_current_reserved
    FROM product
    WHERE id = p_product_id
    FOR UPDATE;
    
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    v_available := v_current_qty - v_current_reserved;
    
    IF v_available < p_quantity THEN
        RETURN FALSE;
    END IF;
    
    UPDATE product
    SET reserved_quantity = reserved_quantity + p_quantity
    WHERE id = p_product_id;
    
    INSERT INTO inventory_log (
        id, product_id, change_type, quantity_before, quantity_after,
        quantity_change, reason, order_id, performed_by, created_at
    ) VALUES (
        gen_random_uuid()::text, p_product_id, 'RESERVED', v_current_reserved,
        v_current_reserved + p_quantity, p_quantity,
        'Stock reserved for order', p_order_id::text, p_user_id::text, NOW()
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Fix release_reserved_stock: no product_id param, but internal query uses product.id correctly
CREATE OR REPLACE FUNCTION release_reserved_stock(
    p_order_id UUID,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    order_item RECORD;
    v_current_reserved INTEGER;
BEGIN
    FOR order_item IN 
        SELECT product_id, quantity 
        FROM order_items 
        WHERE order_id = p_order_id
    LOOP
        SELECT reserved_quantity INTO v_current_reserved
        FROM product
        WHERE id = order_item.product_id
        FOR UPDATE;
        
        UPDATE product
        SET reserved_quantity = GREATEST(0, reserved_quantity - order_item.quantity)
        WHERE id = order_item.product_id;
        
        INSERT INTO inventory_log (
            id, product_id, change_type, quantity_before, quantity_after,
            quantity_change, reason, order_id, performed_by, created_at
        ) VALUES (
            gen_random_uuid()::text, order_item.product_id, 'RELEASED', v_current_reserved,
            GREATEST(0, v_current_reserved - order_item.quantity),
            -order_item.quantity, 'Stock reservation released', p_order_id::text, p_user_id::text, NOW()
        );
    END LOOP;
    
    UPDATE orders
    SET stock_reserved = FALSE
    WHERE id = p_order_id;
END;
$$ LANGUAGE plpgsql;

-- Fix deduct_confirmed_stock: internal product.id comparisons work as VARCHAR
CREATE OR REPLACE FUNCTION deduct_confirmed_stock(
    p_order_id UUID,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    order_item RECORD;
    v_current_qty INTEGER;
    v_current_reserved INTEGER;
BEGIN
    FOR order_item IN 
        SELECT product_id, quantity 
        FROM order_items 
        WHERE order_id = p_order_id
    LOOP
        SELECT stock_quantity, reserved_quantity INTO v_current_qty, v_current_reserved
        FROM product
        WHERE id = order_item.product_id
        FOR UPDATE;
        
        UPDATE product
        SET 
            stock_quantity = GREATEST(0, stock_quantity - order_item.quantity),
            reserved_quantity = GREATEST(0, reserved_quantity - order_item.quantity),
            status = CASE 
                WHEN (stock_quantity - order_item.quantity) <= 0 THEN 'OUT_OF_STOCK'
                ELSE status
            END
        WHERE id = order_item.product_id;
        
        INSERT INTO inventory_log (
            id, product_id, change_type, quantity_before, quantity_after,
            quantity_change, reason, order_id, performed_by, created_at
        ) VALUES (
            gen_random_uuid()::text, order_item.product_id, 'DEDUCTED', v_current_qty,
            GREATEST(0, v_current_qty - order_item.quantity),
            -order_item.quantity, 'Stock deducted for confirmed order', p_order_id::text, p_user_id::text, NOW()
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Fix adjust_stock: product_id should be VARCHAR
CREATE OR REPLACE FUNCTION adjust_stock(
    p_product_id VARCHAR(36),
    p_quantity_change INTEGER,
    p_reason TEXT,
    p_user_id UUID
) RETURNS VOID AS $$
DECLARE
    v_current_qty INTEGER;
    v_new_qty INTEGER;
    v_change_type VARCHAR(50);
BEGIN
    SELECT stock_quantity INTO v_current_qty
    FROM product
    WHERE id = p_product_id
    FOR UPDATE;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product not found: %', p_product_id;
    END IF;
    
    v_new_qty := GREATEST(0, v_current_qty + p_quantity_change);
    
    IF p_quantity_change > 0 THEN
        v_change_type := 'INCREASED';
    ELSIF p_quantity_change < 0 THEN
        v_change_type := 'DECREASED';
    ELSE
        v_change_type := 'ADJUSTMENT';
    END IF;
    
    UPDATE product
    SET 
        stock_quantity = v_new_qty,
        status = CASE 
            WHEN v_new_qty <= 0 THEN 'OUT_OF_STOCK'
            WHEN v_new_qty > 0 AND status = 'OUT_OF_STOCK' THEN 'AVAILABLE'
            ELSE status
        END
    WHERE id = p_product_id;
    
    INSERT INTO inventory_log (
        id, product_id, change_type, quantity_before, quantity_after,
        quantity_change, reason, performed_by, created_at
    ) VALUES (
        gen_random_uuid()::text, p_product_id, v_change_type, v_current_qty, v_new_qty,
        p_quantity_change, p_reason, p_user_id::text, NOW()
    );
END;
$$ LANGUAGE plpgsql;

-- Also fix generate_order_number to use timestamp+random instead of COUNT(*) 
-- to avoid race conditions under concurrent inserts
CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TEXT AS $$
DECLARE
    v_date TEXT;
    v_time TEXT;
    v_rand TEXT;
BEGIN
    v_date := TO_CHAR(NOW(), 'YYYYMMDD');
    v_time := TO_CHAR(NOW(), 'HH24MISS');
    v_rand := LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
    RETURN 'ORD-' || v_date || '-' || v_time || '-' || v_rand;
END;
$$ LANGUAGE plpgsql;

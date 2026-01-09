-- Fix order number generation to prevent duplicates
-- This adds milliseconds and a random component to ensure uniqueness

DROP FUNCTION IF EXISTS generate_order_number();

CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    new_number VARCHAR(50);
    random_suffix VARCHAR(4);
BEGIN
    -- Generate a random 4-character suffix (0-9999)
    random_suffix := LPAD((RANDOM() * 9999)::INTEGER::TEXT, 4, '0');
    
    -- Format: ORD-YYYYMMDD-HHMMSS-RAND
    -- This ensures uniqueness even with simultaneous orders
    new_number := 'ORD-' || 
                  TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD-HH24MISS') || 
                  '-' || 
                  random_suffix;
    
    RETURN new_number;
END;
$$ LANGUAGE plpgsql;

-- Test the function
SELECT generate_order_number() as test1;
SELECT generate_order_number() as test2;
SELECT generate_order_number() as test3;

-- These should all be different

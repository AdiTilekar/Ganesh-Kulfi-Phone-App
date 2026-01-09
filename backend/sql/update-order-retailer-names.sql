-- Update existing orders with correct retailer names and shop names
-- This fixes orders that were created with hardcoded "Retailer" value

UPDATE orders o
SET 
    retailer_name = u.name,
    shop_name = u.shop_name
FROM app_user u
WHERE o.retailer_id = u.id
  AND (o.retailer_name = 'Retailer' OR o.retailer_name IS NULL OR o.retailer_name = '');

-- Verify the update
SELECT 
    order_number,
    retailer_name,
    shop_name,
    created_at
FROM orders
ORDER BY created_at DESC
LIMIT 10;

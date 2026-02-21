-- V21: Safe product re-seed using UPSERT (INSERT ... ON CONFLICT DO UPDATE)
-- Fixes the destructive DELETE+INSERT pattern from V16 that could cascade-delete
-- order_items and inventory_tx rows referencing these products.
--
-- This migration is safe to run on production databases with existing order history.

INSERT INTO product (id, name, description, base_price, category, image_url, is_available, is_seasonal, stock_quantity)
VALUES
  ('mango', 'Mango Kulfi', 'A seasonal delight capturing the sweet, luscious taste of ripe mangoes.', 20.00, 'FRUIT', 'mango_kulfi.png', TRUE, TRUE, 100),
  ('rabdi', 'Rabadi Kulfi', 'Rich, thickened milk kulfi with layers of creamy rabadi.', 20.00, 'CLASSIC', 'rabdi_kulfi.png', TRUE, FALSE, 80),
  ('strawberry', 'Strawberry Kulfi', 'A sweet and tangy kulfi made with fresh strawberries.', 25.00, 'FRUIT', 'strawberry_kulfi.png', TRUE, FALSE, 90),
  ('chocolate', 'Chocolate Kulfi', 'A modern twist with rich, decadent chocolate for all ages.', 35.00, 'PREMIUM', 'chocolate_kulfi.png', TRUE, FALSE, 120),
  ('paan', 'Paan Kulfi', 'Traditional paan flavor with a perfect blend of betel leaf essence.', 25.00, 'FUSION', 'paan_kulfi.png', TRUE, FALSE, 70),
  ('gulkand', 'Gulkand Kulfi', 'Aromatic rose petal preserve kulfi with a royal touch.', 30.00, 'PREMIUM', 'gulkand_kulfi.png', TRUE, FALSE, 60),
  ('dry_fruit', 'Dry Fruit Kulfi', 'Loaded with premium cashews, almonds, and pistachios.', 40.00, 'PREMIUM', 'dry_fruit_kulfi.png', TRUE, FALSE, 85),
  ('pineapple', 'Pineapple Kulfi', 'Tropical pineapple flavor with a refreshing tangy twist.', 25.00, 'FRUIT', 'pineapple_kulfi.png', TRUE, FALSE, 75),
  ('chikoo', 'Chikoo Kulfi', 'Sweet and smooth sapota kulfi with natural sweetness.', 22.00, 'FRUIT', 'chikoo_kulfi.png', TRUE, FALSE, 65),
  ('guava', 'Guava Kulfi', 'Fresh guava kulfi with authentic desi fruit flavor.', 22.00, 'FRUIT', 'guava_kulfi.png', TRUE, FALSE, 70),
  ('jamun', 'Jamun Kulfi', 'Rich purple jamun kulfi with tangy berry notes.', 28.00, 'FRUIT', 'jamun_kulfi.png', TRUE, FALSE, 55),
  ('sitafal', 'Sitafal Kulfi', 'Creamy custard apple kulfi with natural fruit chunks.', 32.00, 'FRUIT', 'sitafal_kulfi.png', TRUE, FALSE, 50),
  ('fig', 'Fig Kulfi', 'Premium fig kulfi with rich, honey-like sweetness.', 38.00, 'PREMIUM', 'fig_kulfi.png', TRUE, FALSE, 45)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  base_price = EXCLUDED.base_price,
  category = EXCLUDED.category,
  image_url = EXCLUDED.image_url,
  is_available = EXCLUDED.is_available,
  is_seasonal = EXCLUDED.is_seasonal;
  -- NOTE: stock_quantity is NOT updated on conflict to preserve production stock levels

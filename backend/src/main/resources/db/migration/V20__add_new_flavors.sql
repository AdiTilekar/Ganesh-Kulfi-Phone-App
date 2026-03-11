-- V20: Add Special Rabdi, Butterscotch, and Pista kulfi flavors

INSERT INTO product (id, name, description, base_price, category, image_url, is_available, is_seasonal, stock_quantity)
VALUES
('special_rabdi', 'Special Rabdi Kulfi', 'Indulgent special rabdi kulfi loaded with thick malai and saffron.', 35.00, 'PREMIUM', 'special_rabdi_kulfi.png', TRUE, FALSE, 60),
('butterscotch', 'Butterscotch Kulfi', 'Buttery caramel kulfi with a rich, velvety butterscotch flavour.', 30.00, 'CLASSIC', 'butterscotch_kulfi.png', TRUE, FALSE, 75),
('pista', 'Pista Kulfi', 'Classic pistachio kulfi enriched with real pista and cream.', 32.00, 'PREMIUM', 'pista_kulfi.png', TRUE, FALSE, 70)
ON CONFLICT (id) DO UPDATE
    SET name            = EXCLUDED.name,
        description     = EXCLUDED.description,
        base_price      = EXCLUDED.base_price,
        category        = EXCLUDED.category,
        image_url       = EXCLUDED.image_url,
        is_available    = EXCLUDED.is_available,
        is_seasonal     = EXCLUDED.is_seasonal,
        stock_quantity  = EXCLUDED.stock_quantity;

-- Day 9 Part 2: Simplify to 3 tiers only (BASIC, SILVER, GOLD)
-- Remove PLATINUM tier and consolidate all tiers

-- 1. First, convert tier column to varchar temporarily
ALTER TABLE app_user ALTER COLUMN tier TYPE varchar(20);

-- 2. Update all PLATINUM tiers to GOLD
UPDATE app_user SET tier = 'GOLD' WHERE tier = 'PLATINUM';

-- 3. Drop the old enum type
DROP TYPE IF EXISTS retailer_tier CASCADE;

-- 4. Create new enum with only 3 tiers
CREATE TYPE retailer_tier AS ENUM ('BASIC', 'SILVER', 'GOLD');

-- 5. Update price_override table if it exists
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'price_override' AND column_name = 'tier') THEN
        ALTER TABLE price_override ALTER COLUMN tier TYPE varchar(20);
        UPDATE price_override SET tier = 'GOLD' WHERE tier = 'PLATINUM';
        ALTER TABLE price_override ALTER COLUMN tier TYPE retailer_tier USING tier::retailer_tier;
    END IF;
END $$;

-- 6. Convert app_user tier column back to enum
ALTER TABLE app_user ALTER COLUMN tier TYPE retailer_tier USING tier::retailer_tier;

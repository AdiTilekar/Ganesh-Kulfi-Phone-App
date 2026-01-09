-- V19: Expand Order Status Values
-- Fix: Allow PACKED, OUT_FOR_DELIVERY, DELIVERED statuses

-- Step 1: Add new values to the order_status enum
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'PACKED';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'OUT_FOR_DELIVERY';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'DELIVERED';

-- Note: PostgreSQL doesn't allow removing enum values, and we need backward compatibility
-- The enum now has: PENDING, CONFIRMED, REJECTED, COMPLETED, CANCELLED, PACKED, OUT_FOR_DELIVERY, DELIVERED
-- This allows the order flow: PENDING -> CONFIRMED -> PACKED -> OUT_FOR_DELIVERY -> DELIVERED

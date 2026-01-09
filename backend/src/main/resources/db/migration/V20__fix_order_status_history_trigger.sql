-- Fix record_order_status_change() trigger to properly cast changed_by to UUID
CREATE OR REPLACE FUNCTION record_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Only record if status actually changed
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_status_history (
            order_id,
            old_status,
            new_status,
            changed_by,
            changed_by_role,
            reason,
            created_at
        ) VALUES (
            NEW.id,
            OLD.status::VARCHAR,
            NEW.status::VARCHAR,
            COALESCE(NEW.confirmed_by, NEW.rejected_by, NEW.cancelled_by, NEW.retailer_id),
            CASE 
                WHEN NEW.confirmed_by IS NOT NULL OR NEW.rejected_by IS NOT NULL OR NEW.cancelled_by IS NOT NULL THEN 'ADMIN'
                ELSE 'RETAILER'
            END,
            COALESCE(NEW.cancellation_reason, NEW.rejection_reason, NEW.factory_notes),
            CURRENT_TIMESTAMP
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

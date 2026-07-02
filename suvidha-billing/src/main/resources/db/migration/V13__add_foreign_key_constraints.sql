-- Add foreign key from transaction.bill_id to bill.id
-- The bill.id is UUID but transaction.bill_id was VARCHAR, so we first
-- alter the column type, then add the FK constraint.

-- Step 1: Convert bill_id from VARCHAR to UUID
ALTER TABLE billing.transaction
    ALTER COLUMN bill_id TYPE UUID USING bill_id::UUID;

-- Step 2: Add the FK constraint
ALTER TABLE billing.transaction
    ADD CONSTRAINT fk_transaction_bill
    FOREIGN KEY (bill_id) REFERENCES billing.bill(id)
    ON DELETE RESTRICT;

-- Add index on bill_id for FK lookup performance
CREATE INDEX IF NOT EXISTS idx_transaction_bill_id ON billing.transaction(bill_id);

-- Cross-schema FK: service_account.citizen_id → auth.citizens_table.id
-- Ensures every service account belongs to a real citizen.
-- ON DELETE SET NULL so citizen deletion doesn't cascade but leaves a trace.

ALTER TABLE billing.service_account
    ADD CONSTRAINT fk_service_account_citizen
    FOREIGN KEY (citizen_id) REFERENCES auth.citizens_table(id)
    ON DELETE SET NULL;

-- Add index on citizen_id for FK lookup performance
CREATE INDEX IF NOT EXISTS idx_service_account_citizen_id ON billing.service_account(citizen_id);

-- Drop the duplicate/unused account_link_requests table.
-- The code uses account_link_request (singular). This table was created
-- redundantly in V1__init_schema.sql and is never referenced by any repository.

DROP TABLE IF EXISTS billing.account_link_requests;


-- Drop the unique constraint on razorpay_order_id since multiple transactions
-- can share the same order when paying multiple bills in one checkout.
ALTER TABLE billing.transaction DROP CONSTRAINT IF EXISTS transaction_razorpay_order_id_key;

-- Add an index instead for efficient lookups by order ID
CREATE INDEX IF NOT EXISTS idx_transaction_razorpay_order_id ON billing.transaction(razorpay_order_id);

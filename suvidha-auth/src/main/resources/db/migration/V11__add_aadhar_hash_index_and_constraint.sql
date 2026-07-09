-- Add unique constraint on aadhar_hash (will be computed by application at startup)
ALTER TABLE citizens_table DROP CONSTRAINT IF EXISTS uk_citizens_aadhar_hash;
ALTER TABLE citizens_table ADD CONSTRAINT uk_citizens_aadhar_hash UNIQUE (aadhar_hash);
CREATE INDEX IF NOT EXISTS idx_citizens_aadhar_hash ON citizens_table(aadhar_hash);

-- Drop old blind index column
ALTER TABLE citizens_table DROP COLUMN IF EXISTS aadhar_blind_index;
DROP INDEX IF EXISTS idx_citizens_aadhar_blind;

ALTER TABLE citizens_table ADD COLUMN IF NOT EXISTS aadhar_blind_index VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS idx_citizens_aadhar_blind ON citizens_table(aadhar_blind_index);

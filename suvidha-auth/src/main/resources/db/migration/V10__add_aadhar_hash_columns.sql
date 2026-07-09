ALTER TABLE citizens_table ADD COLUMN IF NOT EXISTS encrypted_aadhar TEXT;
ALTER TABLE citizens_table ADD COLUMN IF NOT EXISTS aadhar_hash VARCHAR(64);
ALTER TABLE citizens_table ALTER COLUMN aadhar DROP NOT NULL;

-- Copy existing encrypted aadhar values to the new column
UPDATE citizens_table SET encrypted_aadhar = aadhar WHERE aadhar IS NOT NULL AND encrypted_aadhar IS NULL;

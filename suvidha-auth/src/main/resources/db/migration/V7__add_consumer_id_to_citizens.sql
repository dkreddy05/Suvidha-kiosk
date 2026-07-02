-- Database migration to add consumer_id to citizens_table
ALTER TABLE citizens_table ADD COLUMN IF NOT EXISTS consumer_id VARCHAR(50);

-- Backfill any existing citizens with a generated consumer ID starting with 'C' followed by 9 digits
UPDATE citizens_table SET consumer_id = 'C' || floor(random() * 900000000 + 100000000)::text WHERE consumer_id IS NULL;

-- Enforce NOT NULL and UNIQUE constraints on the consumer_id column
ALTER TABLE citizens_table ALTER COLUMN consumer_id SET NOT NULL;
ALTER TABLE citizens_table ADD CONSTRAINT uk_citizens_consumer_id UNIQUE (consumer_id);

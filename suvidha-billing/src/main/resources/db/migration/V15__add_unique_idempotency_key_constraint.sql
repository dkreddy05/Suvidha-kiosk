ALTER TABLE billing.transaction ADD CONSTRAINT uk_transaction_idempotency_key UNIQUE (idempotency_key);

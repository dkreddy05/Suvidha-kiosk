-- Complete billing schema matching JPA entities
CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE IF NOT EXISTS billing.service_account (
    id VARCHAR(255) PRIMARY KEY,
    version BIGINT,
    citizen_id VARCHAR(255) NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    account_no VARCHAR(255) NOT NULL,
    provider_name VARCHAR(255),
    address VARCHAR(512),
    registered_mobile VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_service_account_service_type_account_no UNIQUE (service_type, account_no)
);

CREATE TABLE IF NOT EXISTS billing.bill (
    id UUID PRIMARY KEY,
    bill_number VARCHAR(255) NOT NULL UNIQUE,
    account_id VARCHAR(255) NOT NULL,
    citizen_id VARCHAR(255) NOT NULL,
    meter_reading_id UUID NOT NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    remaining_balance NUMERIC(12,2) NOT NULL,
    late_fee NUMERIC(12,2),
    due_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    version BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_bill_account FOREIGN KEY (account_id) REFERENCES billing.service_account(id)
);

CREATE TABLE IF NOT EXISTS billing.transaction (
    id VARCHAR(255) PRIMARY KEY,
    bill_id VARCHAR(255) NOT NULL,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(255),
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    payment_method VARCHAR(255),
    account_id VARCHAR(255),
    citizen_id VARCHAR(255),
    transaction_id VARCHAR(255),
    idempotency_key VARCHAR(80),
    receipt_data TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_transaction_transaction_id UNIQUE (transaction_id)
);

CREATE TABLE IF NOT EXISTS billing.new_connection_request (
    id VARCHAR(255) PRIMARY KEY,
    citizen_id VARCHAR(255) NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    address VARCHAR(512) NOT NULL,
    property_type VARCHAR(64) NOT NULL,
    provider_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    ref_no VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS billing.account_verification_request (
    id VARCHAR(255) PRIMARY KEY,
    citizen_id VARCHAR(128) NOT NULL,
    consumer_no VARCHAR(128) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    registered_mobile VARCHAR(32) NOT NULL,
    address VARCHAR(512) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ref_no VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing.account_link_request (
    id UUID PRIMARY KEY,
    citizen_id VARCHAR(255) NOT NULL,
    account_no VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    utility_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing.outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(255),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_status_created ON billing.outbox_event(status, created_at);

CREATE TABLE IF NOT EXISTS billing.kiosk_log (
    id UUID PRIMARY KEY,
    kiosk_id VARCHAR(255) NOT NULL,
    citizen_id VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    path VARCHAR(255),
    ref_no VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS billing.meter_readings (
    id BIGSERIAL PRIMARY KEY,
    utility_account_id BIGINT NOT NULL,
    reading_value DECIMAL(12,2) NOT NULL,
    reading_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS billing.payment_locks (
    id BIGSERIAL PRIMARY KEY,
    bill_id VARCHAR(255) NOT NULL UNIQUE,
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS billing.account_link_requests (
    id BIGSERIAL PRIMARY KEY,
    citizen_id VARCHAR(255) NOT NULL,
    account_no VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    utility_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing.notification_logs (
    id BIGSERIAL PRIMARY KEY,
    bill_id VARCHAR(255),
    citizen_id VARCHAR(255),
    notification_type VARCHAR(50),
    status VARCHAR(50),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_account_citizen ON billing.service_account(citizen_id);
CREATE INDEX idx_bill_account ON billing.bill(account_id);
CREATE INDEX idx_transaction_bill ON billing.transaction(bill_id);

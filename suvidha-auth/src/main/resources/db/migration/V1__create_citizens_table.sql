-- Complete auth schema matching JPA entities
CREATE SCHEMA IF NOT EXISTS auth;

-- Tables in public schema (no schema prefix in entity)
CREATE TABLE IF NOT EXISTS citizens_table (
    id VARCHAR(36) PRIMARY KEY,
    mobile VARCHAR(15) NOT NULL,
    aadhar TEXT,
    name VARCHAR(255),
    language_preference VARCHAR(10),
    role VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_citizens_mobile UNIQUE (mobile),
    CONSTRAINT uk_citizens_aadhar UNIQUE (aadhar)
);
CREATE INDEX IF NOT EXISTS idx_citizens_mobile ON citizens_table(mobile);

CREATE TABLE IF NOT EXISTS jwt_key_versions (
    kid VARCHAR(36) PRIMARY KEY,
    public_key TEXT,
    private_key TEXT,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_jwt_keys_active ON jwt_key_versions(is_active);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    citizen_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_refresh_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_citizen_id ON refresh_tokens(citizen_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_tokens(expires_at);

-- Tables in auth schema
CREATE TABLE IF NOT EXISTS auth.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    citizen_id VARCHAR(36) NOT NULL,
    details TEXT,
    ip_address VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON auth.audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_citizen_id ON auth.audit_logs(citizen_id);

CREATE TABLE IF NOT EXISTS auth.consent_records (
    consent_id VARCHAR(36) PRIMARY KEY,
    citizen_id VARCHAR(36) NOT NULL,
    consent_type VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(64),
    user_agent TEXT
);

CREATE TABLE IF NOT EXISTS auth.data_breaches (
    breach_id VARCHAR(36) PRIMARY KEY,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE,
    affected_count INT,
    description TEXT,
    remediation TEXT
);

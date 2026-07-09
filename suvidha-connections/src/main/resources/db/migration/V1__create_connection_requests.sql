CREATE TABLE connection_request (
    id UUID PRIMARY KEY,
    display_id VARCHAR(50) NOT NULL UNIQUE,
    citizen_id VARCHAR(50) NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    address VARCHAR(512) NOT NULL,
    status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    estimated_days INT NOT NULL,
    version BIGINT NOT NULL
);

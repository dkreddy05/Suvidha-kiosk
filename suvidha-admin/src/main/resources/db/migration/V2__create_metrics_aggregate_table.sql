CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE IF NOT EXISTS admin.metrics_aggregate (
    id BIGSERIAL PRIMARY KEY,
    metric_type VARCHAR(50) NOT NULL,
    dimension VARCHAR(100),
    total_count BIGINT NOT NULL DEFAULT 0,
    numeric_value DOUBLE PRECISION,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_metrics_type_dimension UNIQUE (metric_type, dimension)
);

CREATE INDEX IF NOT EXISTS idx_metrics_aggregate_type ON admin.metrics_aggregate(metric_type);

-- ShedLock table (standard schema from ShedLock documentation)
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- DB-level safety net: at most ONE active key can exist at any time.
-- This makes the race impossible even if ShedLock is misconfigured later.
CREATE UNIQUE INDEX uq_active_key ON jwt_key_versions(is_active) WHERE is_active = true;

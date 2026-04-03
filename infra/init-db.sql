-- =============================================================
--  SUVIDHA – Initial Database Bootstrap
--  Runs on first container start via docker-entrypoint-initdb.d
-- =============================================================

-- Auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Billing schema
CREATE SCHEMA IF NOT EXISTS billing;

-- Grievance schema
CREATE SCHEMA IF NOT EXISTS grievance;

-- Connections schema
CREATE SCHEMA IF NOT EXISTS connections;

-- Notification schema
CREATE SCHEMA IF NOT EXISTS notification;

-- Admin schema
CREATE SCHEMA IF NOT EXISTS admin;

-- Grant access to the suvidha user on all schemas
GRANT ALL PRIVILEGES ON SCHEMA auth        TO suvidha;
GRANT ALL PRIVILEGES ON SCHEMA billing     TO suvidha;
GRANT ALL PRIVILEGES ON SCHEMA grievance   TO suvidha;
GRANT ALL PRIVILEGES ON SCHEMA connections TO suvidha;
GRANT ALL PRIVILEGES ON SCHEMA notification TO suvidha;
GRANT ALL PRIVILEGES ON SCHEMA admin       TO suvidha;

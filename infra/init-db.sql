-- =============================================================
--  SUVIDHA – Isolated Databases Initialization
--  Runs on first container start via docker-entrypoint-initdb.d
-- =============================================================

-- Create separate databases for microservice isolation
CREATE DATABASE auth_db;
CREATE DATABASE billing_db;
CREATE DATABASE connections_db;
CREATE DATABASE grievance_db;
CREATE DATABASE notification_db;
CREATE DATABASE analytics_db;

-- Create isolated database users
CREATE USER auth_user WITH PASSWORD 'auth_pass';
CREATE USER billing_user WITH PASSWORD 'billing_pass';
CREATE USER connections_user WITH PASSWORD 'connections_pass';
CREATE USER grievance_user WITH PASSWORD 'grievance_pass';
CREATE USER notification_user WITH PASSWORD 'notification_pass';
CREATE USER admin_user WITH PASSWORD 'admin_pass';

-- Assign ownership to allow microservices to perform Flyway migrations and schema modifications
ALTER DATABASE auth_db OWNER TO auth_user;
ALTER DATABASE billing_db OWNER TO billing_user;
ALTER DATABASE connections_db OWNER TO connections_user;
ALTER DATABASE grievance_db OWNER TO grievance_user;
ALTER DATABASE notification_db OWNER TO notification_user;
ALTER DATABASE analytics_db OWNER TO admin_user;

-- Connect to grievance_db and create schema since Hibernate ddl-auto doesn't auto-create non-default schemas
\c grievance_db
CREATE SCHEMA IF NOT EXISTS grievance;
ALTER SCHEMA grievance OWNER TO grievance_user;

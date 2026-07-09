-- Seed the first ADMIN account if no admin exists
-- The password/secret mechanism for initial admin is gated behind env flag.
-- This creates a placeholder admin that must be activated via the CLI tool.
INSERT INTO citizens_table (id, mobile, aadhar, aadhar_hash, name, language_preference, role, consumer_id, created_at)
SELECT
    'admin-bootstrap-000001',
    '+910000000000',
    'ADMIN_BOOTSTRAP_ENCRYPTED',
    'ADMIN_BOOTSTRAP_HASH',
    'System Administrator',
    'en',
    'ADMIN',
    'C_ADMIN_001',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM citizens_table WHERE role = 'ADMIN'
);

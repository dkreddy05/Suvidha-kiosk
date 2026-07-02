# PostgreSQL Consistency Test Suite

## Architecture Overview

All services share a single PostgreSQL database (`suvidha_auth_db`) with **schema isolation**:

| Schema | Service | Key Tables |
|--------|---------|------------|
| `public` | Auth | `citizens_table`, `jwt_key_versions`, `refresh_tokens` |
| `auth` | Auth | `audit_logs`, `consent_records`, `data_breaches` |
| `billing` | Billing | `service_account`, `bill`, `transaction`, `payment_locks`, `account_link_request`, `account_link_requests`, `outbox_event`, `meter_readings`, `notification_logs`, `kiosk_log`, `new_connection_request`, `account_verification_request` |
| `grievance` | Grievance | `grievances`, `grievance_updates` |
| `notification` | Notification | `notifications` |
| `connections` | Connections | (no migrations — Hibernate auto-DDL) |
| `admin` | Admin | (no migrations — Hibernate auto-DDL) |

**Cross-service data flow:**
- `citizens_table.id` (auth) → referenced as `citizen_id` in billing, grievance, notification (no FK constraints)
- `billing.service_account.id` → `billing.bill.account_id` (FK exists)
- `billing.bill.id` → referenced in `billing.transaction.bill_id` (no FK constraint)
- `grievance.grievances.id` → `grievance.grievance_updates.grievance_id` (FK exists)

---

## Test 1: Orphaned Records

### What to check
Records in child tables that reference non-existent parent records across schemas.

### Queries

```sql
-- 1a. Bills referencing non-existent service accounts
SELECT b.id, b.account_id, b.citizen_id
FROM billing.bill b
LEFT JOIN billing.service_account sa ON b.account_id = sa.id
WHERE sa.id IS NULL;
-- Expected: 0 rows

-- 1b. Transactions referencing non-existent bills
SELECT t.id, t.bill_id, t.razorpay_order_id, t.status
FROM billing.transaction t
LEFT JOIN billing.bill b ON t.bill_id = b.id::varchar
WHERE b.id IS NULL;
-- Expected: 0 rows

-- 1c. Service accounts referencing non-existent citizens
SELECT sa.id, sa.citizen_id, sa.account_no, sa.service_type
FROM billing.service_account sa
LEFT JOIN auth.citizens_table c ON sa.citizen_id = c.id
WHERE c.id IS NULL;
-- Expected: 0 rows (citizens_table is in auth schema)

-- 1d. Grievances referencing non-existent citizens
SELECT g.id, g.citizen_id, g.reference_number
FROM grievance.grievances g
LEFT JOIN auth.citizens_table c ON g.citizen_id = c.id
WHERE c.id IS NULL;
-- Expected: 0 rows

-- 1e. Notifications referencing non-existent citizens
SELECT n.notification_id, n.citizen_id, n.message_type
FROM notification.notifications n
LEFT JOIN auth.citizens_table c ON n.citizen_id = c.id
WHERE c.id IS NULL;
-- Expected: 0 rows

-- 1f. Refresh tokens referencing non-existent citizens
SELECT rt.id, rt.citizen_id, rt.token
FROM auth.refresh_tokens rt
LEFT JOIN auth.citizens_table c ON rt.citizen_id = c.id
WHERE c.id IS NULL;
-- Expected: 0 rows

-- 1g. Grievance updates referencing non-existent grievances
SELECT gu.id, gu.grievance_id, gu.status
FROM grievance.grievance_updates gu
LEFT JOIN grievance.grievances g ON gu.grievance_id = g.id
WHERE g.id IS NULL;
-- Expected: 0 rows

-- 1h. Account link requests referencing non-existent citizens
SELECT alr.id, alr.citizen_id, alr.account_no, alr.status
FROM billing.account_link_request alr
LEFT JOIN citizens_table c ON alr.citizen_id = c.id
WHERE c.id IS NULL;
-- Expected: 0 rows
```

### How to trigger
- Delete a citizen without cascading: `DELETE FROM citizens_table WHERE id = 'xxx';`
- Force-delete a service account: `DELETE FROM billing.service_account WHERE id = 'xxx';`
- Kill the process mid-transaction during payment confirmation

---

## Test 2: Failed Transaction Rollback

### What to check
After a failed operation, no partial data should remain. All writes within a `@Transactional` boundary must be fully rolled back.

### Queries

```sql
-- 2a. Check for transactions in CREATED status with no corresponding payment
-- These should exist (pending payments), but verify they have valid order IDs
SELECT t.id, t.bill_id, t.razorpay_order_id, t.status, t.created_at
FROM billing.transaction t
WHERE t.status = 'CREATED'
ORDER BY t.created_at DESC
LIMIT 20;

-- 2b. Check for bills with PAID status but no CAPTURED transaction
SELECT b.id, b.bill_number, b.status, b.amount_paid, b.remaining_balance
FROM billing.bill b
WHERE b.status = 'PAID'
  AND NOT EXISTS (
    SELECT 1 FROM billing.transaction t
    WHERE t.bill_id = b.id::varchar AND t.status = 'CAPTURED'
  );
-- Expected: 0 rows (every PAID bill should have a CAPTURED transaction)

-- 2c. Check for CAPTURED transactions but bill still shows PENDING/OVERDUE
SELECT t.id AS txn_id, t.bill_id, t.status AS txn_status,
       b.status AS bill_status, b.amount_paid, b.remaining_balance
FROM billing.transaction t
JOIN billing.bill b ON t.bill_id = b.id::varchar
WHERE t.status = 'CAPTURED'
  AND b.status != 'PAID';
-- Expected: 0 rows

-- 2d. Check for expired payment locks still held
SELECT pl.id, pl.bill_id, pl.locked_at, pl.expires_at, pl.locked_by
FROM billing.payment_locks pl
WHERE pl.expires_at < NOW();
-- Expected: 0 rows (expired locks should be cleaned up)

-- 2e. Check for outbox events stuck in PENDING state
SELECT oe.id, oe.event_type, oe.aggregate_id, oe.retry_count, oe.created_at, oe.error_message
FROM billing.outbox_event oe
WHERE oe.status = 'PENDING'
  AND oe.created_at < NOW() - INTERVAL '5 minutes';
-- Expected: 0 rows (stuck events should be retried or marked FAILED)

-- 2f. Check for account link requests in OTP_SENT with expired OTP context
-- (OTP context is in Redis, so check for stale requests)
SELECT alr.id, alr.citizen_id, alr.account_no, alr.status, alr.created_at
FROM billing.account_link_request alr
WHERE alr.status = 'OTP_SENT'
  AND alr.created_at < NOW() - INTERVAL '10 minutes';
-- Expected: 0 rows (OTP TTL is 5 minutes)
```

### How to trigger
- Send a payment confirmation with invalid signature → should rollback all bill updates
- Kill the billing service during `confirmPayment()` → check for partial state
- Send a confirmLink with invalid OTP → should rollback account linking
- Throw an exception during `linkAccount()` in BillingFacadeServiceImpl

---

## Test 3: Duplicate Citizen Records

### What to check
The `citizens_table` has unique constraints on `mobile` and `aadhar`. Verify no duplicates exist.

### Queries

```sql
-- 3a. Duplicate mobile numbers
SELECT mobile, COUNT(*) as cnt
FROM auth.citizens_table
GROUP BY mobile
HAVING COUNT(*) > 1;
-- Expected: 0 rows (uk_citizens_mobile UNIQUE constraint)

-- 3b. Duplicate Aadhar numbers
SELECT aadhar, COUNT(*) as cnt
FROM auth.citizens_table
WHERE aadhar IS NOT NULL
GROUP BY aadhar
HAVING COUNT(*) > 1;
-- Expected: 0 rows (uk_citizens_aadhar UNIQUE constraint)

-- 3c. Citizens with same name + mobile but different IDs (soft duplicates)
SELECT name, mobile, COUNT(*) as cnt
FROM auth.citizens_table
GROUP BY name, mobile
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- 3d. Check Aadhar encryption consistency
SELECT id, mobile, name, aadhar
FROM auth.citizens_table
WHERE aadhar IS NULL
  AND name IS NOT NULL;
-- Expected: depends on business logic — some users may register without Aadhar
```

### How to trigger
- Concurrent registration requests with the same mobile number
- Bypass unique constraint by directly inserting into DB
- Race condition in `AuthenticationServiceImpl.register()` if not properly synchronized

---

## Test 4: Inconsistent Payment State

### What to check
Payment state must be consistent across `transaction`, `bill`, and `payment_locks` tables.

### Queries

```sql
-- 4a. Payment state consistency: amount_paid + remaining_balance = total_amount
SELECT b.id, b.bill_number, b.total_amount, b.amount_paid, b.remaining_balance,
       (b.amount_paid + b.remaining_balance) as calculated_total,
       (b.total_amount - (b.amount_paid + b.remaining_balance)) as discrepancy
FROM billing.bill b
WHERE ABS(b.total_amount - (b.amount_paid + b.remaining_balance)) > 0.01;
-- Expected: 0 rows

-- 4b. PAID bills should have remaining_balance = 0
SELECT b.id, b.bill_number, b.status, b.remaining_balance
FROM billing.bill b
WHERE b.status = 'PAID' AND b.remaining_balance != 0;
-- Expected: 0 rows

-- 4c. Bills with amount_paid > total_amount (overpayment)
SELECT b.id, b.bill_number, b.total_amount, b.amount_paid
FROM billing.bill b
WHERE b.amount_paid > b.total_amount;
-- Expected: 0 rows

-- 4d. Multiple CAPTURED transactions for the same bill (double payment)
SELECT t.bill_id, COUNT(*) as captured_count
FROM billing.transaction t
WHERE t.status = 'CAPTURED'
GROUP BY t.bill_id
HAVING COUNT(*) > 1;
-- Expected: 0 rows (the pessimistic lock should prevent this)

-- 4e. Transactions with duplicate idempotency keys but different payment IDs
SELECT t.idempotency_key, COUNT(*) as cnt,
       STRING_AGG(DISTINCT t.razorpay_payment_id, ', ') as payment_ids
FROM billing.transaction t
WHERE t.idempotency_key IS NOT NULL
GROUP BY t.idempotency_key
HAVING COUNT(DISTINCT t.razorpay_payment_id) > 1;
-- Expected: 0 rows

-- 4f. Transactions with duplicate razorpay_order_id (should be impossible due to UNIQUE constraint)
SELECT razorpay_order_id, COUNT(*) as cnt
FROM billing.transaction
GROUP BY razorpay_order_id
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- 4g. Transactions with duplicate transaction_id
SELECT transaction_id, COUNT(*) as cnt
FROM billing.transaction
WHERE transaction_id IS NOT NULL
GROUP BY transaction_id
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- 4h. Check optimistic locking: bills with same version but different updated_at
-- (This would indicate a lost update)
SELECT b.id, b.bill_number, b.version, b.status, b.updated_at
FROM billing.bill b
WHERE b.version IN (
    SELECT version FROM billing.bill GROUP BY id, version HAVING COUNT(*) > 1
);
-- Expected: 0 rows
```

### How to trigger
- Send two concurrent `confirmPayment` requests with the same orderId
- The pessimistic lock (`findAllByRazorpayOrderIdForUpdate`) should serialize these
- Check if the second request sees the first's changes
- Simulate network timeout between transaction update and bill update

---

## Test 5: Race Conditions During Updates

### What to check
Concurrent updates to the same entity should not cause lost updates or dirty reads.

### Queries

```sql
-- 5a. Service accounts with version conflicts (optimistic locking)
-- Check for accounts where version doesn't match the number of updates
SELECT sa.id, sa.citizen_id, sa.version, sa.updated_at, sa.created_at
FROM billing.service_account sa
WHERE sa.version IS NOT NULL AND sa.version < 0;
-- Expected: 0 rows

-- 5b. Bills with version anomalies
SELECT b.id, b.bill_number, b.version, b.status, b.updated_at
FROM billing.bill b
WHERE b.version IS NOT NULL AND b.version < 0;
-- Expected: 0 rows

-- 5c. Check for concurrent new connection requests with same ref_no
SELECT ref_no, COUNT(*) as cnt
FROM billing.new_connection_request
GROUP BY ref_no
HAVING COUNT(*) > 1;
-- Expected: 0 rows (ref_no has UNIQUE constraint)

-- 5d. Check for concurrent account verification requests with same ref_no
SELECT ref_no, COUNT(*) as cnt
FROM billing.account_verification_request
GROUP BY ref_no
HAVING COUNT(*) > 1;
-- Expected: 0 rows (ref_no has UNIQUE constraint)

-- 5e. Stale reads: transactions updated before their associated bills
SELECT t.id AS txn_id, t.updated_at AS txn_updated,
       b.updated_at AS bill_updated,
       EXTRACT(EPOCH FROM (b.updated_at - t.updated_at)) AS lag_seconds
FROM billing.transaction t
JOIN billing.bill b ON t.bill_id = b.id::varchar
WHERE t.status = 'CAPTURED'
  AND b.updated_at < t.updated_at;
-- Expected: 0 rows (bill should be updated after or at same time as transaction)

-- 5f. Check for payment locks held during active transactions
SELECT pl.bill_id, pl.locked_at, pl.expires_at, t.status AS txn_status
FROM billing.payment_locks pl
JOIN billing.transaction t ON pl.bill_id = t.bill_id
WHERE pl.expires_at > NOW()
  AND t.status IN ('CAPTURED', 'FAILED')
  AND t.updated_at > pl.expires_at;
-- Expected: 0 rows (locks should be released after transaction completes)
```

### How to trigger
- Use `pg_isolation_test` or concurrent curl requests to the same endpoint
- Send 10 concurrent `confirmPayment` requests for the same orderId
- Send 5 concurrent `linkAccount` requests for the same citizen + account
- The pessimistic write lock on transactions should serialize payment confirmations
- The optimistic lock (`@Version`) on bills should prevent lost updates

---

## Test 6: Concurrent Grievance Submissions

### What to check
Multiple grievances submitted simultaneously by the same citizen should all be recorded correctly.

### Queries

```sql
-- 6a. Duplicate reference numbers (should be impossible due to UNIQUE constraint)
SELECT reference_number, COUNT(*) as cnt
FROM grievance.grievances
GROUP BY reference_number
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- 6b. Grievances without initial status update
SELECT g.id, g.reference_number, g.status, g.submitted_at
FROM grievance.grievances g
WHERE NOT EXISTS (
    SELECT 1 FROM grievance.grievance_updates gu
    WHERE gu.grievance_id = g.id AND gu.status = 'SUBMITTED'
);
-- Expected: 0 rows (every grievance should have an initial SUBMITTED update)

-- 6c. Grievance updates referencing wrong grievance status
SELECT gu.id, gu.grievance_id, gu.status AS update_status, g.status AS grievance_status
FROM grievance.grievance_updates gu
JOIN grievance.grievances g ON gu.grievance_id = g.id
WHERE gu.status != g.status
  AND gu.updated_at = g.submitted_at;
-- Expected: 0 rows (initial update should match grievance status)

-- 6d. Grievances with no updates at all (orphaned parent)
SELECT g.id, g.reference_number, g.citizen_id
FROM grievance.grievances g
WHERE NOT EXISTS (
    SELECT 1 FROM grievance.grievance_updates gu
    WHERE gu.grievance_id = g.id
);
-- Expected: 0 rows (CascadeType.ALL ensures updates are saved with grievance)

-- 6e. Concurrent submissions: check for timestamps within 1ms window
SELECT g1.reference_number AS ref1, g2.reference_number AS ref2,
       g1.citizen_id, g2.citizen_id,
       g1.submitted_at, g2.submitted_at,
       EXTRACT(EPOCH FROM (g2.submitted_at - g1.submitted_at)) * 1000 AS ms_diff
FROM grievance.grievances g1
JOIN grievance.grievances g2 ON g1.citizen_id = g2.citizen_id
  AND g1.id < g2.id
  AND ABS(EXTRACT(EPOCH FROM (g2.submitted_at - g1.submitted_at))) < 0.001
ORDER BY g1.submitted_at;
-- Expected: depends on test — if concurrent, both should exist with unique ref numbers

-- 6f. Grievances with future timestamps (clock skew or injection)
SELECT id, reference_number, submitted_at, updated_at
FROM grievance.grievances
WHERE submitted_at > NOW() + INTERVAL '1 minute';
-- Expected: 0 rows
```

### How to trigger
- Send 10 concurrent POST requests to `/api/v1/grievance/complaints` with same citizen JWT
- The reference number is generated as `GRV-{timestamp}-{random8}` — collisions are unlikely but check
- Each grievance should have a unique `reference_number` and a `SUBMITTED` update record

---

## Test 7: Account Linking Duplication

### What to check
Account linking should not create duplicate service accounts or allow the same account to be linked to multiple citizens.

### Queries

```sql
-- 7a. Duplicate service accounts (same service_type + account_no)
SELECT service_type, account_no, COUNT(*) as cnt
FROM billing.service_account
GROUP BY service_type, account_no
HAVING COUNT(*) > 1;
-- Expected: 0 rows (uk_service_account_service_type_account_no UNIQUE constraint)

-- 7b. Same account linked to multiple citizens
SELECT sa.service_type, sa.account_no,
       COUNT(DISTINCT sa.citizen_id) as citizen_count,
       STRING_AGG(DISTINCT sa.citizen_id, ', ') as citizen_ids
FROM billing.service_account sa
WHERE sa.is_active = true
GROUP BY sa.service_type, sa.account_no
HAVING COUNT(DISTINCT sa.citizen_id) > 1;
-- Expected: 0 rows (business rule: one account → one citizen)

-- 7c. Account link requests with duplicate citizen + account + status = COMPLETED
SELECT alr.citizen_id, alr.account_no, alr.utility_type,
       COUNT(*) as completed_count
FROM billing.account_link_request alr
WHERE alr.status = 'COMPLETED'
GROUP BY alr.citizen_id, alr.account_no, alr.utility_type
HAVING COUNT(*) > 1;
-- Expected: 0 rows (or 1 if re-linking is allowed)

-- 7d. Service accounts with citizen_id different from registered_mobile's citizen
-- (This checks if the account was linked to a citizen whose mobile doesn't match)
SELECT sa.id, sa.citizen_id, sa.registered_mobile, sa.account_no, sa.service_type,
       c.mobile AS citizen_mobile
FROM billing.service_account sa
LEFT JOIN auth.citizens_table c ON sa.citizen_id = c.id
WHERE sa.is_active = true
  AND c.mobile IS NOT NULL
  AND sa.registered_mobile != c.mobile;
-- Expected: depends — this may be valid if mobile changed after registration

-- 7e. Orphaned account link requests (no corresponding service account)
SELECT alr.id, alr.citizen_id, alr.account_no, alr.utility_type, alr.status
FROM billing.account_link_request alr
WHERE alr.status = 'COMPLETED'
  AND NOT EXISTS (
    SELECT 1 FROM billing.service_account sa
    WHERE sa.account_no = alr.account_no
      AND sa.service_type = alr.utility_type::varchar
  );
-- Expected: 0 rows

-- 7f. Check the duplicate table: account_link_request vs account_link_requests
-- (There are TWO tables with similar schemas — check for inconsistency)
SELECT 'account_link_request' AS table_name, COUNT(*) as cnt FROM billing.account_link_request
UNION ALL
SELECT 'account_link_requests' AS table_name, COUNT(*) as cnt FROM billing.account_link_requests;
-- Both tables should be in sync or one should be deprecated

-- 7g. Cross-table consistency: check if COMPLETED link requests have matching service accounts
SELECT alr.id AS link_req_id, alr.citizen_id, alr.account_no, alr.utility_type,
       sa.id AS account_id, sa.citizen_id AS account_citizen_id
FROM billing.account_link_request alr
LEFT JOIN billing.service_account sa ON alr.account_no = sa.account_no
  AND alr.utility_type = sa.service_type::varchar
WHERE alr.status = 'COMPLETED'
  AND (sa.id IS NULL OR alr.citizen_id != sa.citizen_id);
-- Expected: 0 rows (COMPLETED link should result in account with matching citizen_id)
```

### How to trigger
- Send concurrent `verifyOwnership` requests for the same account from different citizens
- The `confirmLink` flow uses OTP — try to link the same account twice simultaneously
- Check if `BillingFacadeServiceImpl.linkAccount()` creates duplicates when called concurrently
- The `orElseGet()` pattern in `linkAccount` has a TOCTOU race condition:
  ```java
  ServiceAccount account = serviceAccountRepository
      .findByAccountNoAndServiceType(...)
      .orElseGet(() -> ServiceAccount.builder()...build());
  account.setCitizenId(citizenId);
  serviceAccountRepository.save(account);
  ```
  Two concurrent requests could both find no account, both create new ones, and one overwrites the other.

---

## Master Diagnostic Query

Run this single query to get a health summary of all consistency checks:

```sql
WITH checks AS (
    SELECT 'Orphaned bills (no account)' AS check_name,
           COUNT(*) AS issue_count
    FROM billing.bill b
    LEFT JOIN billing.service_account sa ON b.account_id = sa.id
    WHERE sa.id IS NULL

    UNION ALL

    SELECT 'Orphaned transactions (no bill)',
           COUNT(*)
    FROM billing.transaction t
    LEFT JOIN billing.bill b ON t.bill_id = b.id::varchar
    WHERE b.id IS NULL

    UNION ALL

    SELECT 'Duplicate citizen mobiles',
           COUNT(*)
    FROM (SELECT mobile FROM auth.citizens_table GROUP BY mobile HAVING COUNT(*) > 1) x

    UNION ALL

    SELECT 'Duplicate citizen aadhars',
           COUNT(*)
    FROM (SELECT aadhar FROM auth.citizens_table WHERE aadhar IS NOT NULL GROUP BY aadhar HAVING COUNT(*) > 1) x

    UNION ALL

    SELECT 'PAID bills without CAPTURED txn',
           COUNT(*)
    FROM billing.bill b
    WHERE b.status = 'PAID'
      AND NOT EXISTS (SELECT 1 FROM billing.transaction t WHERE t.bill_id = b.id::varchar AND t.status = 'CAPTURED')

    UNION ALL

    SELECT 'CAPTURED txn with non-PAID bill',
           COUNT(*)
    FROM billing.transaction t
    JOIN billing.bill b ON t.bill_id = b.id::varchar
    WHERE t.status = 'CAPTURED' AND b.status != 'PAID'

    UNION ALL

    SELECT 'Payment amount mismatch',
           COUNT(*)
    FROM billing.bill b
    WHERE ABS(b.total_amount - (b.amount_paid + b.remaining_balance)) > 0.01

    UNION ALL

    SELECT 'Duplicate grievance ref numbers',
           COUNT(*)
    FROM (SELECT reference_number FROM grievance.grievances GROUP BY reference_number HAVING COUNT(*) > 1) x

    UNION ALL

    SELECT 'Grievances without updates',
           COUNT(*)
    FROM grievance.grievances g
    WHERE NOT EXISTS (SELECT 1 FROM grievance.grievance_updates gu WHERE gu.grievance_id = g.id)

    UNION ALL

    SELECT 'Duplicate service accounts',
           COUNT(*)
    FROM (SELECT service_type, account_no FROM billing.service_account GROUP BY service_type, account_no HAVING COUNT(*) > 1) x

    UNION ALL

    SELECT 'Account linked to multiple citizens',
           COUNT(*)
    FROM (SELECT service_type, account_no FROM billing.service_account WHERE is_active = true GROUP BY service_type, account_no HAVING COUNT(DISTINCT citizen_id) > 1) x

    UNION ALL

    SELECT 'Expired payment locks',
           COUNT(*)
    FROM billing.payment_locks
    WHERE expires_at < NOW()

    UNION ALL

    SELECT 'Stuck PENDING outbox events',
           COUNT(*)
    FROM billing.outbox_event
    WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '5 minutes'

    UNION ALL

    SELECT 'Orphaned service accounts (no citizen)', COUNT(*)
    FROM billing.service_account sa
    LEFT JOIN auth.citizens_table c ON sa.citizen_id = c.id
    WHERE c.id IS NULL

    UNION ALL

    SELECT 'Orphaned grievances (no citizen)', COUNT(*)
    FROM grievance.grievances g
    LEFT JOIN auth.citizens_table c ON g.citizen_id = c.id
    WHERE c.id IS NULL

    UNION ALL

    SELECT 'Orphaned notifications (no citizen)', COUNT(*)
    FROM notification.notifications n
    LEFT JOIN auth.citizens_table c ON n.citizen_id = c.id
    WHERE c.id IS NULL

    UNION ALL

    SELECT 'Orphaned refresh tokens (no citizen)', COUNT(*)
    FROM auth.refresh_tokens rt
    LEFT JOIN auth.citizens_table c ON rt.citizen_id = c.id
    WHERE c.id IS NULL

    UNION ALL

    SELECT 'Orphaned grievance updates', COUNT(*)
    FROM grievance.grievance_updates gu
    LEFT JOIN grievance.grievances g ON gu.grievance_id = g.id
    WHERE g.id IS NULL
)
SELECT
    check_name,
    issue_count,
    CASE WHEN issue_count = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM checks
ORDER BY
    CASE WHEN issue_count > 0 THEN 0 ELSE 1 END,
    check_name;
```

### Live Results (Clean Database)

All 18 checks **PASS** on the initialized database:

```
 check_name               | issue_count | status 
--------------------------+-------------+--------
 Account linked to multiple citizens    |           0 | PASS
 CAPTURED txn with non-PAID bill        |           0 | PASS
 Duplicate citizen aadhars              |           0 | PASS
 Duplicate citizen mobiles              |           0 | PASS
 Duplicate grievance ref numbers        |           0 | PASS
 Duplicate service accounts             |           0 | PASS
 Expired payment locks                  |           0 | PASS
 Grievances without updates             |           0 | PASS
 Orphaned bills (no account)            |           0 | PASS
 Orphaned grievance updates             |           0 | PASS
 Orphaned grievances (no citizen)       |           0 | PASS
 Orphaned notifications (no citizen)    |           0 | PASS
 Orphaned refresh tokens (no citizen)   |           0 | PASS
 Orphaned service accounts (no citizen) |           0 | PASS
 Orphaned transactions (no bill)        |           0 | PASS
 PAID bills without CAPTURED txn        |           0 | PASS
 Payment amount mismatch                |           0 | PASS
 Stuck PENDING outbox events            |           0 | PASS
```

---

## How to Run These Tests

### Option 1: Direct psql
```bash
docker exec suvidha-postgres psql -U suvidha -d suvidha_auth_db -f consistency_checks.sql
```

### Option 2: One-liner for a specific check
```bash
docker exec suvidha-postgres psql -U suvidha -d suvidha_auth_db -c "
SELECT b.id, b.bill_number, b.status, b.remaining_balance
FROM billing.bill b
WHERE b.status = 'PAID' AND b.remaining_balance != 0;
"
```

### Option 3: Automated test script
```bash
#!/bin/bash
DB="docker exec suvidha-postgres psql -U suvidha -d suvidha_auth_db -t -A -c"

echo "=== PostgreSQL Consistency Checks ==="

# Check 1: Orphaned bills
result=$($DB "SELECT COUNT(*) FROM billing.bill b LEFT JOIN billing.service_account sa ON b.account_id = sa.id WHERE sa.id IS NULL;")
echo "Orphaned bills: $result (expected: 0)"

# Check 2: Duplicate citizens
result=$($DB "SELECT COUNT(*) FROM (SELECT mobile FROM auth.citizens_table GROUP BY mobile HAVING COUNT(*) > 1) x;")
echo "Duplicate citizen mobiles: $result (expected: 0)"

# ... repeat for all checks
```

---

## Key Vulnerabilities Found in Code

### 1. TOCTOU Race in `BillingFacadeServiceImpl.linkAccount()`
```java
ServiceAccount account = repository.findByAccountNoAndServiceType(...)
    .orElseGet(() -> ServiceAccount.builder()...build());
// Another thread could have created the same account between find and save
account.setCitizenId(citizenId);
repository.save(account);
```
**Fix:** Use `SELECT ... FOR UPDATE` or a database-level upsert (`INSERT ... ON CONFLICT DO UPDATE`).

### 2. No FK from `transaction.bill_id` to `bill.id`
The transaction table references bills by string ID but has no foreign key constraint. A transaction could reference a deleted bill.

### 3. No FK from `service_account.citizen_id` to `citizens_table.id`
Service accounts can reference non-existent citizens. Cross-schema FKs are possible in PostgreSQL but not configured.

### 4. Double table for account link requests
Both `billing.account_link_request` and `billing.account_link_requests` exist. Code uses `account_link_request` but the second table is unused, creating potential confusion.

### 5. Grievance reference number collision risk
Reference numbers use `GRV-{epochMillis}-{random8}`. Under high concurrency, two submissions in the same millisecond with the same random substring would collide on the UNIQUE constraint, causing one to fail with a 500 error instead of a retry.

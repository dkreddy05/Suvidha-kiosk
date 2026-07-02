# Dangerous Logging Mistakes — Production Anti-Patterns

## 1. Logging Sensitive Data

### What NOT to do

```java
// NEVER log raw credentials
log.info("User login: username={}, password={}", username, password);

// NEVER log full tokens
log.info("Authorization header: {}", request.getHeader("Authorization"));

// NEVER log PII without masking
log.info("Processing Aadhaar: {}", citizen.getAadhaarNumber());
log.info("Sending OTP to mobile: {}", citizen.getMobile());
log.info("Payment with card: {}", payment.getCardNumber());

// NEVER log request bodies that may contain secrets
log.info("Request payload: {}", requestBody);
```

### Consequences
- GDPR/DPDP Act violations (fines up to ₹250 crore in India)
- Credential leakage in log aggregation platforms
- Secrets visible to anyone with log read access (developers, support, third-party vendors)

### Fix

```java
// Use MDC for correlation, not raw data
MDC.put("userId", citizen.getId());
log.info("User authenticated successfully");

// Mask before logging
log.info("OTP sent to mobile: {}", MaskUtils.maskMobile(citizen.getMobile()));
// Output: OTP sent to mobile: +91-XXXXX-7890

// Log intent, not data
log.info("Payment processed for amount={} via provider={}",
    payment.getAmount(), payment.getProvider());
```

---

## 2. Logging in Hot Paths

### What NOT to do

```java
// Inside a loop processing 100K records
for (Record r : records) {
    log.debug("Processing record: {}", r);  // 100K log lines per request
}

// Inside tight computation loops
while (iterator.hasNext()) {
    log.trace("Current state: {}", state);
}
```

### Consequences
- Log I/O becomes the bottleneck (10x slower request)
- Disk fills up, causing service crash
- Log aggregation pipeline overwhelmed, dropping actual errors

### Fix

```java
// Log at batch boundaries
log.info("Processing batch of {} records", records.size());
int processed = 0;
for (Record r : records) {
    process(r);
    processed++;
}
log.info("Completed processing {} records", processed);

// Or sample: log every Nth iteration
for (int i = 0; i < records.size(); i++) {
    process(records.get(i));
    if (i > 0 && i % 1000 == 0) {
        log.debug("Progress: {}/{}", i, records.size());
    }
}
```

---

## 3. String Concatenation in Log Calls

### What NOT to do

```java
// String concatenation happens BEFORE the log level check
// Even if DEBUG is disabled, the string is built
log.debug("User " + user.getName() + " with email " + user.getEmail()
    + " and address " + user.getAddress() + " performed action " + action);
```

### Consequences
- Wasted CPU on string building for disabled log levels
- `toString()` on large objects called unnecessarily
- Can trigger lazy-loading in Hibernate (N+1 queries)

### Fix

```java
// SLF4J parameterized logging defers string building until level check passes
log.debug("User {} performed action {}", user.getName(), action);

// For expensive computations, guard explicitly
if (log.isDebugEnabled()) {
    log.debug("Full object state: {}", expensiveToJson(user));
}
```

---

## 4. Logging Exceptions Incorrectly

### What NOT to do

```java
// Loses stack trace
log.error("Failed to process payment: " + e.getMessage());

// Double-logs (exception as param AND as throwable)
log.error("Payment failed: {}", e.getMessage(), e);

// Swallows exception entirely
try {
    processPayment(payment);
} catch (Exception e) {
    log.error("Something went wrong");  // No context, no stack trace
}

// Logs and re-throws without wrapping
catch (Exception e) {
    log.error("Error", e);
    throw e;  // Caller will also log it — duplicate log entries
}
```

### Fix

```java
// Always pass exception as LAST argument (SLF4J convention)
log.error("Failed to process payment for userId={}: {}",
    userId, e.getMessage(), e);

// Wrap with context when re-throwing
catch (PaymentException e) {
    log.error("Payment failed for correlationId={}", correlationId, e);
    throw new BusinessException("PAYMENT_FAILED", e);
}

// At boundary layers only (controller, message listener)
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleAll(Exception e) {
    log.error("Unhandled exception in {}", request.getRequestURI(), e);
    return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_ERROR"));
}
```

---

## 5. Missing Context in Logs

### What NOT to do

```java
log.error("Payment failed");
log.info("Record updated");
log.warn("Timeout occurred");
```

### Consequences
- Impossible to triage without reproducing
- On-call engineers cannot determine scope or impact
- Correlation across services is impossible

### Fix

```java
// Always include: what, where, who, which
log.error("Payment failed for paymentId={} userId={} amount={} provider={}",
    payment.getId(), payment.getUserId(), payment.getAmount(), payment.getProvider());

// Use structured business context
log.info("Connection applied",
    kv("entity_type", "connection"),
    kv("entity_id", connectionId),
    kv("user_id", userId),
    kv("result", "success"));
```

---

## 6. Using Wrong Log Levels

### What NOT to do

```java
// INFO used for debugging
log.info("Entering method processPayment");
log.info("Variable x = {}", x);

// ERROR used for expected conditions
log.error("User not found");  // This is a 404, not a server error
log.error("Invalid input");   // This is a 400, not a server error

// DEBUG used in production for critical events
log.debug("Payment of ₹{} processed", amount);  // Lost in prod if DEBUG disabled
```

### Level Guide

| Level | When to Use | Example |
|---|---|---|
| ERROR | Unrecoverable failure, needs immediate action | DB connection lost, payment gateway down |
| WARN | Recoverable issue, degraded state | Retry attempt 2/3, cache miss, slow query >500ms |
| INFO | Significant business events | Payment completed, connection applied, user registered |
| DEBUG | Developer diagnostics | Query plan, intermediate computation state |
| TRACE | Extremely detailed flow | Every method entry/exit, byte-level protocol data |

---

## 7. Logging Without MDC Propagation in Async Contexts

### What NOT to do

```java
// MDC is ThreadLocal — lost in async threads
CompletableFuture.supplyAsync(() -> {
    log.info("Processing in background");  // No traceId, correlationId, etc.
});

// Same problem with @Async
@Async
public void sendNotification(Notification n) {
    log.info("Sending notification");  // MDC is empty
}
```

### Fix

```java
// Capture MDC before async, restore inside
Map<String, String> mdcContext = MDC.getCopyOfContextMap();
CompletableFuture.supplyAsync(() -> {
    MDC.setContextMap(mdcContext);
    try {
        log.info("Processing in background with full context");
    } finally {
        MDC.clear();
    }
});

// Or use a decorated executor
ExecutorService executor = new MdcAwareExecutorService(Executors.newCachedThreadPool());
```

---

## 8. Unbounded Stack Traces in Logs

### What NOT to do

```java
// Spring's default can log 100+ frames for nested exceptions
// Each frame = ~100 bytes. Deep chains = 50KB+ per log line.
log.error("Request failed", exception);
```

### Consequences
- Single error event = 50KB log line
- 100 errors/second = 5 MB/s = 432 GB/day
- Log aggregation ingestion throttled

### Fix

```xml
<!-- In logback-spring.xml — already configured -->
<throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
    <maxDepthPerThrowable>30</maxDepthPerThrowable>
    <maxLength>2048</maxLength>
    <rootCauseFirst>true</rootCauseFirst>
</throwableConverter>
```

---

## 9. Logging SQL Queries with Parameters

### What NOT to do

```java
// Logs actual parameter values including PII
log.debug("Executing: SELECT * FROM citizens WHERE aadhaar = '1234-5678-9012'");

// Hibernate show_sql in production
spring.jpa.show-sql=true  // Logs every query with bound parameters
```

### Fix

```yaml
# Use parameterized query logging only
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=DEBUG        # Query structure only
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE  # params (dev only)
```

---

## 10. Log Injection Attacks

### What NOT to do

```java
// User-controlled input directly in log message
log.info("User search query: {}", userInput);
// If userInput = "foo\n2026-05-21 ERROR FAKE: System compromised"
// Creates a fake log line in plain-text logs
```

### Consequences
- Attacker injects fake log entries
- Audit trail becomes unreliable
- SIEM rules can be triggered by fake entries

### Fix

```java
// Sanitize user input before logging
log.info("User search query: {}", Sanitizer.sanitize(userInput));

// JSON log format is immune to newline injection
// (already configured via LogstashEncoder)

// Validate and truncate
String safeInput = userInput != null
    ? userInput.replaceAll("[\\r\\n]", "_").substring(0, Math.min(256, userInput.length()))
    : "null";
log.info("User search query: {}", safeInput);
```

---

## Quick Reference: Logging Checklist

- [ ] No passwords, tokens, API keys, or secrets in any log
- [ ] PII (Aadhaar, mobile, email, PAN) masked before logging
- [ ] MDC propagation in all async/thread pool contexts
- [ ] Exceptions logged as last argument with full stack trace (bounded)
- [ ] Log level matches severity (not INFO for debug, not ERROR for 400)
- [ ] Every ERROR/WARN includes correlation_id and context
- [ ] No string concatenation in log calls (use `{}` placeholders)
- [ ] Hot paths log at batch boundaries, not per-item
- [ ] User input sanitized before logging (prevent log injection)
- [ ] `spring.jpa.show-sql=false` in production
- [ ] Async appenders with `discardingThreshold=0`
- [ ] Log rotation with size + time limits and total cap
- [ ] Separate audit log stream with extended retention

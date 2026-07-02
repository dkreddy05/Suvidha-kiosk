
## ADDED Requirements

### Requirement: Billing Module Migration
The billing service SHALL migrate existing `hasRole('ADMIN')` and `hasRole('PROVIDER')` checks to use the new permission-based system.

#### Scenario: replace ADMIN role checks with billing:admin permission
- **WHEN** a controller method uses `hasRole('ADMIN')` for billing operations
- **THEN** it SHALL be replaced with `@RequiresPermission("billing:admin")`

#### Scenario: replace PROVIDER role checks with billing:write permission
- **WHEN** a controller method uses `hasRole('PROVIDER')` for billing write operations
- **THEN** it SHALL be replaced with `@RequiresPermission("billing:write")`

#### Scenario: replace isAuthenticated() with specific permissions
- **WHEN** a controller method uses `isAuthenticated()` for billing read operations
- **THEN** it SHALL be replaced with `@RequiresPermission("billing:read")`

### Requirement: Admin Module Migration
The admin service SHALL migrate its `hasRole('ADMIN')` checks to use permissions.

#### Scenario: replace ADMIN role check with admin:metrics-read permission
- **WHEN** the metrics endpoint uses `hasRole('ADMIN')`
- **THEN** it SHALL be replaced with `@RequiresPermission("admin:metrics-read")`

### Requirement: Grievance Module Migration
The grievance service SHALL adopt permission-based checks for its endpoints.

#### Scenario: replace ADMIN role check with grievance:admin permission
- **WHEN** a grievance endpoint currently uses `hasRole('ADMIN')`
- **THEN** it SHALL be replaced with the appropriate `@RequiresPermission("grievance:admin")`

#### Scenario: authenticated users mapped to grievance:read
- **WHEN** a grievance endpoint requires any authenticated user
- **THEN** it SHALL be replaced with `@RequiresPermission("grievance:read")`

### Requirement: Connections Module Migration
The connections service SHALL adopt permission-based checks.

#### Scenario: replace ADMIN role check with connection:admin permission
- **WHEN** a connections endpoint uses `hasRole('ADMIN')`
- **THEN** it SHALL be replaced with `@RequiresPermission("connection:admin")`

#### Scenario: authenticated users mapped to connection:read
- **WHEN** a connections endpoint requires any authenticated user
- **THEN** it SHALL be replaced with `@RequiresPermission("connection:read")`

### Requirement: No Change to Gateway SecurityConfig
The gateway's `hasRole('ADMIN')` for `/actuator/**` SHALL remain as-is during this migration — gateway-level enforcement can be upgraded separately.

#### Scenario: gateway unchanged for now
- **WHEN** the migration is complete
- **THEN** the gateway security config SHALL still use `hasRole('ADMIN')` for actuator endpoints

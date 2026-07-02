
## ADDED Requirements

### Requirement: JWT Permissions Claim
The auth service JWT generator SHALL include a `permissions` claim containing a list of permission strings assigned to the user's role.

#### Scenario: Token includes permissions
- **WHEN** a JWT access token is generated after OTP verification or refresh
- **THEN** the token payload SHALL contain a `permissions` claim with an array of permission strings (e.g., `["billing:read", "user:read", "connection:read"]`)

#### Scenario: Empty permissions for unassigned role
- **WHEN** a role has no permissions assigned in the database
- **THEN** the `permissions` claim SHALL be an empty array `[]`

### Requirement: Shared Permission Annotation
The `suvidha-common` module SHALL provide a `@RequiresPermission` annotation for method-level security in any service.

#### Scenario: Annotation accepts single permission
- **WHEN** `@RequiresPermission("billing:read")` is placed on a method
- **THEN** only authenticated users with `billing:read` in their JWT permissions SHALL be allowed to invoke the method

#### Scenario: Annotation accepts multiple permissions (AND logic)
- **WHEN** `@RequiresPermission({"billing:write", "billing:admin"})` is placed on a method
- **THEN** the user MUST have ALL specified permissions to invoke the method

#### Scenario: Unauthenticated user rejected
- **WHEN** an unauthenticated request hits a method with `@RequiresPermission`
- **THEN** the system SHALL return 401 Unauthorized

#### Scenario: Unauthorized user rejected
- **WHEN** an authenticated user lacks the required permission
- **THEN** the system SHALL return 403 Forbidden

### Requirement: Gateway Permissions Header
The gateway's `JwtClaimsToHeadersFilter` SHALL forward permissions to downstream services in an `X-User-Permissions` header.

#### Scenario: Permissions header injected
- **WHEN** a request passes through the gateway with a valid JWT containing permissions
- **THEN** the gateway SHALL set the `X-User-Permissions` header with a comma-separated list of permission strings

#### Scenario: Spoofed header stripped
- **WHEN** an incoming request already contains an `X-User-Permissions` header
- **THEN** the gateway SHALL strip it before injecting the JWT-derived value

### Requirement: Permission-Extracting Authentication Filter
Each downstream service's `JwtAuthenticationFilter` (or a shared base filter from `suvidha-common`) SHALL extract permissions from the JWT or headers and set them in the security context.

#### Scenario: Permissions in GrantedAuthority
- **WHEN** a downstream service validates a request
- **THEN** each permission string SHALL be set as a `SimpleGrantedAuthority("PERMISSION_billing:read")` so Spring Security's `hasAuthority()` can be used as an alternative check


## Context

The Suvidha platform spans 7 microservices (auth, billing, grievance, connections, admin, notification, gateway) with 3 hardcoded roles (ADMIN, EMPLOYEE, USER) enforced via scattered `hasRole()` checks. The billing module also references a `PROVIDER` role that doesn't exist in the enum. There is no way to grant granular access — e.g., an employee who should only read billing data but not modify it must be given full `EMPLOYEE` access with no restriction.

## Goals / Non-Goals

**Goals:**
- Permission catalog with string-based permissions (e.g., `billing:read`, `billing:write`)
- Dynamic role-to-permission mapping stored in the database
- JWT tokens carry both role and permissions
- Permission-checking annotation in `suvidha-common` usable across all services
- Admin CRUD APIs for managing permissions and role assignments
- Gateway forwards permissions to downstream services via headers
- Seed permissions for all existing modules with default role assignments
- Add the missing `PROVIDER` role

**Non-Goals:**
- Hierarchical roles (seniority-based) — permissions are flat per role
- Attribute-based access control (ABAC) with user attributes
- Runtime permission evaluation without token refresh (must re-login or refresh token for new permissions)
- UI for role/permission management (API only)
- Per-instance overrides (e.g., user-specific permissions beyond their role)

## Decisions

1. **Flat permission strings (module:action) vs hierarchical tree** — Flat strings like `billing:read` are simpler, easier to store/transmit in JWT, and easier to check with Spring Security. Hierarchical trees add complexity with little benefit for this scale.

2. **Permissions in JWT vs service calls to auth** — Embedding permissions in the JWT `permissions` claim avoids a synchronous call to auth on every request, reduces latency, and keeps services stateless. This means permissions take effect only after token refresh (acceptable trade-off).

3. **`@RequiresPermission` annotation vs AOP pointcut** — Custom annotation in `suvidha-common` provides explicit, readable, compile-time-checked security. A regex-based AOP pointcut would be fragile and implicit.

4. **Gateway header passthrough** — Adding `X-User-Permissions` to the existing set of headers (`X-User-Id`, `X-User-Role`, etc.) from `JwtClaimsToHeadersFilter` is the simplest approach and consistent with the existing architecture.

5. **Single Role vs Multiple Roles per user** — Current design assumes one role per user. Adding multi-role support would break the existing `role` column in `citizens_table` and `Role` enum. We keep single-role for now; fine-grained permissions compensate for the lack of multiple roles.

6. **Database-stored permission assignments vs code-defined** — Storing in DB (with seed migration) allows admin changes without redeployment. A code-defined fallback provides a safe default.

## Risks / Trade-offs

- **[Risk] Permissions in JWT become stale** — Mitigation: Keep access token TTL at 30 min, refresh token rotation ensures fresh permissions within 7 days. Admin permission changes only apply after the user's next token refresh.
- **[Risk] Large permission sets inflate JWT size** — Mitigation: All permissions are short strings (e.g., `billing:read`). Even with 50+ permissions, the JWT stays well under 8KB. If needed, implement permission subsetting per service.
- **[Risk] Downstream services may not honor new annotation** — Mitigation: The `@RequiresPermission` annotation will be in shared common with a clear SPI. Each service team adds the annotation as they adopt the system. Old `hasRole()` checks remain as fallback during migration.
- **[Risk] Gateway header injection (X-User-Permissions spoofing)** — Mitigation: The existing `JwtClaimsToHeadersFilter` already strips incoming headers before injecting JWT-derived values. Permissions will follow the same pattern.

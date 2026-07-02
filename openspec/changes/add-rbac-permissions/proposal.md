
## Why

The platform currently has only 3 hardcoded roles (ADMIN, EMPLOYEE, USER) with no fine-grained permission system. Role-based access control is scattered across individual service security configs and `@PreAuthorize` annotations, making it impossible to grant or restrict specific capabilities without code changes. A `PROVIDER` role is referenced in billing code but doesn't exist in the role enum. As the platform scales to more utility services, the lack of a unified permission system creates security gaps, duplication, and maintenance overhead.

## What Changes

- Introduce a **permission-based RBAC system** with fine-grained permissions (e.g., `billing:read`, `billing:write`, `user:manage`)
- Create a **role-permission mapping** that can be configured dynamically (not hardcoded)
- Enhance **JWT tokens** to include granted permissions in claims
- Add **permission-checking utilities** and annotation support across all services
- Build **admin APIs** for managing roles and permissions
- Add **database schema** for permissions, role-permission mappings
- Migrate existing `@PreAuthorize("hasRole(...)")` checks to use permission-based checks where appropriate
- Add the missing `PROVIDER` role and ensure all existing role references are consistent
- Add **default permission sets** for each role (ADMIN, EMPLOYEE, USER, PROVIDER)

## Capabilities

### New Capabilities
- `role-permission-definition`: Define the permission catalog and role-permission assignment system, including database schema, seed data, and admin CRUD APIs
- `permission-enforcement`: Permission checking infrastructure — JWT claim enhancement, annotation support, method security, and gateway-level enforcement
- `rbac-migration`: Migrate existing role-based security annotations across all services (billing, grievance, connections, admin, auth) to use the new permission system

### Modified Capabilities
- *(None — no existing specs to modify)*

## Impact

- **suvidha-auth**: Role enum expanded (add PROVIDER), new RBAC service, permission + role-permission entities, new Flyway migration, JWT token now includes `permissions` claim
- **suvidha-gateway**: May need to pass permissions headers downstream like it does for other JWT claims
- **suvidha-billing**: Replace `hasRole('ADMIN')` / `hasRole('PROVIDER')` with permission checks like `hasPermission('billing:admin')` or `hasPermission('billing:write')`
- **suvidha-grievance, suvidha-connections, suvidha-admin**: Similar annotation migrations
- **suvidha-common**: Shared `@RequiresPermission` annotation and permission evaluation logic
- **Database**: New tables: `permissions`, `role_permissions`; seed data migration

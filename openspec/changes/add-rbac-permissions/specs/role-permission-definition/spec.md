
## ADDED Requirements

### Requirement: Permission Catalog
The system SHALL maintain a permission catalog with string-based identifiers in `module:action` format.

#### Scenario: Permission structure
- **WHEN** a permission is defined
- **THEN** it MUST have a unique `name` (e.g., `billing:read`), a `description`, a `module` grouping, and an `action` (read/write/admin/manage)

### Requirement: Role-Permission Mapping Table
The system SHALL store role-to-permission assignments in a database join table `role_permissions`.

#### Scenario: Role has multiple permissions
- **WHEN** a role is assigned to permissions
- **THEN** the `role_permissions` table SHALL contain rows with `role` (enum value) and `permission_id` (FK to permissions table)

#### Scenario: Permission assigned to multiple roles
- **WHEN** a permission is used by multiple roles
- **THEN** the system SHALL support multiple `role_permissions` rows referencing the same `permission_id`

### Requirement: Admin CRUD for Permissions
The auth service SHALL expose admin-only REST APIs for managing permissions and role-permission mappings.

#### Scenario: Create permission
- **WHEN** an ADMIN calls `POST /api/admin/permissions` with `{name, description, module, action}`
- **THEN** a new permission SHALL be created in the `permissions` table

#### Scenario: List permissions
- **WHEN** an ADMIN calls `GET /api/admin/permissions`
- **THEN** the system SHALL return all permissions with pagination and optional `module` filter

#### Scenario: Update permission
- **WHEN** an ADMIN calls `PUT /api/admin/permissions/{id}`
- **THEN** the permission SHALL be updated

#### Scenario: Delete permission
- **WHEN** an ADMIN calls `DELETE /api/admin/permissions/{id}`
- **THEN** the permission SHALL be deleted (soft or hard) and all associated role mappings removed

#### Scenario: Assign permissions to role
- **WHEN** an ADMIN calls `PUT /api/admin/roles/{role}/permissions` with a list of permission IDs
- **THEN** the role SHALL be assigned exactly those permissions (replace semantics)

#### Scenario: View role permissions
- **WHEN** an ADMIN calls `GET /api/admin/roles/{role}/permissions`
- **THEN** the system SHALL return all permissions assigned to that role

### Requirement: Seed Permission Data
The system SHALL ship with a Flyway migration that seeds standard permissions and default role assignments.

#### Scenario: Billing module permissions
- **WHEN** the migration runs
- **THEN** permissions for `billing:read`, `billing:write`, `billing:admin`, `billing:deactivate` SHALL be created

#### Scenario: User module permissions
- **WHEN** the migration runs
- **THEN** permissions for `user:read`, `user:manage`, `user:delete` SHALL be created

#### Scenario: Grievance module permissions
- **WHEN** the migration runs
- **THEN** permissions for `grievance:read`, `grievance:write`, `grievance:assign`, `grievance:admin` SHALL be created

#### Scenario: Connections module permissions
- **WHEN** the migration runs
- **THEN** permissions for `connection:read`, `connection:write`, `connection:approve`, `connection:admin` SHALL be created

#### Scenario: Admin module permissions
- **WHEN** the migration runs
- **THEN** permissions for `admin:metrics-read`, `admin:permissions-manage`, `admin:audit-read` SHALL be created

#### Scenario: Notification module permissions
- **WHEN** the migration runs
- **THEN** permissions for `notification:send`, `notification:read`, `notification:admin` SHALL be created

#### Scenario: Default ADMIN role permissions
- **WHEN** the migration runs
- **THEN** the ADMIN role SHALL be assigned ALL permissions across all modules

#### Scenario: Default EMPLOYEE role permissions
- **WHEN** the migration runs
- **THEN** the EMPLOYEE role SHALL be assigned read permissions for billing, grievance, connections, and user modules

#### Scenario: Default USER role permissions
- **WHEN** the migration runs
- **THEN** the USER role SHALL be assigned `user:read` and `connection:read` permissions only

#### Scenario: Default PROVIDER role permissions
- **WHEN** the migration runs
- **THEN** the PROVIDER role SHALL be assigned `billing:write`, `connection:read`, `connection:write`, `grievance:read` permissions

### Requirement: Role Enum Expanded
The `Role` enum SHALL include `PROVIDER` in addition to existing ADMIN, EMPLOYEE, USER values.

#### Scenario: Provider role exists
- **WHEN** the system starts
- **THEN** the Role enum SHALL have four values: ADMIN, EMPLOYEE, USER, PROVIDER

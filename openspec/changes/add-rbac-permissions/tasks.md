
## 1. Database Schema & Seed Data

- [ ] 1.1 Add the PROVIDER value to the Role enum
- [ ] 1.2 Create Permission entity with fields: id, name, description, module, action
- [ ] 1.3 Create RolePermission join entity or use @ManyToMany on Permission
- [ ] 1.4 Create Flyway migration V7__add_rbac_tables.sql for permissions and role_permissions tables
- [ ] 1.5 Create Flyway migration V8__seed_default_permissions.sql with all module permissions
- [ ] 1.6 Create Flyway migration V9__seed_role_permissions.sql with default role-to-permission assignments

## 2. Permission Management Service & APIs

- [ ] 2.1 Create PermissionRepository with findByName, findByModule, findAll
- [ ] 2.2 Create RolePermissionRepository with findByRole, deleteByRole
- [ ] 2.3 Create PermissionService with CRUD operations for permissions
- [ ] 2.4 Create RbacAdminController with endpoints:
      - GET/POST /api/admin/permissions
      - GET/PUT/DELETE /api/admin/permissions/{id}
      - GET /api/admin/roles/{role}/permissions
      - PUT /api/admin/roles/{role}/permissions
- [ ] 2.5 Secure admin endpoints with ADMIN role check
- [ ] 2.6 Add DTOs: PermissionRequest, PermissionResponse, RolePermissionsRequest

## 3. JWT Permission Claim Enhancement

- [ ] 3.1 Load permissions for the user's role in JwtToken.generateToken()
- [ ] 3.2 Add "permissions" claim with list of permission strings to JWT payload
- [ ] 3.3 Update JwtAuth filter to extract permissions claim and add as SimpleGrantedAuthority("PERMISSION_<name>")

## 4. Shared Permission Annotation

- [ ] 4.1 Create @RequiresPermission annotation in suvidha-common with value() (String array) and logic() (AND/OR)
- [ ] 4.2 Create PermissionAspect or PermissionEvaluator in suvidha-common that checks permissions from SecurityContext
- [ ] 4.3 Add spring-boot-starter-aop dependency to suvidha-common if not present
- [ ] 4.4 Register @RequiresPermission with Spring Security's method security in each service

## 5. Gateway Permission Header Forwarding

- [ ] 5.1 Update JwtClaimsToHeadersFilter to extract permissions from JWT claims
- [ ] 5.2 Add X-User-Permissions header (comma-separated) to forwarded headers
- [ ] 5.3 Ensure spoofed X-User-Permissions headers are stripped

## 6. Permission Extraction in Downstream Services

- [ ] 6.1 Update suvidha-common's JwtAuthenticationFilter base (or each service's filter) to extract permissions from JWT/X-User-Permissions
- [ ] 6.2 Add "PERMISSION_" prefixed SimpleGrantedAuthority for each permission in:
      - suvidha-billing JwtAuthenticationFilter
      - suvidha-grievance JwtAuthenticationFilter
      - suvidha-connections JwtAuthenticationFilter
      - suvidha-admin JwtAuthenticationFilter

## 7. Migrate Billing Module Annotations

- [ ] 7.1 Add @RequiresPermission("billing:read") to BillingController endpoints with @PreAuthorize("isAuthenticated()")
- [ ] 7.2 Replace @PreAuthorize("hasRole('ADMIN')") with @RequiresPermission("billing:admin") in ServiceAccountController
- [ ] 7.3 Replace @PreAuthorize("hasRole('ADMIN')") with @RequiresPermission("billing:admin") in NewConnectionController
- [ ] 7.4 Replace @PreAuthorize("hasRole('PROVIDER')") with @RequiresPermission("billing:write") in NewConnectionController
- [ ] 7.5 Add @RequiresPermission("billing:read") to CoreNewConnectionController
- [ ] 7.6 Add @RequiresPermission("billing:read") to BillingSpecController and AccountLinkingController

## 8. Migrate Other Modules

- [ ] 8.1 Replace hasRole('ADMIN') with @RequiresPermission("admin:metrics-read") in admin SecurityConfig
- [ ] 8.2 Add @RequiresPermission("grievance:read") to authenticated grievance endpoints
- [ ] 8.3 Add @RequiresPermission("connection:read") to authenticated connections endpoints

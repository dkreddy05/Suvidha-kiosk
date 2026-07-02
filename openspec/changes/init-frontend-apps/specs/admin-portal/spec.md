## ADDED Requirements

### Requirement: Admin Dashboard
The `suvidha-admin` app SHALL provide a dashboard displaying key metrics like total users, open grievances, and payment success rates.

#### Scenario: Admin views dashboard
- **WHEN** the admin logs into the portal
- **THEN** they see an overview of key performance indicators

### Requirement: Grievance Management
The admin portal MUST allow employees and admins to view all grievances, filter them by status, and update their resolution state.

#### Scenario: Employee resolves a grievance
- **WHEN** an employee updates a grievance status to RESOLVED
- **THEN** the system records the update and the grievance is marked resolved

### Requirement: Connection Approvals
The portal SHALL provide an interface for administrators to review, approve, or reject new utility connection requests.

#### Scenario: Admin approves connection
- **WHEN** an admin reviews a pending connection and clicks Approve
- **THEN** the connection status transitions to APPROVED

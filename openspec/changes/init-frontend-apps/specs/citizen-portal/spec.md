## ADDED Requirements

### Requirement: OTP Login Flow
The `suvidha-citizen` app SHALL provide an OTP-based login flow utilizing the backend `/api/auth/send-otp` and `/api/auth/verify-otp` endpoints.

#### Scenario: Citizen logs in
- **WHEN** the citizen enters a valid OTP
- **THEN** the system logs them in and routes them to the dashboard

### Requirement: Utility Billing
The citizen portal SHALL allow users to view linked utility accounts, check pending bills, and initiate payments.

#### Scenario: Citizen views bills
- **WHEN** navigating to the billing section
- **THEN** a list of all linked utility accounts and current bills is displayed

### Requirement: Grievance Submission
The portal MUST enable citizens to submit grievances and track their status.

#### Scenario: Submitting a grievance
- **WHEN** a citizen fills out the grievance form and submits
- **THEN** the system returns a reference number and displays it on the tracking page

### Requirement: DPDP Privacy Center
The portal SHALL provide a privacy center for citizens to view consents, download personal data, and request account deletion.

#### Scenario: Exporting data
- **WHEN** the citizen requests a data export
- **THEN** a JSON file containing all their personal data is downloaded

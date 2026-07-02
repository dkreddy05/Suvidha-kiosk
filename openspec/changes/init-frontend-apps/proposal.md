## Why

The Suvidha backend architecture (Java Spring Boot microservices) is well-structured, but currently lacks a modern, unified, and premium frontend tailored for citizens and administrators. We need to initialize the foundational frontend workspaces to enable interactions with the existing robust backend API, supporting features like utility billing, new connections, grievances, and DPDP-compliant citizen data privacy.

## What Changes

- Scaffolding the frontend workspace with two distinct Next.js applications: `suvidha-citizen` and `suvidha-admin`.
- Setting up a shared UI component package/directory for the design system (Tailwind CSS, shadcn/ui).
- Configuring Next.js App Router with i18n support (en, hi, te, ta) and middleware for auth guards.
- Initializing API client configurations (Axios interceptors) and state management hooks (TanStack Query, Zustand).

## Capabilities

### New Capabilities
- `frontend-workspace-setup`: Initialize Next.js projects for Citizen and Admin portals, alongside a shared `@suvidha/ui` package.
- `citizen-portal`: The core public-facing application for citizens to login via OTP, pay bills, raise grievances, and manage profiles.
- `admin-portal`: The dashboard for administrators and employees to monitor KPIs, connections, and grievances.

### Modified Capabilities
- 

## Impact

- Adds frontend Next.js applications to the project workspace.
- Introduces new tooling dependencies (TypeScript, Tailwind, React Query, Zustand).
- Establishes the standard frontend architecture and design patterns (AppShell, AuthFlow, API integration) for all future UI development.

## Context

The Suvidha platform currently has a well-structured Java Spring Boot microservice backend but requires a comprehensive frontend ecosystem. The ecosystem needs to cater to two primary roles: citizens (interacting with public services like billing, connections, grievances, and profile data) and administrators/employees (managing these services and analyzing metrics).

## Goals / Non-Goals

**Goals:**
- Create two separate Next.js 15 App Router applications (`suvidha-citizen` and `suvidha-admin`).
- Establish a shared UI component package `@suvidha/ui` using Tailwind CSS and shadcn/ui.
- Implement robust state management: TanStack Query for server state and Zustand for client state (auth/UI preferences).
- Ensure high standards of design (premium government UX, multilingual support, animations via Framer Motion, DPDP compliance).

**Non-Goals:**
- Modifying backend services in this phase (though some backend API improvements have been noted).
- Full implementation of all pages and features; this change focuses on initializing the workspace, scaffolding the apps, and setting up the architecture.

## Decisions

- **Two Separate Applications**: Deploying `suvidha-citizen` and `suvidha-admin` as separate apps within a monorepo setup. Rationale: Separation of concerns, varied security postures, and different bundle sizes.
- **Monorepo Structure (Turbo)**: Using Turborepo for orchestrating builds across the Next.js apps and shared `@suvidha/ui` package. Rationale: Ensures consistency in the design system and simplifies dependency management.
- **Next.js App Router**: Leveraging Next.js 15's App Router. Rationale: Access to React Server Components (RSC), seamless nested layouts, and edge middleware for auth guards.
- **State Management Split**: Using TanStack Query for fetching/caching API data and Zustand for ephemeral/local state (tokens, theme). Rationale: Reduces boilerplate and provides superior cache invalidation for features like billing and grievances.
- **Axios Interceptors**: Handling JWT access and refresh tokens explicitly via Axios. Rationale: Centralizes auth token management and transparently handles 401 retries.

## Risks / Trade-offs

- **[Risk] Monorepo Complexity** → Mitigation: Use a well-established tool like Turborepo and strictly define package boundaries (`apps/` vs `packages/`).
- **[Risk] i18n in App Router** → Mitigation: Use `next-intl` which has first-class support for App Router and RSC to ensure localization is smooth from day one.

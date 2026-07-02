## ADDED Requirements

### Requirement: Monorepo scaffolding
The workspace SHALL provide a Turborepo-based monorepo containing `suvidha-citizen`, `suvidha-admin`, and a shared `@suvidha/ui` package.

#### Scenario: Successful workspace setup
- **WHEN** a developer initializes the repository
- **THEN** the Next.js apps and shared UI package are linked correctly

### Requirement: Tooling configurations
The workspace MUST include configurations for TypeScript, ESLint, Prettier, and Husky across all frontend packages.

#### Scenario: Code linting
- **WHEN** a developer runs the lint command
- **THEN** it validates all Next.js apps and the UI package using shared ESLint rules

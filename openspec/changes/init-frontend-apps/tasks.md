## 1. Monorepo Setup

- [ ] 1.1 Initialize a Turborepo workspace at the project root
- [ ] 1.2 Create `apps/` and `packages/` directories
- [ ] 1.3 Scaffold `suvidha-citizen` Next.js 15 App Router app in `apps/`
- [ ] 1.4 Scaffold `suvidha-admin` Next.js 15 App Router app in `apps/`
- [ ] 1.5 Scaffold `@suvidha/ui` package in `packages/`

## 2. Shared Packages Configuration

- [ ] 2.1 Setup Tailwind CSS and shadcn/ui in `@suvidha/ui`
- [ ] 2.2 Configure shared ESLint, Prettier, and TypeScript configs
- [ ] 2.3 Setup Husky for pre-commit hooks

## 3. Citizen Portal Initialization

- [ ] 3.1 Install and configure `next-intl` for i18n support in `suvidha-citizen`
- [ ] 3.2 Setup Zustand store (`auth.store.ts` and `ui.store.ts`)
- [ ] 3.3 Setup TanStack Query global client
- [ ] 3.4 Create base layout with AppShell, Sidebar, and Header
- [ ] 3.5 Configure Next.js edge middleware for auth routing

## 4. Admin Portal Initialization

- [ ] 4.1 Install and configure TanStack Query and Zustand in `suvidha-admin`
- [ ] 4.2 Create base admin layout with Sidebar and Header
- [ ] 4.3 Configure Next.js edge middleware for admin role checking

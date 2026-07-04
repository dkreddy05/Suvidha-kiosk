FROM node:20-alpine AS base

FROM base AS deps
WORKDIR /app
COPY package.json package-lock.json ./
COPY apps/citizen/package.json ./apps/citizen/package.json
COPY packages/ui/ ./packages/ui/
RUN npm ci

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/package.json ./package.json
COPY --from=deps /app/package-lock.json ./package-lock.json
COPY --from=deps /app/packages/ui ./packages/ui
COPY apps/citizen/ ./apps/citizen/
RUN npm run build -w @suvidha/citizen

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/apps/citizen/.next ./.next
COPY --from=builder /app/apps/citizen/next.config.ts ./next.config.ts
COPY --from=builder /app/apps/citizen/package.json ./package.json
COPY --from=builder /app/node_modules ./node_modules
EXPOSE 3000
CMD ["npx", "next", "start", "-p", "3000"]

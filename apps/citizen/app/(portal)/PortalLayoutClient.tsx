'use client';

import { AppShell } from '@/components/layout/AppShell';

export function PortalLayoutClient({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AppShell>{children}</AppShell>;
}

import { redirect } from 'next/navigation';
import { cookies } from 'next/headers';
import { PortalLayoutClient } from './PortalLayoutClient';

export default async function PortalLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const cookieStore = await cookies();
  const authCookie = cookieStore.get('session');

  if (!authCookie) {
    redirect('/login');
  }

  return <PortalLayoutClient>{children}</PortalLayoutClient>;
}

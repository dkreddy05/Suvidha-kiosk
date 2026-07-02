import type { Metadata } from 'next';
import { Plus_Jakarta_Sans, DM_Sans } from 'next/font/google';
import { Providers } from './providers';
import './globals.css';

const plusJakartaSans = Plus_Jakarta_Sans({
  subsets: ['latin'],
  variable: '--font-display',
  display: 'swap',
});

const dmSans = DM_Sans({
  subsets: ['latin'],
  variable: '--font-body',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'Suvidha - Citizen Portal',
  description: 'Access your water billing, connections, and grievance services',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${plusJakartaSans.variable} ${dmSans.variable} font-body antialiased bg-white dark:bg-gray-950 text-gray-900 dark:text-gray-100`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}

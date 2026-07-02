'use client';

import { usePathname } from 'next/navigation';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard,
  Receipt,
  FileText,
  Cable,
  User,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { useUIStore } from '@/store/ui.store';

const mobileNavItems = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Billing', href: '/billing', icon: Receipt },
  { label: 'Grievances', href: '/grievances', icon: FileText },
  { label: 'Connections', href: '/connections', icon: Cable },
  { label: 'Profile', href: '/profile', icon: User },
];

interface AppShellProps {
  children: React.ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const { sidebarOpen } = useUIStore();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Sidebar />

      <div
        className={cn(
          'transition-all duration-300',
          sidebarOpen ? 'lg:ml-64' : 'lg:ml-16'
        )}
      >
        <Header />

        <main className="pb-20 lg:pb-0">
          <AnimatePresence mode="wait">
            <motion.div
              key={pathname}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2, ease: 'easeInOut' }}
            >
              {children}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      <nav className="fixed bottom-0 left-0 right-0 z-40 border-t border-border bg-white dark:bg-surface-dark lg:hidden">
        <div className="flex items-center justify-around h-16 px-2">
          {mobileNavItems.map((item) => {
            const hrefPath = item.href.split('?')[0];
            const isActive =
              hrefPath === '/dashboard'
                ? pathname === '/dashboard'
                : pathname.startsWith(hrefPath);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
                  isActive
                    ? 'text-brand-600 dark:text-brand-400'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                )}
              >
                <Icon className="h-5 w-5" />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </div>
      </nav>
    </div>
  );
}

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Receipt,
  History,
  Cable,
  PlusCircle,
  FileText,
  PlusSquare,
  Bell,
  Settings,
  Shield,
  LogOut,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/store/ui.store';
import { useAuthStore } from '@/store/auth.store';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

interface NavSection {
  title?: string;
  items: NavItem[];
}

const navSections: NavSection[] = [
  {
    items: [{ label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard }],
  },
  {
    title: 'Billing',
    items: [
      { label: 'Accounts', href: '/billing', icon: Receipt },
      { label: 'Payment History', href: '/billing?tab=history', icon: History },
    ],
  },
  {
    title: 'Connections',
    items: [
      { label: 'My Requests', href: '/connections', icon: Cable },
      { label: 'New Connection', href: '/connections/new', icon: PlusCircle },
    ],
  },
  {
    title: 'Grievances',
    items: [
      { label: 'My Grievances', href: '/grievances', icon: FileText },
      { label: 'Submit New', href: '/grievances/new', icon: PlusSquare },
    ],
  },
  {
    items: [{ label: 'Notifications', href: '/notifications', icon: Bell }],
  },
  {
    title: 'Profile',
    items: [
      { label: 'Settings', href: '/profile', icon: Settings },
      { label: 'Privacy Center', href: '/profile/privacy', icon: Shield },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { sidebarOpen, toggleSidebar } = useUIStore();
  const logout = useAuthStore((s) => s.logout);

  const isActive = (href: string) => {
    const hrefPath = href.split('?')[0];
    if (hrefPath === '/dashboard') {
      return pathname === '/dashboard';
    }
    return pathname.startsWith(hrefPath);
  };

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 z-40 flex h-screen flex-col border-r border-border bg-white dark:bg-surface-dark transition-all duration-300',
        sidebarOpen ? 'w-64' : 'w-16'
      )}
    >
      <div className="flex h-16 items-center justify-between px-4 border-b border-border">
        {sidebarOpen && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500 text-white font-bold text-sm">
              S
            </div>
            <span className="font-display text-lg font-semibold tracking-tight text-gray-900 dark:text-gray-100">
              Suvidha
            </span>
          </Link>
        )}
        <button
          onClick={toggleSidebar}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
          aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
        >
          {sidebarOpen ? <ChevronLeft className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </button>
      </div>

      <nav className="flex-1 overflow-y-auto px-2 py-4 space-y-4">
        {navSections.map((section, idx) => (
          <div key={idx}>
            {section.title && sidebarOpen && (
              <p className="px-3 mb-1 text-xs font-semibold uppercase tracking-wider text-muted dark:text-muted-dark">
                {section.title}
              </p>
            )}
            <ul className="space-y-0.5">
              {section.items.map((item) => {
                const active = isActive(item.href);
                const Icon = item.icon;
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={cn(
                        'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                        active
                          ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/20 dark:text-brand-300'
                          : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100'
                      )}
                    >
                      <Icon className="h-5 w-5 shrink-0" />
                      {sidebarOpen && <span>{item.label}</span>}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      <div className="border-t border-border p-2">
        <button
          onClick={logout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-600 dark:hover:text-red-400 transition-colors"
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {sidebarOpen && <span>Logout</span>}
        </button>
      </div>
    </aside>
  );
}

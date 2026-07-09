'use client';

import { usePathname, useRouter } from 'next/navigation';
import {
  PanelLeftClose,
  PanelLeftOpen,
  Globe,
  Bell,
  ChevronDown,
  User,
  Settings,
  LogOut,
} from 'lucide-react';
import { useUIStore } from '@/store/ui.store';
import { useAuthStore } from '@/store/auth.store';
import {
  Avatar,
  AvatarFallback,
  Button,
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from '@suvidha/ui';

const languages = [
  { code: 'en', label: 'English' },
  { code: 'hi', label: 'हिन्दी' },
  { code: 'te', label: 'తెలుగు' },
  { code: 'ta', label: 'தமிழ்' },
];

export function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { sidebarOpen, toggleSidebar, language, setLanguage } = useUIStore();
  const { citizen, logout } = useAuthStore();

  const currentLang = languages.find((l) => l.code === language) ?? languages[0];
  const initials = citizen?.name
    ? citizen.name
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : 'U';

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-white/80 dark:bg-surface-dark/80 backdrop-blur-lg px-4 lg:px-6">
      <div className="flex items-center gap-3">
        <button
          onClick={toggleSidebar}
          className="flex h-9 w-9 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
          aria-label={sidebarOpen ? 'Close sidebar' : 'Open sidebar'}
        >
          {sidebarOpen ? (
            <PanelLeftClose className="h-5 w-5" />
          ) : (
            <PanelLeftOpen className="h-5 w-5" />
          )}
        </button>
        <h1 className="text-lg font-semibold text-gray-900 dark:text-gray-100 hidden sm:block">
          {pathname === '/dashboard'
            ? 'Dashboard'
            : pathname
                .split('/')
                .filter(Boolean)
                .pop()
                ?.replace(/-/g, ' ')
                .replace(/\b\w/g, (c) => c.toUpperCase()) ?? ''}
        </h1>
      </div>

      <div className="flex items-center gap-2">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="gap-1.5 text-sm font-medium text-gray-600 dark:text-gray-400">
              <Globe className="h-4 w-4" />
              <span className="hidden sm:inline">{currentLang.label}</span>
              <ChevronDown className="h-3 w-3" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-[140px]">
            <DropdownMenuLabel>Language</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {languages.map((lang) => (
              <DropdownMenuItem
                key={lang.code}
                onClick={() => setLanguage(lang.code as 'en' | 'hi' | 'te' | 'ta')}
                className={language === lang.code ? 'bg-brand-50 dark:bg-brand-900/20 text-brand-700 dark:text-brand-300' : ''}
              >
                {lang.label}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        <Button
          variant="ghost"
          size="icon"
          className="relative"
          aria-label="Notifications"
          onClick={() => router.push('/notifications')}
        >
          <Bell className="h-5 w-5 text-gray-600 dark:text-gray-400" />
          <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white">
            3
          </span>
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 px-2" aria-label="User menu">
              <Avatar className="h-8 w-8">
                <AvatarFallback>{initials}</AvatarFallback>
              </Avatar>
              <div className="hidden lg:flex flex-col items-start text-sm">
                <span className="font-medium text-gray-900 dark:text-gray-100 leading-tight">
                  {citizen?.name ?? 'User'}
                </span>
                <span className="text-xs text-muted dark:text-muted-dark leading-tight">
                  {citizen?.mobile ? `+91 ${citizen.mobile}` : ''}
                </span>
              </div>
              <ChevronDown className="hidden lg:block h-4 w-4 text-muted dark:text-muted-dark" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-[200px]">
            <DropdownMenuLabel>
              <div className="flex flex-col">
                <span>{citizen?.name ?? 'User'}</span>
                <span className="text-xs font-normal text-muted dark:text-muted-dark">
                  {citizen?.mobile ?? ''}
                </span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => router.push('/profile')}>
              <User className="mr-2 h-4 w-4" />
              Profile
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => router.push('/profile')}>
              <Settings className="mr-2 h-4 w-4" />
              Settings
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={logout}
              className="text-red-600 dark:text-red-400 focus:text-red-600 dark:focus:text-red-400"
            >
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}

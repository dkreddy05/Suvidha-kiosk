'use client';

import { Globe, ChevronDown } from 'lucide-react';
import { useUIStore } from '@/store/ui.store';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
  Button,
} from '@suvidha/ui';

const languages = [
  { code: 'en', label: 'English' },
  { code: 'hi', label: 'हिन्दी' },
  { code: 'te', label: 'తెలుగు' },
  { code: 'ta', label: 'தமிழ்' },
];

export function AuthLayoutClient() {
  const { language, setLanguage } = useUIStore();

  const currentLang = languages.find((l) => l.code === language) ?? languages[0];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1.5">
          <Globe className="h-4 w-4" />
          <span>{currentLang.label}</span>
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
            className={
              language === lang.code
                ? 'bg-brand-50 dark:bg-brand-900/20 text-brand-700 dark:text-brand-300'
                : ''
            }
          >
            {lang.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

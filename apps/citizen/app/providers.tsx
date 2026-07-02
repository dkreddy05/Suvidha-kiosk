'use client';

import { useState, useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from 'next-themes';
import { Toaster } from 'sonner';
import { NextIntlClientProvider } from 'next-intl';
import { useAuthStore } from '@/store/auth.store';
import { useUIStore } from '@/store/ui.store';

import en from '../i18n/en.json';
import hi from '../i18n/hi.json';
import te from '../i18n/te.json';
import ta from '../i18n/ta.json';

const messagesMap = { en, hi, te, ta };

function AuthHydration({ children }: { children: React.ReactNode }) {
  const hydrate = useAuthStore((s) => s.hydrate);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    hydrate();
    setHydrated(true);
  }, [hydrate]);

  if (!hydrated) {
    return null;
  }

  return <>{children}</>;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  const language = useUIStore((s) => s.language);
  const activeMessages = messagesMap[language] || en;

  return (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale={language} messages={activeMessages} timeZone="Asia/Kolkata">
        <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
          <AuthHydration>{children}</AuthHydration>
          <Toaster
            position="top-right"
            richColors
            closeButton
            toastOptions={{
              duration: 4000,
            }}
          />
        </ThemeProvider>
      </NextIntlClientProvider>
    </QueryClientProvider>
  );
}

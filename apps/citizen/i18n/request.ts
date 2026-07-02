import { getRequestConfig } from 'next-intl/server';
import { cookies } from 'next/headers';
import { locales, defaultLocale, Locale } from './config';

export default getRequestConfig(async () => {
  const cookieStore = await cookies();
  const localeCookie = cookieStore.get('NEXT_LOCALE')?.value;
  const locale = (localeCookie && locales.includes(localeCookie as any)) ? (localeCookie as Locale) : defaultLocale;

  return {
    locale,
    messages: (await import(`./${locale}.json`)).default,
  };
});

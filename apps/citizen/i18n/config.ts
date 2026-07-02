export const locales = ['en', 'hi', 'te', 'ta'] as const;
export const defaultLocale = 'en' as const;
export type Locale = (typeof locales)[number];

import { create } from 'zustand'
import type { LanguagePref } from '../api/types'

export type LanguageStore = {
  lang: LanguagePref
  setLang: (lang: LanguagePref) => void
}

export const useLanguageStore = create<LanguageStore>((set) => ({
  lang: 'en',
  setLang: (lang) => set({ lang }),
}))

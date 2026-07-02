"use client";

import { useUIStore } from "@/store/ui.store";
import { LANGUAGES } from "@/lib/constants";
import { Globe } from "lucide-react";

export function LanguageSwitcher() {
  const { language, setLanguage } = useUIStore();

  return (
    <div className="flex items-center gap-1.5">
      <Globe className="h-4 w-4 text-muted" />
      <select
        value={language}
        onChange={(e) => setLanguage(e.target.value as typeof language)}
        className="bg-transparent text-sm text-muted hover:text-foreground focus:outline-none cursor-pointer"
        aria-label="Select language"
      >
        {LANGUAGES.map((lang) => (
          <option key={lang.code} value={lang.code}>
            {lang.label}
          </option>
        ))}
      </select>
    </div>
  );
}

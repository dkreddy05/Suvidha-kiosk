import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/**/*.{ts,tsx}",
    "../../apps/*/app/**/*.{ts,tsx}",
    "../../apps/*/components/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#EFF6FF",
          100: "#DBEAFE",
          200: "#BFDBFE",
          300: "#93C5FD",
          400: "#60A5FA",
          500: "#2563EB",
          600: "#1D4ED8",
          700: "#1E40AF",
          800: "#1E3A8A",
          900: "#1E3A5F",
        },
        success: { DEFAULT: "#16A34A", light: "#DCFCE7" },
        warning: { DEFAULT: "#D97706", light: "#FEF3C7" },
        danger: { DEFAULT: "#DC2626", light: "#FEE2E2" },
        info: { DEFAULT: "#0891B2", light: "#CFFAFE" },
        surface: {
          DEFAULT: "#FFFFFF",
          dark: "#141414",
        },
        muted: { DEFAULT: "#6B7280", dark: "#9CA3AF" },
        border: { DEFAULT: "#E5E7EB", dark: "#262626" },
      },
      fontFamily: {
        display: ["Plus Jakarta Sans", "system-ui", "sans-serif"],
        body: ["DM Sans", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
      },
      fontSize: {
        "2xs": ["0.625rem", { lineHeight: "1rem" }],
      },
      borderRadius: {
        xl: "0.75rem",
        "2xl": "1rem",
      },
      animation: {
        "fade-in": "fadeIn 0.2s ease-out",
        "slide-up": "slideUp 0.3s ease-out",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;

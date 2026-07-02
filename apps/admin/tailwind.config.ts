import type { Config } from "tailwindcss";
import sharedConfig from "../../packages/ui/src/tailwind.config";

const config: Config = {
  ...sharedConfig,
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "../../packages/ui/src/**/*.{ts,tsx}",
  ],
};

export default config;

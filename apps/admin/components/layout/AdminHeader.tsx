"use client";

import { useAdminAuth } from "@/store/auth.store";
import { useTheme } from "next-themes";
import { Button, Avatar } from "@suvidha/ui";
import {
  Sun,
  Moon,
  Bell,
  LogOut,
  ChevronDown,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from "@suvidha/ui";
import { useEffect, useState } from "react";

export function AdminHeader() {
  const { user, logout } = useAdminAuth();
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-white/80 backdrop-blur-sm dark:bg-gray-900/80 px-6">
      <div>
        <h1 className="text-lg font-semibold font-display">
          {user?.name ? `Welcome, ${user.name}` : "Admin Dashboard"}
        </h1>
      </div>

      <div className="flex items-center gap-3">
        {/* Theme Toggle */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          aria-label="Toggle theme"
        >
          {mounted && theme === "dark" ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}
        </Button>

        {/* Notifications */}
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-5 w-5" />
        </Button>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="flex items-center gap-2 px-2">
              <Avatar className="h-8 w-8 bg-brand-500 text-white text-xs">
                {user?.name?.charAt(0)?.toUpperCase() || "A"}
              </Avatar>
              <span className="hidden sm:inline text-sm font-medium">
                {user?.name || "Admin"}
              </span>
              <ChevronDown className="h-4 w-4 text-muted" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <div className="px-2 py-1.5 text-sm text-muted">
              {user?.email}
            </div>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={logout} className="text-red-600">
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}

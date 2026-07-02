"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@suvidha/ui";
import {
  LayoutDashboard,
  Users,
  FileText,
  Receipt,
  Cable,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Gauge,
} from "lucide-react";
import { useAdminUI } from "@/store/ui.store";
import { useAdminAuth } from "@/store/auth.store";
import { Button } from "@suvidha/ui";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: Gauge },
  { href: "/admin/users", label: "Users", icon: Users },
  { href: "/admin/grievances", label: "Grievances", icon: FileText },
  { href: "/admin/payments", label: "Payments", icon: Receipt },
  { href: "/admin/connections", label: "Connections", icon: Cable },
  { href: "/admin/settings", label: "Settings", icon: Settings },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const { sidebarOpen, toggleSidebar } = useAdminUI();
  const { logout } = useAdminAuth();

  return (
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 flex h-screen flex-col border-r border-border bg-white dark:bg-gray-900 transition-all duration-300",
        sidebarOpen ? "w-64" : "w-16"
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center justify-between border-b border-border px-4">
        {sidebarOpen && (
          <Link href="/admin/dashboard" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500 text-white text-sm font-bold">
              S
            </div>
            <span className="font-display text-lg font-semibold">Suvidha</span>
          </Link>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={toggleSidebar}
          className="shrink-0"
          aria-label={sidebarOpen ? "Collapse sidebar" : "Expand sidebar"}
        >
          {sidebarOpen ? (
            <ChevronLeft className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          )}
        </Button>
      </div>

      {/* Nav Items */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300"
                  : "text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800",
                !sidebarOpen && "justify-center"
              )}
              title={!sidebarOpen ? item.label : undefined}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {sidebarOpen && <span>{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Logout */}
      <div className="border-t border-border p-3">
        <button
          onClick={logout}
          className={cn(
            "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 dark:text-gray-400 dark:hover:bg-red-950 dark:hover:text-red-400 transition-colors",
            !sidebarOpen && "justify-center"
          )}
          title={!sidebarOpen ? "Logout" : undefined}
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {sidebarOpen && <span>Logout</span>}
        </button>
      </div>
    </aside>
  );
}

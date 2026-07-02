"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { AdminHeader } from "@/components/layout/AdminHeader";
import { useAdminAuth } from "@/store/auth.store";
import { useAdminUI } from "@/store/ui.store";
import { cn } from "@suvidha/ui";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated } = useAdminAuth();
  const router = useRouter();
  const pathname = usePathname();
  const { sidebarOpen } = useAdminUI();

  useEffect(() => {
    const token = sessionStorage.getItem("adminToken");
    const storedUser = localStorage.getItem("adminUser");
    if (!token || !storedUser) {
      router.push("/login");
    }
  }, [router]);

  if (!isAuthenticated) {
    return <div className="flex h-screen items-center justify-center">Loading...</div>;
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <AdminSidebar />
      <div
        className={cn(
          "flex flex-1 flex-col transition-all duration-300",
          sidebarOpen ? "ml-64" : "ml-16"
        )}
      >
        <AdminHeader />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}

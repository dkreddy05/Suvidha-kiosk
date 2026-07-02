"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { Bell } from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
} from "@suvidha/ui";
import { formatDate, maskMobile } from "@/lib/utils";
import { notificationsApi, type Notification } from "@/lib/api/notifications";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { useAuthStore } from "@/store/auth.store";
import { queryKeys } from "@/lib/query-keys";
import { STALE_TIMES } from "@/lib/constants";

const NOTIFICATIONS_PAGE_SIZE = 10;

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.04, delayChildren: 0.06 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, x: -12 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.3, ease: "easeOut" } },
};

const messageTypeColor: Record<string, string> = {
  OTP: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
  BILLING: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
  GRIEVANCE: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300",
  CONNECTION: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300",
  GENERAL: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300",
};

function getBadgeClass(messageType: string): string {
  return messageTypeColor[messageType] ?? messageTypeColor.GENERAL;
}

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const citizen = useAuthStore((s) => s.citizen);

  const { data, isLoading, error } = useQuery({
    queryKey: [...queryKeys.notifications.history(citizen?.id ?? ""), page],
    queryFn: () =>
      notificationsApi.getHistory(citizen!.id, page, NOTIFICATIONS_PAGE_SIZE),
    enabled: !!citizen?.id,
    staleTime: STALE_TIMES.NOTIFICATIONS,
  });

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Notifications"
        description="View all SMS and notification history"
      />

      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="px-4 sm:px-6 lg:px-8 pb-8 max-w-3xl"
      >
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-brand-500" />
              Notification History
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-4">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="flex items-start gap-4">
                    <div className="w-3 h-3 mt-1.5 shrink-0">
                      <LoadingSkeleton rows={1} className="rounded-full" />
                    </div>
                    <div className="flex-1 space-y-2">
                      <LoadingSkeleton rows={1} className="w-1/4" />
                      <LoadingSkeleton rows={1} className="w-3/4" />
                    </div>
                  </div>
                ))}
              </div>
            ) : error ? (
              <p className="text-sm text-danger py-8 text-center">
                Failed to load notifications. Please try again.
              </p>
            ) : data && data.content.length > 0 ? (
              <div className="relative pl-6 border-l-2 border-gray-200 dark:border-gray-700 space-y-6">
                {data.content.map((notification: Notification, index: number) => (
                  <motion.div
                    key={notification.id}
                    variants={itemVariants}
                    className="relative"
                  >
                    {/* Timeline dot */}
                    <div className="absolute -left-[25px] top-1.5 flex items-center justify-center">
                      <div
                        className={`h-3 w-3 rounded-full border-2 ${
                          notification.status === "SENT"
                            ? "bg-green-500 border-green-200 dark:border-green-900"
                            : notification.status === "FAILED"
                              ? "bg-red-500 border-red-200 dark:border-red-900"
                              : "bg-amber-500 border-amber-200 dark:border-amber-900"
                        }`}
                      />
                    </div>

                    <div className="rounded-lg border border-border bg-white dark:bg-surface-dark p-4 shadow-sm space-y-2">
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <Badge className={getBadgeClass(notification.messageType)}>
                          {notification.messageType}
                        </Badge>
                        <span className="text-xs text-muted dark:text-muted-dark whitespace-nowrap">
                          {formatDate(notification.sentAt, "relative")}
                        </span>
                      </div>
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm text-gray-900 dark:text-gray-100 font-mono">
                          {maskMobile(notification.mobile)}
                        </span>
                        <StatusBadge status={notification.status} />
                      </div>
                    </div>
                  </motion.div>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={Bell}
                title="No notifications yet"
                description="You will see your SMS and notification history here once they are sent."
              />
            )}

            {/* Pagination */}
            {data && data.totalPages > 1 && (
              <div className="flex items-center justify-between pt-6 mt-6 border-t border-border">
                <p className="text-xs text-muted dark:text-muted-dark">
                  Page {data.number + 1} of {data.totalPages}
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

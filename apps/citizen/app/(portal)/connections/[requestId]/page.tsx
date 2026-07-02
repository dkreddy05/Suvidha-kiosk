"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useMemo } from "react";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Zap,
  Droplets,
  Flame,
  Clock,
  CheckCircle2,
  XCircle,
  MapPin,
  FileText,
  AlertCircle,
  AlertTriangle,
  Cable,
  ChevronRight,
} from "lucide-react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Skeleton,
  Progress,
} from "@suvidha/ui";
import { formatDate, cn } from "@/lib/utils";
import { useConnectionDetail } from "@/hooks/connections/useConnections";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type {
  ConnectionDetail,
  ConnectionStatus,
  ConnectionTimelineEntry,
} from "@/types/connections";
import type { ServiceType } from "@/types/billing";

const serviceIconMap: Record<ServiceType, typeof Zap> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  GAS: Flame,
};

const serviceNameMap: Record<ServiceType, string> = {
  ELECTRICITY: "Electricity",
  WATER: "Water",
  GAS: "Gas",
};

const serviceColorMap: Record<
  ServiceType,
  { bg: string; text: string; darkBg: string; darkText: string }
> = {
  ELECTRICITY: {
    bg: "bg-amber-100",
    text: "text-amber-700",
    darkBg: "dark:bg-amber-900/30",
    darkText: "dark:text-amber-300",
  },
  WATER: {
    bg: "bg-blue-100",
    text: "text-blue-700",
    darkBg: "dark:bg-blue-900/30",
    darkText: "dark:text-blue-300",
  },
  GAS: {
    bg: "bg-orange-100",
    text: "text-orange-700",
    darkBg: "dark:bg-orange-900/30",
    darkText: "dark:text-orange-300",
  },
};

const STATUS_ORDER: ConnectionStatus[] = [
  "PENDING",
  "UNDER_REVIEW",
  "APPROVED",
  "COMPLETED",
];

const STATUS_ICONS: Record<
  ConnectionStatus,
  typeof Clock
> = {
  PENDING: Clock,
  UNDER_REVIEW: Clock,
  APPROVED: CheckCircle2,
  COMPLETED: CheckCircle2,
  REJECTED: XCircle,
  CANCELLED: XCircle,
};

const STATUS_LABELS: Record<ConnectionStatus, string> = {
  PENDING: "Pending",
  UNDER_REVIEW: "Under Review",
  APPROVED: "Approved",
  COMPLETED: "Completed",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

function getStatusColor(
  status: ConnectionStatus,
  isActive: boolean,
  isCompleted: boolean
): string {
  if (isCompleted) return "text-green-500 border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-700";
  if (isActive) {
    if (status === "REJECTED" || status === "CANCELLED")
      return "text-red-500 border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-700";
    return "text-brand-500 border-brand-300 bg-brand-50 dark:bg-brand-900/20 dark:border-brand-700";
  }
  return "text-gray-300 border-gray-200 bg-gray-50 dark:text-gray-600 dark:border-gray-700 dark:bg-gray-800/50";
}

function getConnectorColor(
  isCompleted: boolean,
  isActive: boolean,
  isTerminal: boolean
): string {
  if (isTerminal && (isActive || !isCompleted)) return "bg-red-300 dark:bg-red-700";
  if (isCompleted) return "bg-green-400";
  if (isActive) return "bg-brand-400";
  return "bg-gray-200 dark:bg-gray-700";
}

function getProgressPercent(status: ConnectionStatus): number {
  if (status === "REJECTED" || status === "CANCELLED") return 60;
  const idx = STATUS_ORDER.indexOf(status);
  if (idx < 0) return 0;
  return ((idx + 1) / STATUS_ORDER.length) * 100;
}

function TimelineEntry({
  entry,
  index,
  total,
  currentStatus,
}: {
  entry: ConnectionTimelineEntry;
  index: number;
  total: number;
  currentStatus: ConnectionStatus;
}) {
  const Icon = STATUS_ICONS[entry.status] ?? Clock;
  const isTerminal =
    currentStatus === "REJECTED" || currentStatus === "CANCELLED";
  const isLast = index === total - 1;

  const statusIdx = STATUS_ORDER.indexOf(entry.status);
  const currentIdx = isTerminal
    ? STATUS_ORDER.indexOf(currentStatus as any)
    : STATUS_ORDER.indexOf(currentStatus);
  const isCompleted = statusIdx < currentIdx;
  const isActive = entry.status === currentStatus;

  const timelineStatus =
    isTerminal && index === total - 1 ? currentStatus : entry.status;
  const colorClass = getStatusColor(timelineStatus, isActive, isCompleted);
  const connectorColor = getConnectorColor(isCompleted, isActive, isTerminal);

  return (
    <div className="flex gap-4">
      <div className="flex flex-col items-center">
        <div
          className={cn(
            "flex h-8 w-8 items-center justify-center rounded-full border-2 shrink-0 transition-colors",
            colorClass
          )}
        >
          <Icon className="h-4 w-4" />
        </div>
        {!isLast && (
          <div
            className={cn(
              "w-0.5 h-full min-h-[24px] transition-colors",
              connectorColor
            )}
          />
        )}
      </div>
      <div className={cn("pb-8 flex-1", isLast && "pb-0")}>
        <div className="flex items-center gap-2 flex-wrap">
          <span
            className={cn(
              "text-sm font-semibold",
              isCompleted && "text-green-600 dark:text-green-400",
              isActive &&
                !isTerminal &&
                "text-brand-600 dark:text-brand-400",
              isActive &&
                isTerminal &&
                "text-red-600 dark:text-red-400",
              !isActive && !isCompleted && "text-gray-400 dark:text-gray-500"
            )}
          >
            {STATUS_LABELS[entry.status]}
          </span>
          <span className="text-xs text-muted dark:text-muted-dark">
            {formatDate(entry.timestamp)}
          </span>
        </div>
        {entry.message && (
          <p
            className={cn(
              "text-sm mt-1",
              isCompleted || isActive
                ? "text-gray-700 dark:text-gray-300"
                : "text-gray-400 dark:text-gray-500"
            )}
          >
            {entry.message}
          </p>
        )}
        {entry.updatedBy && (
          <p className="text-xs text-muted dark:text-muted-dark mt-0.5">
            Updated by {entry.updatedBy}
          </p>
        )}
      </div>
    </div>
  );
}

function TimelineSkeleton() {
  return (
    <div className="space-y-0">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          <div className="flex flex-col items-center">
            <Skeleton className="h-8 w-8 rounded-full" />
            {i < 3 && <Skeleton className="w-0.5 h-full min-h-[40px]" />}
          </div>
          <div className={cn("pb-6 flex-1", i === 3 && "pb-0")}>
            <div className="flex items-center gap-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-20" />
            </div>
            <Skeleton className="h-3 w-48 mt-2" />
          </div>
        </div>
      ))}
    </div>
  );
}

function ConnectionTimeline({
  detail,
}: {
  detail: ConnectionDetail;
}) {
  const timeline = detail.timeline ?? [];

  return (
    <div className="relative">
      {timeline.map((entry, idx) => (
        <TimelineEntry
          key={`${entry.status}-${idx}`}
          entry={entry}
          index={idx}
          total={timeline.length}
          currentStatus={detail.status}
        />
      ))}
    </div>
  );
}

export default function ConnectionDetailPage() {
  const params = useParams();
  const router = useRouter();
  const requestId = params.requestId as string;

  const {
    data: detail,
    isLoading,
    error,
    refetch,
  } = useConnectionDetail(requestId);

  const copyReference = useCallback(() => {
    if (!detail) return;
    navigator.clipboard.writeText(detail.referenceNumber);
  }, [detail]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Track Connection Request"
          breadcrumbs={[
            { label: "My Connections", href: "/connections" },
            { label: "Request Details" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8">
          <div className="max-w-3xl mx-auto space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <Skeleton className="h-14 w-14 rounded-xl" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-5 w-48" />
                    <Skeleton className="h-4 w-36" />
                    <Skeleton className="h-8 w-64 mt-1" />
                  </div>
                  <Skeleton className="h-7 w-28 rounded-full" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <Skeleton className="h-5 w-32" />
              </CardHeader>
              <CardContent>
                <TimelineSkeleton />
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Track Connection Request"
          breadcrumbs={[
            { label: "My Connections", href: "/connections" },
            { label: "Request Details" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertTriangle}
            title="Connection request not found"
            description={
              error
                ? "We couldn't load this request. Please try again."
                : "The connection request you're looking for doesn't exist or has been removed."
            }
            actionLabel={error ? "Retry" : "Back to Connections"}
            onAction={() =>
              error ? refetch() : router.push("/connections")
            }
          />
        </div>
      </div>
    );
  }

  const Icon = serviceIconMap[detail.serviceType] ?? Zap;
  const colors =
    serviceColorMap[detail.serviceType] ?? serviceColorMap.ELECTRICITY;
  const progressPercent = getProgressPercent(detail.status);
  const isTerminal =
    detail.status === "REJECTED" || detail.status === "CANCELLED";
  const slaDate = detail.slaEndDate
    ? new Date(detail.slaEndDate).toLocaleDateString("en-IN", {
        day: "numeric",
        month: "long",
        year: "numeric",
      })
    : null;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Track Connection Request"
        breadcrumbs={[
          { label: "My Connections", href: "/connections" },
          { label: detail.referenceNumber },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8">
        <div className="max-w-3xl mx-auto space-y-6">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, ease: "easeOut" }}
          >
            <Card>
              <CardContent className="pt-6">
                <div className="flex flex-col sm:flex-row sm:items-start gap-4">
                  <div
                    className={cn(
                      "flex h-14 w-14 items-center justify-center rounded-xl shrink-0",
                      colors.bg,
                      colors.text,
                      colors.darkBg,
                      colors.darkText
                    )}
                  >
                    <Icon className="h-7 w-7" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div>
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                          {serviceNameMap[detail.serviceType]} Connection
                        </h2>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-sm font-mono text-gray-900 dark:text-gray-100 tracking-wider">
                            {detail.referenceNumber}
                          </span>
                          <button
                            type="button"
                            onClick={copyReference}
                            className="text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                            title="Copy reference number"
                          >
                            <FileText className="h-4 w-4" />
                          </button>
                        </div>
                      </div>
                      <StatusBadge
                        status={detail.status}
                        className="text-sm px-3 py-1 self-start"
                      />
                    </div>

                    <div className="flex items-center gap-2 mt-3 text-sm text-muted dark:text-muted-dark">
                      <MapPin className="h-4 w-4 shrink-0" />
                      <span className="line-clamp-2">{detail.address}</span>
                    </div>

                    <div className="flex items-center gap-2 mt-1.5 text-sm text-muted dark:text-muted-dark">
                      <Clock className="h-4 w-4 shrink-0" />
                      <span>Submitted on {formatDate(detail.createdAt)}</span>
                    </div>

                    {slaDate && !isTerminal && (
                      <div className="mt-3 rounded-lg bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800 px-3 py-2">
                        <p className="text-sm text-blue-700 dark:text-blue-300">
                          Expected resolution by{" "}
                          <span className="font-semibold">{slaDate}</span>
                        </p>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-5 pt-4 border-t border-border">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold text-muted dark:text-muted-dark uppercase tracking-wider">
                      Application Progress
                    </span>
                    <span className="text-xs font-medium text-muted dark:text-muted-dark">
                      {isTerminal
                        ? detail.status === "REJECTED"
                          ? "Application Rejected"
                          : "Application Cancelled"
                        : `${Math.round(progressPercent)}% Complete`}
                    </span>
                  </div>
                  <Progress
                    value={progressPercent}
                    className={cn(
                      "h-2",
                      isTerminal && "[&>*]:bg-red-400"
                    )}
                  />
                </div>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: 0.1, ease: "easeOut" }}
          >
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Application Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                {detail.timeline && detail.timeline.length > 0 ? (
                  <ConnectionTimeline detail={detail} />
                ) : (
                  <div className="flex flex-col items-center py-8 text-center">
                    <Clock className="h-10 w-10 text-muted dark:text-muted-dark mb-3" />
                    <p className="text-sm text-muted dark:text-muted-dark">
                      No timeline updates yet. Your application is being
                      processed.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>

          <div className="flex items-center gap-3 pt-2">
            <Button
              variant="outline"
              onClick={() => router.push("/connections")}
            >
              <ArrowLeft className="h-4 w-4 mr-1.5" />
              Back to My Connections
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

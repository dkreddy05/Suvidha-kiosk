"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, useCallback } from "react";
import { motion } from "framer-motion";
import {
  Cable,
  PlusCircle,
  Clock,
  CheckCircle2,
  XCircle,
  ArrowLeft,
  ChevronRight,
  MapPin,
  FileText,
  Zap,
  Droplets,
  Flame,
  AlertCircle,
} from "lucide-react";
import { Button, Skeleton } from "@suvidha/ui";
import { formatDate, cn } from "@/lib/utils";
import { useConnections } from "@/hooks/connections/useConnections";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { ConnectionRequest, ConnectionStatus } from "@/types/connections";
import type { ServiceType } from "@/types/billing";

const ITEMS_PER_PAGE = 10;

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

const STATUS_STEPS: ConnectionStatus[] = [
  "PENDING",
  "UNDER_REVIEW",
  "APPROVED",
  "COMPLETED",
];

function getActiveStepIndex(status: ConnectionStatus): number {
  if (status === "REJECTED" || status === "CANCELLED") {
    const idx = STATUS_STEPS.indexOf(status as any);
    if (idx !== -1) return idx;
    return STATUS_STEPS.indexOf("UNDER_REVIEW");
  }
  const idx = STATUS_STEPS.indexOf(status);
  return idx >= 0 ? idx : 0;
}

function StatusStepper({ status }: { status: ConnectionStatus }) {
  const activeIndex = getActiveStepIndex(status);
  const isTerminal = status === "REJECTED" || status === "CANCELLED";
  const terminalLabel =
    status === "REJECTED" ? "Rejected" : "Cancelled";

  return (
    <div className="flex items-center gap-1.5">
      {STATUS_STEPS.map((step, idx) => {
        const StepIcon =
          idx < activeIndex
            ? CheckCircle2
            : idx === activeIndex && !isTerminal
              ? Clock
              : ChevronRight;
        const isActive = idx === activeIndex && !isTerminal;
        const isCompleted = idx < activeIndex;
        return (
          <div key={step} className="flex items-center gap-1.5">
            {idx > 0 && (
              <div
                className={cn(
                  "h-px w-3",
                  isCompleted
                    ? "bg-green-400"
                    : "bg-gray-200 dark:bg-gray-700"
                )}
              />
            )}
            <div className="flex items-center gap-1">
              <div
                className={cn(
                  "flex h-5 w-5 items-center justify-center rounded-full",
                  isCompleted && "bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400",
                  isActive && "bg-brand-100 text-brand-600 dark:bg-brand-900/30 dark:text-brand-400",
                  !isActive && !isCompleted && "bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500"
                )}
              >
                <StepIcon className="h-3 w-3" />
              </div>
              <span
                className={cn(
                  "text-xs font-medium hidden sm:inline",
                  isCompleted && "text-green-600 dark:text-green-400",
                  isActive && "text-brand-600 dark:text-brand-400",
                  !isActive && !isCompleted && "text-gray-400 dark:text-gray-500"
                )}
              >
                {step === "PENDING"
                  ? "Pending"
                  : step === "UNDER_REVIEW"
                    ? "Review"
                    : step === "APPROVED"
                      ? "Approved"
                      : "Completed"}
              </span>
            </div>
          </div>
        );
      })}
      {isTerminal && (
        <div className="flex items-center gap-1.5 ml-1">
          <div className="h-px w-3 bg-gray-200 dark:bg-gray-700" />
          <div className="flex items-center gap-1">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400">
              <XCircle className="h-3 w-3" />
            </div>
            <span className="text-xs font-medium text-red-600 dark:text-red-400 hidden sm:inline">
              {terminalLabel}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

function ConnectionCard({
  request,
}: {
  request: ConnectionRequest;
}) {
  const Icon = serviceIconMap[request.serviceType] ?? Zap;
  const colors =
    serviceColorMap[request.serviceType] ?? serviceColorMap.ELECTRICITY;

  const copyReference = useCallback(() => {
    navigator.clipboard.writeText(request.referenceNumber);
  }, [request.referenceNumber]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: "easeOut" }}
      className="rounded-xl border border-border bg-white dark:bg-surface-dark p-5 shadow-sm transition-all duration-200 hover:shadow-md"
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3 min-w-0">
            <div
              className={cn(
                "flex h-10 w-10 items-center justify-center rounded-lg shrink-0",
                colors.bg,
                colors.text,
                colors.darkBg,
                colors.darkText
              )}
            >
              <Icon className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                {serviceNameMap[request.serviceType]} Connection
              </p>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span className="text-xs font-mono text-muted dark:text-muted-dark truncate">
                  {request.referenceNumber}
                </span>
                <button
                  type="button"
                  onClick={copyReference}
                  className="shrink-0 text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                  title="Copy reference number"
                >
                  <FileText className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>
          <StatusBadge status={request.status} />
        </div>

        <div className="flex items-start gap-2 text-sm text-muted dark:text-muted-dark">
          <MapPin className="h-4 w-4 mt-0.5 shrink-0" />
          <span className="line-clamp-1">{request.address}</span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1 border-t border-border">
          <div className="flex items-center gap-2 text-xs text-muted dark:text-muted-dark">
            <Clock className="h-3.5 w-3.5" />
            <span>Submitted {formatDate(request.createdAt)}</span>
          </div>
          <div className="flex items-center gap-3">
            <StatusStepper status={request.status} />
            <Link
              href={`/connections/${request.id}`}
              className="inline-flex items-center gap-1 text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors shrink-0"
            >
              Track
              <ChevronRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

function ConnectionCardSkeleton() {
  return (
    <div className="rounded-xl border border-border bg-white dark:bg-surface-dark p-5 space-y-4">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <Skeleton className="h-10 w-10 rounded-lg" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-36" />
            <Skeleton className="h-3 w-28" />
          </div>
        </div>
        <Skeleton className="h-6 w-20 rounded-full" />
      </div>
      <Skeleton className="h-4 w-full" />
      <div className="flex items-center justify-between pt-1 border-t border-border">
        <Skeleton className="h-3 w-32" />
        <div className="flex items-center gap-2">
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-4 w-12" />
        </div>
      </div>
    </div>
  );
}

export default function ConnectionsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pageParam = searchParams.get("page");
  const currentPage = pageParam ? parseInt(pageParam, 10) : 0;

  const { data, isLoading, error, refetch } = useConnections(currentPage);

  const handlePageChange = useCallback(
    (page: number) => {
      router.push(`/connections${page > 0 ? `?page=${page}` : ""}`);
    },
    [router]
  );

  const requests = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Connection Requests"
          description="Track and manage your new connection applications"
          actions={
            <Link href="/connections/new">
              <Button>
                <PlusCircle className="h-4 w-4 mr-1.5" />
                Apply for New Connection
              </Button>
            </Link>
          }
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertCircle}
            title="Something went wrong"
            description="We couldn't load your connection requests. Please try again."
            actionLabel="Retry"
            onAction={() => refetch()}
          />
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Connection Requests"
          description="Track and manage your new connection applications"
          actions={
            <Link href="/connections/new">
              <Button>
                <PlusCircle className="h-4 w-4 mr-1.5" />
                Apply for New Connection
              </Button>
            </Link>
          }
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8 space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <ConnectionCardSkeleton key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (requests.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Connection Requests"
          description="Track and manage your new connection applications"
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={Cable}
            title="No connection requests yet"
            description="You haven't applied for any new connections yet. Start your application now."
            actionLabel="Apply Now"
            onAction={() => router.push("/connections/new")}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="My Connection Requests"
        description="Track and manage your new connection applications"
        actions={
          <Link href="/connections/new">
            <Button>
              <PlusCircle className="h-4 w-4 mr-1.5" />
              Apply for New Connection
            </Button>
          </Link>
        }
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8 space-y-4">
        {requests.map((request: ConnectionRequest) => (
          <ConnectionCard key={request.id} request={request} />
        ))}

        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4">
            <p className="text-sm text-muted dark:text-muted-dark">
              Page {currentPage + 1} of {totalPages}
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                Previous
              </Button>
              {Array.from({ length: totalPages }).map((_, i) => (
                <Button
                  key={i}
                  variant={i === currentPage ? "default" : "outline"}
                  size="sm"
                  onClick={() => handlePageChange(i)}
                  className="min-w-[36px]"
                >
                  {i + 1}
                </Button>
              ))}
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

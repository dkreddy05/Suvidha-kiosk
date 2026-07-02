"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback } from "react";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  ArrowLeft,
  FileText,
  Clock,
  AlertCircle,
  MessageSquare,
  ExternalLink,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  Skeleton,
} from "@suvidha/ui";
import { formatDate, cn } from "@/lib/utils";
import { useGrievanceDetail } from "@/hooks/grievances/useGrievances";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { GrievanceUpdate } from "@/types/grievances";
import {
  GRIEVANCE_STATUS_CONFIG,
  type GrievanceStatus,
} from "@/types/grievances";

function TimelineEntry({
  update,
  index,
  total,
}: {
  update: GrievanceUpdate;
  index: number;
  total: number;
}) {
  const config = GRIEVANCE_STATUS_CONFIG[update.status] ?? {
    label: update.status ?? "Unknown",
    variant: "default",
  };
  const isLast = index === total - 1;

  return (
    <div className="flex gap-4">
      <div className="flex flex-col items-center">
        <div
          className={cn(
            "flex h-8 w-8 items-center justify-center rounded-full border-2 shrink-0",
            "border-brand-300 bg-brand-50 text-brand-600 dark:bg-brand-900/20 dark:border-brand-700 dark:text-brand-400"
          )}
        >
          <MessageSquare className="h-4 w-4" />
        </div>
        {!isLast && (
          <div className="w-0.5 h-full min-h-[24px] bg-gray-200 dark:bg-gray-700" />
        )}
      </div>
      <div className={cn("pb-8 flex-1", isLast && "pb-0")}>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            Status changed to {config.label}
          </span>
          <span className="text-xs text-muted dark:text-muted-dark">
            {formatDate(update.timestamp)}
          </span>
        </div>
        {update.message && (
          <p className="text-sm text-gray-700 dark:text-gray-300 mt-1">
            {update.message}
          </p>
        )}
        {update.updatedBy && (
          <p className="text-xs text-muted dark:text-muted-dark mt-0.5">
            Updated by {update.updatedBy}
          </p>
        )}
      </div>
    </div>
  );
}

function TimelineSkeleton() {
  return (
    <div className="space-y-0">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          <div className="flex flex-col items-center">
            <Skeleton className="h-8 w-8 rounded-full" />
            {i < 2 && <Skeleton className="w-0.5 h-full min-h-[40px]" />}
          </div>
          <div className={cn("pb-6 flex-1", i === 2 && "pb-0")}>
            <div className="flex items-center gap-2">
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-3 w-20" />
            </div>
            <Skeleton className="h-3 w-56 mt-2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export default function GrievanceDetailPage() {
  const params = useParams();
  const router = useRouter();
  const refNumber = params.refNumber as string;

  const {
    data: detail,
    isLoading,
    error,
    refetch,
  } = useGrievanceDetail(refNumber);

  const copyReference = useCallback(() => {
    if (!detail) return;
    navigator.clipboard.writeText(detail.referenceNumber);
    toast.success("Reference number copied");
  }, [detail]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Track Grievance"
          breadcrumbs={[
            { label: "My Grievances", href: "/grievances" },
            { label: "Grievance Details" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8">
          <div className="max-w-3xl mx-auto space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 space-y-3">
                    <Skeleton className="h-7 w-56" />
                    <Skeleton className="h-5 w-24 rounded-full" />
                    <Skeleton className="h-4 w-36" />
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
                <Skeleton className="h-16 w-full" />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <Skeleton className="h-5 w-40" />
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
          title="Track Grievance"
          breadcrumbs={[
            { label: "My Grievances", href: "/grievances" },
            { label: "Grievance Details" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertCircle}
            title="Grievance not found"
            description={
              error
                ? "We couldn't load this grievance. Please try again."
                : "The grievance you're looking for doesn't exist or has been removed."
            }
            actionLabel={error ? "Retry" : "Back to My Grievances"}
            onAction={() =>
              error ? refetch() : router.push("/grievances")
            }
          />
        </div>
      </div>
    );
  }

  const updates = detail.updates ?? [];
  const isResolvedOrClosed =
    detail.status === "RESOLVED" || detail.status === "CLOSED";

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Track Grievance"
        breadcrumbs={[
          { label: "My Grievances", href: "/grievances" },
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
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-xl font-mono font-bold text-gray-900 dark:text-gray-100 tracking-wider">
                        {detail.referenceNumber}
                      </h2>
                      <button
                        type="button"
                        onClick={copyReference}
                        className="shrink-0 text-muted dark:text-muted-dark hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                        title="Copy reference number"
                      >
                        <FileText className="h-4 w-4" />
                      </button>
                    </div>
                    <Badge variant="info" className="mt-2">
                      {detail.category}
                    </Badge>
                    <div className="flex items-center gap-2 mt-2 text-sm text-muted dark:text-muted-dark">
                      <Clock className="h-4 w-4 shrink-0" />
                      <span>Submitted on {formatDate(detail.submittedAt)}</span>
                    </div>
                  </div>
                  <StatusBadge
                    status={detail.status}
                    className="text-sm px-3 py-1 self-start"
                  />
                </div>

                {isResolvedOrClosed && (
                  <div className="mt-4 rounded-lg bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800 px-3 py-2">
                    <p className="text-sm text-green-700 dark:text-green-300 font-medium">
                      This grievance has been{" "}
                      {detail.status === "RESOLVED" ? "resolved" : "closed"}.
                    </p>
                  </div>
                )}
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
                <CardTitle className="text-lg">Issue Details</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                  {detail.description}
                </p>
                {detail.photoUrl && (
                  <div className="mt-4">
                    <a
                      href={detail.photoUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
                    >
                      <ExternalLink className="h-4 w-4" />
                      View attached photo
                    </a>
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: 0.2, ease: "easeOut" }}
          >
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Updates Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                {updates.length > 0 ? (
                  <div className="relative">
                    {updates.map((update, idx) => (
                      <TimelineEntry
                        key={`${update.status}-${idx}`}
                        update={update}
                        index={idx}
                        total={updates.length}
                      />
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center py-8 text-center">
                    <Clock className="h-10 w-10 text-muted dark:text-muted-dark mb-3" />
                    <p className="text-sm text-muted dark:text-muted-dark">
                      No updates yet. Your grievance is being reviewed.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>

          <div className="flex items-center gap-3 pt-2">
            <Button
              variant="outline"
              onClick={() => router.push("/grievances")}
            >
              <ArrowLeft className="h-4 w-4 mr-1.5" />
              Back to My Grievances
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

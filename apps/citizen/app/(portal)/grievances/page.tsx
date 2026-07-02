"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, useCallback, useMemo } from "react";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  FileText,
  PlusCircle,
  AlertCircle,
  Search,
  Clock,
  ChevronRight,
} from "lucide-react";
import { Button, Skeleton, Badge, Tabs, TabsList, TabsTrigger } from "@suvidha/ui";
import { formatDate } from "@/lib/utils";
import { useGrievances } from "@/hooks/grievances/useGrievances";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { Grievance, GrievanceStatus } from "@/types/grievances";

const TAB_VALUES: Array<{ value: string; label: string; status?: GrievanceStatus }> = [
  { value: "all", label: "All" },
  { value: "OPEN", label: "Open", status: "OPEN" },
  { value: "IN_PROGRESS", label: "In Progress", status: "IN_PROGRESS" },
  { value: "RESOLVED", label: "Resolved", status: "RESOLVED" },
];

function GrievanceCard({ grievance }: { grievance: Grievance }) {
  const copyReference = useCallback(() => {
    navigator.clipboard.writeText(grievance.referenceNumber);
    toast.success("Reference number copied");
  }, [grievance.referenceNumber]);

  const descriptionExcerpt =
    grievance.description.length > 100
      ? grievance.description.slice(0, 100) + "..."
      : grievance.description;

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: "easeOut" }}
      className="rounded-xl border border-border bg-white dark:bg-surface-dark p-5 shadow-sm transition-all duration-200 hover:shadow-md"
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-start justify-between">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="text-sm font-mono font-semibold text-gray-900 dark:text-gray-100 tracking-wider">
                {grievance.referenceNumber}
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
            <Badge variant="info" className="mt-1.5">
              {grievance.category}
            </Badge>
          </div>
          <StatusBadge status={grievance.status} />
        </div>

        <p className="text-sm text-gray-700 dark:text-gray-300 line-clamp-2">
          {descriptionExcerpt}
        </p>

        <div className="flex items-center justify-between pt-1 border-t border-border">
          <div className="flex items-center gap-2 text-xs text-muted dark:text-muted-dark">
            <Clock className="h-3.5 w-3.5" />
            <span>Submitted {formatDate(grievance.submittedAt)}</span>
          </div>
          <Link
            href={`/grievances/${grievance.referenceNumber}`}
            className="inline-flex items-center gap-1 text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors shrink-0"
          >
            Track
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>
      </div>
    </motion.div>
  );
}

function GrievanceCardSkeleton() {
  return (
    <div className="rounded-xl border border-border bg-white dark:bg-surface-dark p-5 space-y-4">
      <div className="flex items-start justify-between">
        <div className="space-y-2 flex-1">
          <Skeleton className="h-4 w-40" />
          <Skeleton className="h-5 w-20 rounded-full" />
        </div>
        <Skeleton className="h-6 w-20 rounded-full" />
      </div>
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-3 w-3/4" />
      <div className="flex items-center justify-between pt-1 border-t border-border">
        <Skeleton className="h-3 w-32" />
        <Skeleton className="h-4 w-12" />
      </div>
    </div>
  );
}

export default function GrievancesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pageParam = searchParams.get("page");
  const tabParam = searchParams.get("status");
  const [searchQuery, setSearchQuery] = useState("");

  const currentPage = pageParam ? parseInt(pageParam, 10) : 0;
  const currentTab = TAB_VALUES.some((t) => t.value === tabParam) ? (tabParam ?? "all") : "all";
  const statusFilter = TAB_VALUES.find((t) => t.value === currentTab)?.status;

  const { data, isLoading, error, refetch } = useGrievances(currentPage, statusFilter);

  const handleTabChange = useCallback(
    (value: string) => {
      const params = new URLSearchParams(searchParams.toString());
      if (value === "all") {
        params.delete("status");
      } else {
        params.set("status", value);
      }
      params.delete("page");
      router.push(`/grievances${params.toString() ? `?${params.toString()}` : ""}`);
    },
    [router, searchParams]
  );

  const handlePageChange = useCallback(
    (page: number) => {
      const params = new URLSearchParams(searchParams.toString());
      if (page > 0) {
        params.set("page", page.toString());
      } else {
        params.delete("page");
      }
      router.push(`/grievances${params.toString() ? `?${params.toString()}` : ""}`);
    },
    [router, searchParams]
  );

  const allGrievances = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const filteredGrievances = useMemo(() => {
    if (!searchQuery.trim()) return allGrievances;
    const query = searchQuery.toLowerCase();
    return allGrievances.filter((g: Grievance) =>
      g.referenceNumber.toLowerCase().includes(query)
    );
  }, [allGrievances, searchQuery]);

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Grievances"
          description="Track and manage your submitted grievances"
          actions={
            <Link href="/grievances/new">
              <Button>
                <PlusCircle className="h-4 w-4 mr-1.5" />
                Submit New Grievance
              </Button>
            </Link>
          }
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertCircle}
            title="Something went wrong"
            description="We couldn't load your grievances. Please try again."
            actionLabel="Retry"
            onAction={() => refetch()}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="My Grievances"
        description="Track and manage your submitted grievances"
        actions={
          <Link href="/grievances/new">
            <Button>
              <PlusCircle className="h-4 w-4 mr-1.5" />
              Submit New Grievance
            </Button>
          </Link>
        }
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8 space-y-4">
        <Tabs value={currentTab} onValueChange={handleTabChange}>
          <TabsList>
            {TAB_VALUES.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted dark:text-muted-dark pointer-events-none" />
          <input
            type="text"
            placeholder="Search by reference number..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="flex h-10 w-full rounded-lg border border-border bg-white dark:bg-surface-dark pl-10 pr-3 py-2 text-sm ring-offset-background placeholder:text-muted dark:placeholder:text-muted-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2"
          />
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <GrievanceCardSkeleton key={i} />
            ))}
          </div>
        ) : filteredGrievances.length === 0 ? (
          <EmptyState
            icon={FileText}
            title={searchQuery ? "No matching grievances" : "No grievances found"}
            description={
              searchQuery
                ? "No grievances match your search. Try a different reference number."
                : "You haven't submitted any grievances yet."
            }
            actionLabel={searchQuery ? undefined : "Submit a Grievance"}
            onAction={searchQuery ? undefined : () => router.push("/grievances/new")}
          />
        ) : (
          <>
            <div className="space-y-4">
              {filteredGrievances.map((grievance: Grievance) => (
                <GrievanceCard key={grievance.id} grievance={grievance} />
              ))}
            </div>

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
          </>
        )}
      </div>
    </div>
  );
}

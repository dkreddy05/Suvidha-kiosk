"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState, useMemo, useCallback } from "react";
import { motion } from "framer-motion";
import {
  Zap,
  Droplets,
  Flame,
  ArrowLeft,
  ChevronRight,
  Receipt,
  AlertTriangle,
} from "lucide-react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Skeleton,
  Badge,
} from "@suvidha/ui";
import { formatCurrency, formatDate } from "@/lib/utils";
import { useAccounts } from "@/hooks/billing/useAccounts";
import { useBills } from "@/hooks/billing/useBills";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { ServiceType, Bill, BillStatus } from "@/types/billing";
import { SERVICE_TYPES, SERVICE_ICONS } from "@/types/billing";

const ITEMS_PER_PAGE = 10;

const serviceIconMap: Record<ServiceType, typeof Zap> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  GAS: Flame,
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

function BillRow({
  bill,
  accountId,
}: {
  bill: Bill;
  accountId: string;
}) {
  const isPayable = bill.status === "PENDING" || bill.status === "OVERDUE";

  return (
    <tr className="border-b border-border last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
      <td className="py-3.5 px-4 text-sm text-gray-900 dark:text-gray-100">
        {bill.billMonth}
      </td>
      <td className="py-3.5 px-4 text-sm font-mono text-muted dark:text-muted-dark">
        {bill.billNumber}
      </td>
      <td className="py-3.5 px-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
        {formatCurrency(bill.amount)}
      </td>
      <td className="py-3.5 px-4 text-sm text-muted dark:text-muted-dark">
        {formatDate(bill.dueDate)}
      </td>
      <td className="py-3.5 px-4">
        <StatusBadge status={bill.status} />
      </td>
      <td className="py-3.5 px-4">
        {isPayable ? (
          <Link href={`/billing/${accountId}/pay?billId=${bill.id}`}>
            <Button size="sm" variant="default">
              Pay
            </Button>
          </Link>
        ) : (
          <span className="inline-flex items-center gap-1.5 text-sm font-medium text-green-600 dark:text-green-400">
            <span>Paid</span>
            <span className="text-lg leading-none">✓</span>
          </span>
        )}
      </td>
    </tr>
  );
}

function BillTableSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div className="rounded-xl border border-border bg-white dark:bg-surface-dark overflow-hidden">
      <div className="hidden md:block">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-gray-50 dark:bg-gray-800/50">
              {["Month", "Bill No.", "Amount", "Due Date", "Status", "Action"].map(
                (h) => (
                  <th
                    key={h}
                    className="py-3 px-4 text-left text-xs font-semibold text-muted dark:text-muted-dark uppercase tracking-wider"
                  >
                    {h}
                  </th>
                )
              )}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: rows }).map((_, i) => (
              <tr key={i} className="border-b border-border last:border-b-0">
                {Array.from({ length: 6 }).map((_, j) => (
                  <td key={j} className="py-3.5 px-4">
                    <Skeleton
                      className={
                        "h-4 " +
                        (j === 0 ? "w-24" : j === 1 ? "w-20" : j === 2 ? "w-16" : j === 3 ? "w-28" : j === 4 ? "w-16" : "w-14")
                      }
                    />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="md:hidden space-y-3 p-4">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="space-y-2 border-b border-border pb-3 last:border-b-0 last:pb-0">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-16" />
          </div>
        ))}
      </div>
    </div>
  );
}

export default function AccountDetailPage() {
  const params = useParams();
  const router = useRouter();
  const accountId = params.accountId as string;

  const [page, setPage] = useState(0);

  const { data: accounts, isLoading: accountsLoading } = useAccounts();
  const { data: billsData, isLoading: billsLoading, error: billsError, refetch: refetchBills } =
    useBills(accountId, page);

  const account = useMemo(() => {
    if (!accounts) return undefined;
    return accounts.find((a) => a.id === accountId);
  }, [accounts, accountId]);

  const bills = useMemo(() => {
    if (!billsData) return [];
    return Array.isArray(billsData) ? billsData : (billsData as any).content ?? billsData ?? [];
  }, [billsData]);

  const totalPages = Math.max(1, Math.ceil(bills.length / ITEMS_PER_PAGE));
  const currentPage = Math.min(page, totalPages - 1);
  const startIdx = currentPage * ITEMS_PER_PAGE;
  const endIdx = startIdx + ITEMS_PER_PAGE;
  const paginatedBills = bills.slice(startIdx, endIdx);

  const handlePrevPage = useCallback(() => {
    setPage((p) => Math.max(0, p - 1));
  }, []);

  const handleNextPage = useCallback(() => {
    setPage((p) => Math.min(totalPages - 1, p + 1));
  }, [totalPages]);

  const isLoading = accountsLoading || billsLoading;

  if (accountsLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Account Details"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Account" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8">
          <div className="flex flex-col lg:flex-row gap-6">
            <div className="flex-1">
              <BillTableSkeleton rows={5} />
            </div>
            <div className="w-full lg:w-[30%]">
              <Card>
                <CardHeader>
                  <Skeleton className="h-5 w-28" />
                </CardHeader>
                <CardContent className="space-y-3">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="h-4 w-1/2" />
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!account) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Account Not Found"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Account" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertTriangle}
            title="Account not found"
            description="The utility account you're looking for doesn't exist or has been removed."
            actionLabel="Back to Billing"
            onAction={() => router.push("/billing")}
          />
        </div>
      </div>
    );
  }

  const Icon = serviceIconMap[account.serviceType] ?? Zap;
  const colors = serviceColorMap[account.serviceType] ?? serviceColorMap.ELECTRICITY;

  const totalPending = bills
    .filter((b: Bill) => b.status === "PENDING" || b.status === "OVERDUE")
    .reduce((sum: number, b: Bill) => sum + b.amount + (b.lateFee ?? 0), 0);
  const overdueCount = bills.filter((b: Bill) => b.status === "OVERDUE").length;
  const paidCount = bills.filter((b: Bill) => b.status === "PAID").length;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title={account.providerName}
        description={`Manage bills for your ${account.serviceType.toLowerCase()} account`}
        breadcrumbs={[
          { label: "Billing", href: "/billing" },
          { label: account.providerName },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1 min-w-0">
            {billsError ? (
              <EmptyState
                icon={AlertTriangle}
                title="Failed to load bills"
                description="We couldn't load your bills. Please try again."
                actionLabel="Retry"
                onAction={() => refetchBills()}
              />
            ) : bills.length === 0 ? (
              <Card>
                <CardContent className="py-12">
                  <EmptyState
                    icon={Receipt}
                    title="No bills found"
                    description="There are no bills for this account yet."
                  />
                </CardContent>
              </Card>
            ) : (
              <>
                <div className="rounded-xl border border-border bg-white dark:bg-surface-dark overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-border bg-gray-50 dark:bg-gray-800/50">
                          {["Month", "Bill No.", "Amount", "Due Date", "Status", "Action"].map(
                            (h) => (
                              <th
                                key={h}
                                className="py-3 px-4 text-left text-xs font-semibold text-muted dark:text-muted-dark uppercase tracking-wider whitespace-nowrap"
                              >
                                {h}
                              </th>
                            )
                          )}
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedBills.map((bill: Bill) => (
                          <BillRow key={bill.id} bill={bill} accountId={accountId} />
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {totalPages > 1 && (
                  <div className="flex items-center justify-between mt-4">
                    <p className="text-sm text-muted dark:text-muted-dark">
                      Page {currentPage + 1} of {totalPages}
                    </p>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handlePrevPage}
                        disabled={currentPage === 0}
                      >
                        Previous
                      </Button>
                      {Array.from({ length: totalPages }).map((_, i) => (
                        <Button
                          key={i}
                          variant={i === currentPage ? "default" : "outline"}
                          size="sm"
                          onClick={() => setPage(i)}
                          className="min-w-[36px]"
                        >
                          {i + 1}
                        </Button>
                      ))}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleNextPage}
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

          <div className="w-full lg:w-[30%] shrink-0">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-lg ${colors.bg} ${colors.text} ${colors.darkBg} ${colors.darkText}`}
                  >
                    <Icon className="h-4 w-4" />
                  </div>
                  <span className="text-sm">Account Summary</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <p className="text-xs text-muted dark:text-muted-dark uppercase tracking-wider font-semibold mb-1">
                    Provider
                  </p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {account.providerName}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted dark:text-muted-dark uppercase tracking-wider font-semibold mb-1">
                    Account Number
                  </p>
                  <p className="text-sm font-mono font-medium text-gray-900 dark:text-gray-100">
                    ••••{account.accountNumber.slice(-4)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted dark:text-muted-dark uppercase tracking-wider font-semibold mb-1">
                    Service Type
                  </p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 capitalize">
                    {account.serviceType.toLowerCase()}
                  </p>
                </div>
                <div className="border-t border-border pt-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted dark:text-muted-dark">Total Pending</span>
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">
                      {formatCurrency(totalPending)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted dark:text-muted-dark">Overdue</span>
                    <span className="text-sm font-medium text-red-600 dark:text-red-400">
                      {overdueCount} bill{overdueCount !== 1 ? "s" : ""}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted dark:text-muted-dark">Paid</span>
                    <span className="text-sm font-medium text-green-600 dark:text-green-400">
                      {paidCount} bill{paidCount !== 1 ? "s" : ""}
                    </span>
                  </div>
                </div>
                <div className="border-t border-border pt-3">
                  <Link href={`/billing/${accountId}/pay`}>
                    <Button className="w-full" size="sm">
                      Pay All Pending
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}

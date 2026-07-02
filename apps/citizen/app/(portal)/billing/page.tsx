"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueries } from "@tanstack/react-query";
import { motion } from "framer-motion";
import {
  Zap,
  Droplets,
  Flame,
  Plus,
  ChevronRight,
  AlertTriangle,
  Receipt,
} from "lucide-react";
import { Button, Skeleton } from "@suvidha/ui";
import { formatCurrency } from "@/lib/utils";
import { billingApi } from "@/lib/api/billing";
import { queryKeys } from "@/lib/query-keys";
import { STALE_TIMES } from "@/lib/constants";
import { useAccounts } from "@/hooks/billing/useAccounts";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { UtilityAccount, ServiceType, Bill, BillStatus } from "@/types/billing";
import type { PaginatedResponse } from "@/types/api";

const serviceIconMap: Record<ServiceType, typeof Zap> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  GAS: Flame,
};

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
};

function getLargestPendingBill(data: PaginatedResponse<Bill> | undefined | never[]): Bill | undefined {
  const bills = Array.isArray(data) ? data : data?.content;
  if (!bills || bills.length === 0) return undefined;
  const pending = bills.filter(
    (b: Bill) => b.status === "PENDING" || b.status === "OVERDUE"
  );
  if (pending.length === 0) return undefined;
  return pending.reduce((max, b) => (b.amount > max.amount ? b : max), pending[0]);
}

function getAccountBillStatus(data: PaginatedResponse<Bill> | undefined | never[]): BillStatus {
  const bills = Array.isArray(data) ? data : data?.content;
  if (!bills || bills.length === 0) return "PAID";
  if (bills.some((b: Bill) => b.status === "OVERDUE")) return "OVERDUE";
  if (bills.some((b: Bill) => b.status === "PENDING")) return "PENDING";
  return "PAID";
}

interface AccountCardProps {
  account: UtilityAccount;
  largestBill?: Bill;
  accountStatus: BillStatus;
}

function AccountCard({ account, largestBill, accountStatus }: AccountCardProps) {
  const Icon = serviceIconMap[account.serviceType] ?? Zap;

  return (
    <Link href={`/billing/${account.id}`}>
      <motion.div
        variants={itemVariants}
        className="group relative flex flex-col gap-4 rounded-xl border border-border bg-white dark:bg-surface-dark p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 cursor-pointer h-full"
      >
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div
              className={
                "flex h-10 w-10 items-center justify-center rounded-lg shrink-0 " +
                (account.serviceType === "ELECTRICITY"
                  ? "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300"
                  : account.serviceType === "WATER"
                    ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300"
                    : "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300")
              }
            >
              <Icon className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                {account.providerName}
              </p>
              <p className="text-xs text-muted dark:text-muted-dark font-mono mt-0.5">
                ••••{account.accountNumber.slice(-4)}
              </p>
            </div>
          </div>
          <StatusBadge status={accountStatus} />
        </div>

        <div className="flex items-end justify-between mt-auto">
          <div className="space-y-1">
            {largestBill ? (
              <>
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                  {formatCurrency(largestBill.amount)}
                </p>
                <p className="text-xs text-muted dark:text-muted-dark">
                  Due{" "}
                  {new Date(largestBill.dueDate).toLocaleDateString("en-IN", {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                  })}
                </p>
              </>
            ) : (
              <p className="text-sm text-muted dark:text-muted-dark">No pending bills</p>
            )}
          </div>
          <div className="flex items-center gap-1 text-sm font-medium text-brand-500 group-hover:text-brand-600 transition-colors">
            <span>View Details</span>
            <ChevronRight className="h-4 w-4" />
          </div>
        </div>
      </motion.div>
    </Link>
  );
}

function AccountCardSkeleton() {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border bg-white dark:bg-surface-dark p-5">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <Skeleton className="h-10 w-10 rounded-lg" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
        </div>
        <Skeleton className="h-6 w-16 rounded-full" />
      </div>
      <div className="flex items-end justify-between">
        <div className="space-y-2">
          <Skeleton className="h-6 w-24" />
          <Skeleton className="h-3 w-28" />
        </div>
        <Skeleton className="h-4 w-24" />
      </div>
    </div>
  );
}

export default function BillingPage() {
  const router = useRouter();
  const { data: accounts, isLoading: accountsLoading, error: accountsError, refetch: refetchAccounts } = useAccounts();

  const billsQueries = useQueries({
    queries: (accounts ?? []).map((account) => ({
      queryKey: [...queryKeys.billing.bills(account.id)],
      queryFn: () => billingApi.getBills(account.id),
      staleTime: STALE_TIMES.BILLS,
      enabled: !!account.id,
    })),
  });

  const isLoading = accountsLoading || billsQueries.some((q) => q.isLoading);

  if (accountsError) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Utility Accounts"
          description="Manage your linked utility accounts"
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertTriangle}
            title="Something went wrong"
            description="We couldn't load your accounts. Please try again later."
            actionLabel="Retry"
            onAction={() => refetchAccounts()}
          />
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Utility Accounts"
          description="Manage your linked utility accounts"
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <AccountCardSkeleton key={i} />
            ))}
          </div>
        </div>
      </div>
    );
  }

  const accountList = accounts ?? [];

  if (accountList.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="My Utility Accounts"
          description="Manage your linked utility accounts"
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={Receipt}
            title="No accounts linked"
            description="Link your first utility account to start paying bills online."
            actionLabel="Link Your First Account"
            onAction={() => router.push("/billing/link")}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="My Utility Accounts"
        description="Manage your linked utility accounts"
      />

      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="px-4 sm:px-6 lg:px-8 pb-8"
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {accountList.map((account, idx) => {
            const billsData = billsQueries[idx]?.data ?? [];
            const largestBill = getLargestPendingBill(billsData);
            const accountStatus = getAccountBillStatus(billsData);
            return (
              <AccountCard
                key={account.id}
                account={account}
                largestBill={largestBill}
                accountStatus={accountStatus}
              />
            );
          })}

          <Link href="/billing/link">
            <motion.div
              variants={itemVariants}
              className="flex h-full min-h-[180px] items-center justify-center rounded-xl border-2 border-dashed border-border bg-white dark:bg-surface-dark p-6 transition-all duration-200 hover:border-brand-400 hover:bg-brand-50/50 dark:hover:bg-brand-900/10 group cursor-pointer"
            >
              <div className="flex flex-col items-center gap-3 text-muted dark:text-muted-dark group-hover:text-brand-500 transition-colors">
                <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-dashed border-current">
                  <Plus className="h-6 w-6" />
                </div>
                <span className="text-sm font-semibold">Link New Account</span>
                <span className="text-xs">Add an electricity, water, or gas account</span>
              </div>
            </motion.div>
          </Link>
        </div>
      </motion.div>
    </div>
  );
}

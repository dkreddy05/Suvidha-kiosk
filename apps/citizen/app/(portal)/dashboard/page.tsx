"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import {
  Receipt,
  FileText,
  Cable,
  User,
  ChevronRight,
  Zap,
  Droplets,
  Flame,
  Plus,
  AlertTriangle,
} from "lucide-react";
import { cn, formatDate } from "@/lib/utils";
import { useProfile } from "@/hooks/auth/useAuth";
import { useAccounts } from "@/hooks/billing/useAccounts";
import { useGrievances } from "@/hooks/grievances/useGrievances";
import { useConnections } from "@/hooks/connections/useConnections";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { UtilityAccount, ServiceType } from "@/types/billing";
import type { Grievance } from "@/types/grievances";
import type { ConnectionRequest } from "@/types/connections";
import { CONNECTION_STATUS_LABELS } from "@/types/connections";

const greeting = () => {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
};

const serviceIconMap: Record<ServiceType, typeof Zap> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  GAS: Flame,
};

const quickActions = [
  {
    icon: Receipt,
    title: "Pay a Bill",
    description: "View and pay your utility bills",
    href: "/billing",
  },
  {
    icon: FileText,
    title: "Report Grievance",
    description: "Submit a complaint or issue",
    href: "/grievances/new",
  },
  {
    icon: Cable,
    title: "Track Connection",
    description: "Monitor your connection requests",
    href: "/connections",
  },
  {
    icon: User,
    title: "View Profile",
    description: "Manage your account settings",
    href: "/profile",
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.05, delayChildren: 0.08 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
};

interface AccountCardProps {
  account: UtilityAccount;
}

function AccountCard({ account }: AccountCardProps) {
  const Icon = serviceIconMap[account.serviceType] ?? Zap;

  return (
    <div className="flex shrink-0 w-64 flex-col gap-3 rounded-xl border border-border bg-white dark:bg-surface-dark p-4 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-0.5">
      <div className="flex items-center gap-3">
        <div
          className={cn(
            "flex h-10 w-10 items-center justify-center rounded-lg",
            account.serviceType === "ELECTRICITY" && "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
            account.serviceType === "WATER" && "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
            account.serviceType === "GAS" && "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300"
          )}
        >
          <Icon className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
            {account.providerName}
          </p>
          <p className="text-xs text-muted dark:text-muted-dark">
            ••••{account.accountNumber.slice(-4)}
          </p>
        </div>
      </div>
      <div className="flex items-center justify-between">
        <StatusBadge status={account.status} />
      </div>
    </div>
  );
}

interface GrievanceCardProps {
  grievance: Grievance;
}

function GrievanceCard({ grievance }: GrievanceCardProps) {
  const truncatedDesc =
    grievance.description.length > 80
      ? `${grievance.description.slice(0, 80)}...`
      : grievance.description;

  return (
    <div className="flex items-center justify-between rounded-xl border border-border bg-white dark:bg-surface-dark p-4 shadow-sm transition-all duration-200 hover:shadow-md">
      <div className="min-w-0 flex-1 space-y-1.5">
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono text-muted dark:text-muted-dark">
            #{grievance.referenceNumber}
          </span>
          <StatusBadge status={grievance.category} />
        </div>
        <p className="text-sm text-gray-900 dark:text-gray-100 truncate">
          {truncatedDesc}
        </p>
        <div className="flex items-center gap-3 text-xs text-muted dark:text-muted-dark">
          <StatusBadge status={grievance.status} />
          <span>{formatDate(grievance.submittedAt)}</span>
        </div>
      </div>
      <ChevronRight className="h-4 w-4 shrink-0 text-muted dark:text-muted-dark ml-3" />
    </div>
  );
}

interface ConnectionCardProps {
  connection: ConnectionRequest;
}

const stepOrder = ["PENDING", "UNDER_REVIEW", "APPROVED", "COMPLETED"];

function ConnectionCard({ connection }: ConnectionCardProps) {
  const currentStepIdx = stepOrder.indexOf(connection.status);

  return (
    <div className="rounded-xl border border-border bg-white dark:bg-surface-dark p-4 shadow-sm space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div
            className={cn(
              "flex h-8 w-8 items-center justify-center rounded-lg",
              connection.serviceType === "ELECTRICITY" && "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
              connection.serviceType === "WATER" && "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
              connection.serviceType === "GAS" && "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300"
            )}
          >
            {(() => {
              const Icon = serviceIconMap[connection.serviceType] ?? Zap;
              return <Icon className="h-4 w-4" />;
            })()}
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              {connection.serviceType.charAt(0) + connection.serviceType.slice(1).toLowerCase()} Connection
            </p>
            <p className="text-xs text-muted dark:text-muted-dark">
              Ref: {connection.referenceNumber}
            </p>
          </div>
        </div>
        <StatusBadge status={connection.status} />
      </div>

      {/* Stepper */}
      <div className="flex items-center gap-1 pt-1">
        {stepOrder.map((step, idx) => {
          const isCompleted = currentStepIdx > idx;
          const isCurrent = currentStepIdx === idx;
          const isLast = idx === stepOrder.length - 1;
          return (
            <div key={step} className="flex items-center gap-1 flex-1">
              <div
                className={cn(
                  "flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-bold transition-colors",
                  isCompleted
                    ? "bg-brand-500 text-white"
                    : isCurrent
                      ? "border-2 border-brand-500 text-brand-500"
                      : "border-2 border-gray-200 dark:border-gray-700 text-gray-400 dark:text-gray-500"
                )}
              >
                {isCompleted ? "✓" : idx + 1}
              </div>
              {!isLast && (
                <div
                  className={cn(
                    "h-0.5 flex-1",
                    isCompleted
                      ? "bg-brand-500"
                      : "bg-gray-200 dark:bg-gray-700"
                  )}
                />
              )}
            </div>
          );
        })}
      </div>
      <div className="flex items-center justify-between text-xs text-muted dark:text-muted-dark">
        <span>{CONNECTION_STATUS_LABELS[connection.status]}</span>
        <span>{formatDate(connection.createdAt)}</span>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { data: profile, isLoading: profileLoading, error: profileError } = useProfile();
  const { data: accounts, isLoading: accountsLoading, error: accountsError } = useAccounts();
  const { data: grievancesPage, isLoading: grievancesLoading, error: grievancesError } = useGrievances(0);
  const { data: connectionsPage, isLoading: connectionsLoading, error: connectionsError } = useConnections(0);

  const name = profile?.name ?? "User";
  const accountsList = accounts ?? [];
  const grievancesList = grievancesPage?.content ?? [];
  const connectionsList = connectionsPage?.content ?? [];

  const pendingBillsCount = 0;

  const openGrievancesCount = grievancesList.filter(
    (g) => g.status === "OPEN" || g.status === "IN_PROGRESS"
  ).length;

  const activeConnectionsCount = connectionsList.filter(
    (c) => c.status !== "CANCELLED" && c.status !== "REJECTED"
  ).length;

  const stats = [
    { label: "Accounts", value: accountsList.length, color: "bg-blue-500" },
    { label: "Pending Bills", value: pendingBillsCount, color: "bg-amber-500" },
    { label: "Open Grievances", value: openGrievancesCount, color: "bg-purple-500" },
    { label: "Active Connections", value: activeConnectionsCount, color: "bg-green-500" },
  ];

  const hasError = !!(profileError || accountsError || grievancesError || connectionsError);

  if (hasError) {
    return (
      <div className="px-4 sm:px-6 lg:px-8 pt-6">
        <EmptyState
          icon={AlertTriangle}
          title="Something went wrong"
          description="We couldn't load your dashboard. Please try again later."
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Dashboard"
        description="Overview of your accounts and activity"
      />

      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="px-4 sm:px-6 lg:px-8 pb-8 space-y-8"
      >
        {/* Welcome Banner */}
        <motion.section variants={itemVariants}>
          {profileLoading ? (
            <div className="rounded-2xl bg-gradient-to-br from-brand-600 to-brand-900 p-6 sm:p-8">
              <LoadingSkeleton rows={3} className="max-w-md" />
            </div>
          ) : (
            <div className="rounded-2xl bg-gradient-to-br from-brand-600 to-brand-900 p-6 sm:p-8 text-white">
              <h2 className="text-xl sm:text-2xl font-display font-bold">
                {greeting()}, {name}!
              </h2>
              <p className="text-brand-100 mt-1 text-sm">
                Here&apos;s what&apos;s happening with your services today.
              </p>
              <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
                {stats.map((stat) => (
                  <div
                    key={stat.label}
                    className="rounded-xl bg-white/10 backdrop-blur px-4 py-3"
                  >
                    <p className="text-2xl font-bold">{stat.value}</p>
                    <p className="text-brand-100 text-xs mt-0.5">{stat.label}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </motion.section>

        {/* Quick Actions */}
        <motion.section variants={itemVariants}>
          <h3 className="text-lg font-display font-semibold text-gray-900 dark:text-gray-100 mb-4">
            Quick Actions
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {quickActions.map((action) => {
              const Icon = action.icon;
              return (
                <Link
                  key={action.href}
                  href={action.href}
                  className="group rounded-xl border border-border bg-white dark:bg-surface-dark p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-0.5"
                >
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400 group-hover:bg-brand-100 dark:group-hover:bg-brand-900/30 transition-colors">
                      <Icon className="h-5 w-5" />
                    </div>
                    <ChevronRight className="h-4 w-4 text-muted dark:text-muted-dark group-hover:text-gray-900 dark:group-hover:text-gray-100 transition-colors" />
                  </div>
                  <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {action.title}
                  </h4>
                  <p className="text-xs text-muted dark:text-muted-dark mt-1">
                    {action.description}
                  </p>
                </Link>
              );
            })}
          </div>
        </motion.section>

        {/* Linked Accounts */}
        <motion.section variants={itemVariants}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-display font-semibold text-gray-900 dark:text-gray-100">
              Linked Accounts
            </h3>
            <Link
              href="/billing"
              className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
            >
              View All
            </Link>
          </div>

          {accountsLoading ? (
            <LoadingSkeleton type="card" rows={3} />
          ) : accountsList.length === 0 ? (
            <EmptyState
              icon={Receipt}
              title="No accounts linked"
              description="Link your utility accounts to start paying bills online."
            />
          ) : (
            <div className="flex gap-4 overflow-x-auto pb-2 -mx-4 sm:-mx-6 lg:-mx-8 px-4 sm:px-6 lg:px-8 scrollbar-thin">
              {accountsList.map((account) => (
                <AccountCard key={account.id} account={account} />
              ))}
              <Link
                href="/billing/link"
                className="flex shrink-0 w-64 items-center justify-center rounded-xl border-2 border-dashed border-border bg-white dark:bg-surface-dark p-4 transition-all duration-200 hover:border-brand-400 hover:bg-brand-50/50 dark:hover:bg-brand-900/10 group"
              >
                <div className="flex flex-col items-center gap-2 text-muted dark:text-muted-dark group-hover:text-brand-500 transition-colors">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-dashed border-current">
                    <Plus className="h-5 w-5" />
                  </div>
                  <span className="text-sm font-medium">Link Account</span>
                </div>
              </Link>
            </div>
          )}
        </motion.section>

        {/* Recent Grievances */}
        <motion.section variants={itemVariants}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-display font-semibold text-gray-900 dark:text-gray-100">
              Recent Grievances
            </h3>
            <Link
              href="/grievances"
              className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
            >
              View All
            </Link>
          </div>

          {grievancesLoading ? (
            <LoadingSkeleton type="card" rows={1} />
          ) : grievancesList.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No grievances found"
              description="You haven't submitted any grievances yet."
            />
          ) : (
            <div className="space-y-3">
              {grievancesList.slice(0, 3).map((grievance) => (
                <Link key={grievance.id} href={`/grievances/${grievance.referenceNumber}`}>
                  <GrievanceCard grievance={grievance} />
                </Link>
              ))}
            </div>
          )}
        </motion.section>

        {/* Pending Connections */}
        {connectionsList.length > 0 && (
          <motion.section variants={itemVariants}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-display font-semibold text-gray-900 dark:text-gray-100">
                Pending Connections
              </h3>
              <Link
                href="/connections"
                className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
              >
                View All
              </Link>
            </div>

            {connectionsLoading ? (
              <LoadingSkeleton type="card" rows={1} />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {connectionsList.slice(0, 3).map((connection) => (
                  <Link key={connection.id} href={`/connections/${connection.id}`}>
                    <ConnectionCard connection={connection} />
                  </Link>
                ))}
              </div>
            )}
          </motion.section>
        )}
      </motion.div>
    </div>
  );
}

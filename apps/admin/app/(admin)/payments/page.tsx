"use client";

import { useState, useMemo, useCallback } from "react";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  DollarSign,
  TrendingUp,
  TrendingDown,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
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
import { formatCurrency, formatDate } from "@suvidha/ui";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

type Period = "month" | "quarter" | "year";

interface PaymentTransaction {
  id: string;
  transactionId: string;
  account: string;
  amount: number;
  method: "UPI" | "CARD" | "CASH";
  status: "SUCCESS" | "FAILED" | "PENDING";
  date: string;
}

const PERIODS: { value: Period; label: string }[] = [
  { value: "month", label: "This Month" },
  { value: "quarter", label: "This Quarter" },
  { value: "year", label: "This Year" },
];

const METHOD_ICONS: Record<string, string> = {
  UPI: "💳",
  CARD: "💳",
  CASH: "💵",
};

const STATUS_VARIANT: Record<string, "success" | "rejected" | "pending"> = {
  SUCCESS: "success",
  FAILED: "rejected",
  PENDING: "pending",
};

const MOCK_VOLUME_DATA = [
  { month: "Jan", volume: 820000 },
  { month: "Feb", volume: 780000 },
  { month: "Mar", volume: 910000 },
  { month: "Apr", volume: 870000 },
  { month: "May", volume: 960000 },
  { month: "Jun", volume: 1020000 },
  { month: "Jul", volume: 980000 },
  { month: "Aug", volume: 1050000 },
  { month: "Sep", volume: 1120000 },
  { month: "Oct", volume: 1080000 },
  { month: "Nov", volume: 1150000 },
  { month: "Dec", volume: 1250000 },
];

const MOCK_TRANSACTIONS: PaymentTransaction[] = [
  { id: "TXN-001", transactionId: "pay_abc123def456", account: "ACC-1001", amount: 1250, method: "UPI", status: "SUCCESS", date: "2025-05-24T10:30:00Z" },
  { id: "TXN-002", transactionId: "pay_ghi789jkl012", account: "ACC-1002", amount: 3400, method: "CARD", status: "SUCCESS", date: "2025-05-24T09:15:00Z" },
  { id: "TXN-003", transactionId: "pay_mno345pqr678", account: "ACC-1003", amount: 750, method: "CASH", status: "FAILED", date: "2025-05-23T18:00:00Z" },
  { id: "TXN-004", transactionId: "pay_stu901vwx234", account: "ACC-1004", amount: 2100, method: "UPI", status: "SUCCESS", date: "2025-05-23T14:45:00Z" },
  { id: "TXN-005", transactionId: "pay_yza567bcd890", account: "ACC-1005", amount: 4800, method: "CARD", status: "PENDING", date: "2025-05-23T11:20:00Z" },
  { id: "TXN-006", transactionId: "pay_efg123hij456", account: "ACC-1006", amount: 920, method: "UPI", status: "SUCCESS", date: "2025-05-22T16:10:00Z" },
  { id: "TXN-007", transactionId: "pay_klm789nop012", account: "ACC-1007", amount: 5600, method: "CASH", status: "SUCCESS", date: "2025-05-22T08:30:00Z" },
  { id: "TXN-008", transactionId: "pay_qrs345tuv678", account: "ACC-1008", amount: 1800, method: "CARD", status: "FAILED", date: "2025-05-21T13:00:00Z" },
  { id: "TXN-009", transactionId: "pay_wxy901zab234", account: "ACC-1009", amount: 2950, method: "UPI", status: "SUCCESS", date: "2025-05-21T09:45:00Z" },
  { id: "TXN-010", transactionId: "pay_cde567fgh890", account: "ACC-1010", amount: 1100, method: "CASH", status: "SUCCESS", date: "2025-05-20T15:30:00Z" },
  { id: "TXN-011", transactionId: "pay_ijk123lmn456", account: "ACC-1011", amount: 7200, method: "UPI", status: "SUCCESS", date: "2025-05-20T11:00:00Z" },
  { id: "TXN-012", transactionId: "pay_opq789rst012", account: "ACC-1012", amount: 450, method: "CARD", status: "PENDING", date: "2025-05-19T17:20:00Z" },
];

function truncateId(id: string): string {
  if (id.length <= 12) return id;
  return `${id.slice(0, 6)}...${id.slice(-4)}`;
}

export default function PaymentsPage() {
  const [loading, setLoading] = useState(false);
  const [period, setPeriod] = useState<Period>("month");
  const [page, setPage] = useState(0);
  const perPage = 10;

  const summary = useMemo(() => {
    const total = MOCK_TRANSACTIONS.reduce((s, t) => s + t.amount, 0);
    const success = MOCK_TRANSACTIONS.filter((t) => t.status === "SUCCESS").length;
    const failed = MOCK_TRANSACTIONS.filter((t) => t.status === "FAILED").length;
    const pending = MOCK_TRANSACTIONS.filter((t) => t.status === "PENDING").length;
    const totalCount = MOCK_TRANSACTIONS.length;
    const successRate = totalCount > 0 ? ((success / totalCount) * 100) : 0;
    const avgValue = totalCount > 0 ? total / totalCount : 0;
    return { total, successCount: success, failedCount: failed, pendingCount: pending, successRate, avgValue };
  }, []);

  const sortedTxns = useMemo(
    () => [...MOCK_TRANSACTIONS].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()),
    []
  );

  const totalPages = Math.max(1, Math.ceil(sortedTxns.length / perPage));
  const paged = sortedTxns.slice(page * perPage, (page + 1) * perPage);

  const refresh = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      toast.success("Payments refreshed");
    }, 600);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Payment Analytics
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Track payment performance and transactions
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 rounded-lg bg-gray-100 p-1 dark:bg-gray-800">
            {PERIODS.map((p) => (
              <button
                key={p.value}
                type="button"
                onClick={() => setPeriod(p.value)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  period === p.value
                    ? "bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100"
                    : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>
          <Button variant="outline" size="sm" onClick={refresh} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="p-6">
                <Skeleton className="mb-2 h-4 w-24" />
                <Skeleton className="h-8 w-32" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    Total Revenue
                  </p>
                  <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {formatCurrency(summary.total)}
                  </p>
                </div>
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
                  <DollarSign className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    Transaction Count
                  </p>
                  <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {sortedTxns.length}
                  </p>
                </div>
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
                  <TrendingUp className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
              </div>
              <div className="mt-2 flex gap-3 text-xs text-gray-500 dark:text-gray-400">
                <span className="text-green-600 dark:text-green-400">{summary.successCount} success</span>
                <span className="text-red-600 dark:text-red-400">{summary.failedCount} failed</span>
                <span className="text-amber-600 dark:text-amber-400">{summary.pendingCount} pending</span>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    Success Rate
                  </p>
                  <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {summary.successRate.toFixed(1)}%
                  </p>
                </div>
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-teal-100 dark:bg-teal-900/30">
                  <TrendingUp className="h-5 w-5 text-teal-600 dark:text-teal-400" />
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    Average Transaction Value
                  </p>
                  <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                    {formatCurrency(summary.avgValue)}
                  </p>
                </div>
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                  <TrendingUp className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            Payment Volume Trend
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <Skeleton className="h-72 w-full rounded-lg" />
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={MOCK_VOLUME_DATA}>
                  <defs>
                    <linearGradient id="volGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    className="stroke-gray-200 dark:stroke-gray-700"
                  />
                  <XAxis
                    dataKey="month"
                    tick={{ fontSize: 12 }}
                    className="fill-gray-500 dark:fill-gray-400"
                  />
                  <YAxis
                    tick={{ fontSize: 12 }}
                    className="fill-gray-500 dark:fill-gray-400"
                    tickFormatter={(v: number) =>
                      v >= 100000 ? `${(v / 100000).toFixed(1)}L` : `${(v / 1000).toFixed(0)}K`
                    }
                  />
                  <Tooltip
                    contentStyle={{
                      borderRadius: "8px",
                      border: "1px solid #e2e8f0",
                      background: "#fff",
                    }}
                    formatter={(value: number) => [
                      formatCurrency(value),
                      "Volume",
                    ]}
                  />
                  <Area
                    type="monotone"
                    dataKey="volume"
                    stroke="#10b981"
                    strokeWidth={2}
                    fill="url(#volGradient)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            Recent Transactions
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 p-4">
                  <Skeleton className="h-4 w-28" />
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-5 w-20 rounded-full" />
                  <Skeleton className="h-4 w-28" />
                </div>
              ))}
            </div>
          ) : paged.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12">
              <DollarSign className="mb-3 h-12 w-12 text-gray-300 dark:text-gray-600" />
              <p className="text-lg font-medium text-gray-500 dark:text-gray-400">
                No transactions found
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 dark:border-gray-700">
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Transaction ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Account
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Amount
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Method
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Status
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Date
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {paged.map((txn) => (
                    <tr
                      key={txn.id}
                      className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                    >
                      <td className="px-4 py-3 font-mono text-sm text-gray-900 dark:text-gray-100">
                        {truncateId(txn.transactionId)}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                        {txn.account}
                      </td>
                      <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-gray-100">
                        {formatCurrency(txn.amount)}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                        {txn.method}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant={STATUS_VARIANT[txn.status]}>
                          {txn.status}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                        {formatDate(txn.date)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {!loading && paged.length > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Showing {(page * perPage) + 1}–{Math.min((page + 1) * perPage, sortedTxns.length)} of{" "}
            {sortedTxns.length} transactions
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </Button>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </motion.div>
  );
}

"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  TrendingUp,
  TrendingDown,
  Users,
  FileText,
  Receipt,
  Activity,
  DollarSign,
  Percent,
} from "lucide-react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  Skeleton,
} from "@suvidha/ui";
import type {
  DashboardMetrics,
  UserMetric,
  GrievanceMetric,
  PaymentMetric,
} from "@/types/index";
import { adminApi } from "@/lib/api/client";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";

type Period = "today" | "week" | "month" | "quarter";

interface PeriodOption {
  value: Period;
  label: string;
}

const PERIODS: PeriodOption[] = [
  { value: "today", label: "Today" },
  { value: "week", label: "This Week" },
  { value: "month", label: "This Month" },
  { value: "quarter", label: "This Quarter" },
];

const MOCK_METRICS: DashboardMetrics = {
  totalUsers: 28450,
  activeSessions: 1283,
  totalGrievances: 4567,
  openGrievances: 892,
  revenue: 1250000,
  paymentSuccessRate: 94.7,
};

const MOCK_USER_METRICS: UserMetric[] = [
  { date: "Dec", count: 4200 },
  { date: "Jan", count: 4800 },
  { date: "Feb", count: 5100 },
  { date: "Mar", count: 5600 },
  { date: "Apr", count: 6200 },
  { date: "May", count: 6800 },
];

const MOCK_GRIEVANCE_METRICS: GrievanceMetric[] = [
  { period: "Dec", open: 180, inProgress: 120, resolved: 300 },
  { period: "Jan", open: 200, inProgress: 140, resolved: 350 },
  { period: "Feb", open: 160, inProgress: 130, resolved: 380 },
  { period: "Mar", open: 220, inProgress: 150, resolved: 420 },
  { period: "Apr", open: 190, inProgress: 160, resolved: 450 },
  { period: "May", open: 210, inProgress: 170, resolved: 480 },
];

const MOCK_PAYMENT_METRICS: PaymentMetric[] = [
  { date: "Dec", volume: 450000, successRate: 95, revenue: 427500 },
  { date: "Jan", volume: 520000, successRate: 94, revenue: 488800 },
  { date: "Feb", volume: 480000, successRate: 96, revenue: 460800 },
  { date: "Mar", volume: 580000, successRate: 95, revenue: 551000 },
  { date: "Apr", volume: 620000, successRate: 94, revenue: 582800 },
  { date: "May", volume: 650000, successRate: 95, revenue: 617500 },
];

const GRIEVANCE_STATUS_DATA = [
  { name: "Open", value: 892, color: "#ef4444" },
  { name: "In Progress", value: 645, color: "#a855f7" },
  { name: "Resolved", value: 2170, color: "#22c55e" },
  { name: "Rejected", value: 340, color: "#f97316" },
  { name: "Closed", value: 520, color: "#64748b" },
];

const TRENDS: Record<string, { direction: "up" | "down"; value: string }> = {
  totalUsers: { direction: "up", value: "+12.5%" },
  activeSessions: { direction: "up", value: "+8.3%" },
  totalGrievances: { direction: "up", value: "+5.2%" },
  openGrievances: { direction: "down", value: "-3.1%" },
  revenue: { direction: "up", value: "+15.8%" },
  paymentSuccessRate: { direction: "up", value: "+1.2%" },
};

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  bgColor: string;
  trend?: { direction: "up" | "down"; value: string };
  loading?: boolean;
}

function StatCard({ title, value, icon, bgColor, trend, loading }: StatCardProps) {
  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-4">
            <Skeleton className="h-12 w-12 rounded-full" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-32" />
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center gap-4">
          <div
            className={`flex h-12 w-12 items-center justify-center rounded-full ${bgColor}`}
          >
            {icon}
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
              {title}
            </p>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                {value}
              </span>
              {trend && (
                <span
                  className={`inline-flex items-center gap-0.5 text-xs font-medium ${
                    trend.direction === "up"
                      ? "text-green-600 dark:text-green-400"
                      : "text-red-600 dark:text-red-400"
                  }`}
                >
                  {trend.direction === "up" ? (
                    <TrendingUp className="h-3 w-3" />
                  ) : (
                    <TrendingDown className="h-3 w-3" />
                  )}
                  {trend.value}
                </span>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function ChartSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-40" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-72 w-full rounded-lg" />
      </CardContent>
    </Card>
  );
}

function PeriodSelector({
  value,
  onChange,
}: {
  value: Period;
  onChange: (p: Period) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      {PERIODS.map((period) => (
        <button
          key={period.value}
          type="button"
          onClick={() => onChange(period.value)}
          className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
            value === period.value
              ? "bg-brand-500 text-white shadow-sm"
              : "bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700"
          }`}
        >
          {period.label}
        </button>
      ))}
    </div>
  );
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatNumber(num: number): string {
  if (num >= 100000) {
    return (num / 100000).toFixed(1) + "L";
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + "K";
  }
  return num.toLocaleString("en-IN");
}

export default function DashboardPage() {
  const router = useRouter();
  const [period, setPeriod] = useState<Period>("month");
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState<DashboardMetrics>(MOCK_METRICS);
  const [userMetrics, setUserMetrics] = useState<UserMetric[]>(MOCK_USER_METRICS);
  const [grievanceMetrics, setGrievanceMetrics] = useState<GrievanceMetric[]>(
    MOCK_GRIEVANCE_METRICS
  );
  const [paymentMetrics, setPaymentMetrics] = useState<PaymentMetric[]>(
    MOCK_PAYMENT_METRICS
  );

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [dashboardRes, userRes, grievanceRes, paymentRes] =
        await Promise.all([
          adminApi.getDashboard().catch(() => MOCK_METRICS),
          adminApi.getUserMetrics(period).catch(() => MOCK_USER_METRICS),
          adminApi
            .getGrievanceMetrics(period)
            .catch(() => MOCK_GRIEVANCE_METRICS),
          adminApi
            .getPaymentMetrics(period)
            .catch(() => MOCK_PAYMENT_METRICS),
        ]);

      setMetrics(dashboardRes as DashboardMetrics);
      setUserMetrics(userRes as UserMetric[]);
      setGrievanceMetrics(grievanceRes as GrievanceMetric[]);
      setPaymentMetrics(paymentRes as PaymentMetric[]);
    } catch {
      setMetrics(MOCK_METRICS);
      setUserMetrics(MOCK_USER_METRICS);
      setGrievanceMetrics(MOCK_GRIEVANCE_METRICS);
      setPaymentMetrics(MOCK_PAYMENT_METRICS);
      toast.error("Failed to load dashboard data. Showing sample data.");
    } finally {
      setLoading(false);
    }
  }, [period]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchData();
    }, 800);
    return () => clearTimeout(timer);
  }, [fetchData]);

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.05 },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      className="space-y-6"
    >
      <motion.div
        variants={itemVariants}
        className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
      >
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Dashboard
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Overview of platform metrics and analytics
          </p>
        </div>
        <PeriodSelector value={period} onChange={setPeriod} />
      </motion.div>

      <motion.div
        variants={itemVariants}
        className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
      >
        <StatCard
          title="Total Users"
          value={loading ? "—" : formatNumber(metrics.totalUsers)}
          icon={<Users className="h-5 w-5 text-white" />}
          bgColor="bg-blue-500"
          trend={TRENDS.totalUsers}
          loading={loading}
        />
        <StatCard
          title="Active Sessions"
          value={loading ? "—" : formatNumber(metrics.activeSessions)}
          icon={<Activity className="h-5 w-5 text-white" />}
          bgColor="bg-green-500"
          trend={TRENDS.activeSessions}
          loading={loading}
        />
        <StatCard
          title="Total Grievances"
          value={loading ? "—" : formatNumber(metrics.totalGrievances)}
          icon={<FileText className="h-5 w-5 text-white" />}
          bgColor="bg-amber-500"
          trend={TRENDS.totalGrievances}
          loading={loading}
        />
        <StatCard
          title="Open Grievances"
          value={loading ? "—" : formatNumber(metrics.openGrievances)}
          icon={
            <svg
              className="h-5 w-5 text-white"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          }
          bgColor="bg-red-500"
          trend={TRENDS.openGrievances}
          loading={loading}
        />
        <StatCard
          title="Revenue"
          value={loading ? "—" : formatCurrency(metrics.revenue)}
          icon={<DollarSign className="h-5 w-5 text-white" />}
          bgColor="bg-brand-500"
          trend={TRENDS.revenue}
          loading={loading}
        />
        <StatCard
          title="Payment Success %"
          value={loading ? "—" : `${metrics.paymentSuccessRate}%`}
          icon={<Percent className="h-5 w-5 text-white" />}
          bgColor="bg-teal-500"
          trend={TRENDS.paymentSuccessRate}
          loading={loading}
        />
      </motion.div>

      <motion.div
        variants={itemVariants}
        className="grid grid-cols-1 gap-6 lg:grid-cols-2"
      >
        {loading ? (
          <>
            <ChartSkeleton />
            <ChartSkeleton />
          </>
        ) : (
          <>
            <Card>
              <CardHeader>
                <CardTitle className="text-base font-semibold">
                  User Registrations
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={userMetrics}>
                      <defs>
                        <linearGradient
                          id="userGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="5%"
                            stopColor="#6366f1"
                            stopOpacity={0.3}
                          />
                          <stop
                            offset="95%"
                            stopColor="#6366f1"
                            stopOpacity={0}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        className="stroke-gray-200 dark:stroke-gray-700"
                      />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 12 }}
                        className="fill-gray-500 dark:fill-gray-400"
                      />
                      <YAxis
                        tick={{ fontSize: 12 }}
                        className="fill-gray-500 dark:fill-gray-400"
                      />
                      <Tooltip
                        contentStyle={{
                          borderRadius: "8px",
                          border: "1px solid #e2e8f0",
                          background: "var(--tooltip-bg, #fff)",
                        }}
                        formatter={(value: number) => [
                          value.toLocaleString("en-IN"),
                          "Users",
                        ]}
                      />
                      <Line
                        type="monotone"
                        dataKey="count"
                        stroke="#6366f1"
                        strokeWidth={2}
                        dot={{ r: 4, fill: "#6366f1" }}
                        activeDot={{ r: 6 }}
                        fill="url(#userGradient)"
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base font-semibold">
                  Grievance Resolution
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={grievanceMetrics}>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        className="stroke-gray-200 dark:stroke-gray-700"
                      />
                      <XAxis
                        dataKey="period"
                        tick={{ fontSize: 12 }}
                        className="fill-gray-500 dark:fill-gray-400"
                      />
                      <YAxis
                        tick={{ fontSize: 12 }}
                        className="fill-gray-500 dark:fill-gray-400"
                      />
                      <Tooltip
                        contentStyle={{
                          borderRadius: "8px",
                          border: "1px solid #e2e8f0",
                          background: "var(--tooltip-bg, #fff)",
                        }}
                      />
                      <Bar
                        dataKey="open"
                        name="Open"
                        stackId="a"
                        fill="#ef4444"
                        radius={[0, 0, 0, 0]}
                      />
                      <Bar
                        dataKey="inProgress"
                        name="In Progress"
                        stackId="a"
                        fill="#a855f7"
                        radius={[0, 0, 0, 0]}
                      />
                      <Bar
                        dataKey="resolved"
                        name="Resolved"
                        stackId="a"
                        fill="#22c55e"
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>
          </>
        )}
      </motion.div>

      <motion.div
        variants={itemVariants}
        className="grid grid-cols-1 gap-6 lg:grid-cols-2"
      >
        {loading ? (
          <>
            <ChartSkeleton />
            <ChartSkeleton />
          </>
        ) : (
          <>
            <Card>
              <CardHeader>
                <CardTitle className="text-base font-semibold">
                  Payment Volume
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={paymentMetrics}>
                      <defs>
                        <linearGradient
                          id="paymentGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="5%"
                            stopColor="#10b981"
                            stopOpacity={0.3}
                          />
                          <stop
                            offset="95%"
                            stopColor="#10b981"
                            stopOpacity={0}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        className="stroke-gray-200 dark:stroke-gray-700"
                      />
                      <XAxis
                        dataKey="date"
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
                          background: "var(--tooltip-bg, #fff)",
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
                        fill="url(#paymentGradient)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base font-semibold">
                  Grievances by Status
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex h-72 flex-col items-center justify-center">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={GRIEVANCE_STATUS_DATA}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={100}
                        paddingAngle={2}
                        dataKey="value"
                      >
                        {GRIEVANCE_STATUS_DATA.map((entry) => (
                          <Cell key={entry.name} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          borderRadius: "8px",
                          border: "1px solid #e2e8f0",
                          background: "var(--tooltip-bg, #fff)",
                        }}
                        formatter={(value: number, name: string) => [
                          value.toLocaleString("en-IN"),
                          name,
                        ]}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                  <div className="mt-2 flex flex-wrap justify-center gap-4">
                    {GRIEVANCE_STATUS_DATA.map((entry) => (
                      <div
                        key={entry.name}
                        className="flex items-center gap-1.5"
                      >
                        <span
                          className="h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: entry.color }}
                        />
                        <span className="text-xs text-gray-600 dark:text-gray-400">
                          {entry.name}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        )}
      </motion.div>
    </motion.div>
  );
}

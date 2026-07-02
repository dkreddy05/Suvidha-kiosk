"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  Zap,
  Droplets,
  Flame,
  Search,
  ChevronLeft,
  ChevronRight,
  Eye,
  RefreshCw,
  Filter,
} from "lucide-react";
import {
  Button,
  Card,
  CardContent,
  Badge,
  Input,
  Select,
  Skeleton,
} from "@suvidha/ui";
import { statusConfig } from "@suvidha/ui";
import { formatDate } from "@suvidha/ui";
import type { AdminConnection } from "@/types/index";
import { adminApi } from "@/lib/api/client";

const STATUS_OPTIONS = [
  "All Status",
  "PENDING",
  "UNDER_REVIEW",
  "APPROVED",
  "REJECTED",
  "COMPLETED",
];

const SERVICE_ICONS: Record<string, React.ReactNode> = {
  Electricity: <Zap className="h-4 w-4" />,
  Water: <Droplets className="h-4 w-4" />,
  Gas: <Flame className="h-4 w-4" />,
};

const MOCK_CONNECTIONS: AdminConnection[] = [
  {
    id: "CON-001",
    referenceNumber: "CN-2025-001",
    citizenId: "USR-001",
    serviceType: "Water",
    address: "42, MG Road, Indiranagar, Bangalore, Karnataka",
    status: "PENDING",
    createdAt: "2025-05-20T08:30:00Z",
    citizenName: "Rahul Sharma",
    citizenMobile: "9876543210",
  },
  {
    id: "CON-002",
    referenceNumber: "CN-2025-002",
    citizenId: "USR-005",
    serviceType: "Electricity",
    address: "120, Civil Lines, Jaipur, Rajasthan",
    status: "UNDER_REVIEW",
    createdAt: "2025-05-18T14:15:00Z",
    citizenName: "Vijay Singh",
    citizenMobile: "5432109876",
  },
  {
    id: "CON-003",
    referenceNumber: "CN-2025-003",
    citizenId: "USR-008",
    serviceType: "Gas",
    address: "102, Ashram Road, Ahmedabad, Gujarat",
    status: "APPROVED",
    createdAt: "2025-05-15T10:00:00Z",
    citizenName: "Anjali Mehta",
    citizenMobile: "2109876543",
  },
  {
    id: "CON-004",
    referenceNumber: "CN-2025-004",
    citizenId: "USR-011",
    serviceType: "Water",
    address: "12, Jubilee Hills, Hyderabad, Telangana",
    status: "COMPLETED",
    createdAt: "2025-05-10T09:45:00Z",
    citizenName: "Suresh Reddy",
    citizenMobile: "8877665544",
  },
  {
    id: "CON-005",
    referenceNumber: "CN-2025-005",
    citizenId: "USR-003",
    serviceType: "Electricity",
    address: "88, Sector 12, Noida, Uttar Pradesh",
    status: "REJECTED",
    createdAt: "2025-05-08T11:30:00Z",
    citizenName: "Amit Kumar",
    citizenMobile: "7654321098",
  },
  {
    id: "CON-006",
    referenceNumber: "CN-2025-006",
    citizenId: "USR-010",
    serviceType: "Gas",
    address: "33, Marine Drive, Kochi, Kerala",
    status: "PENDING",
    createdAt: "2025-05-22T16:00:00Z",
    citizenName: "Kavita Nair",
    citizenMobile: "9988776655",
  },
  {
    id: "CON-007",
    referenceNumber: "CN-2025-007",
    citizenId: "USR-015",
    serviceType: "Water",
    address: "67, Hazratganj, Lucknow, Uttar Pradesh",
    status: "UNDER_REVIEW",
    createdAt: "2025-05-21T07:00:00Z",
    citizenName: "Manoj Tiwari",
    citizenMobile: "4433221100",
  },
  {
    id: "CON-008",
    referenceNumber: "CN-2025-008",
    citizenId: "USR-004",
    serviceType: "Electricity",
    address: "7, Green Park Colony, Delhi",
    status: "APPROVED",
    createdAt: "2025-05-19T13:00:00Z",
    citizenName: "Sunita Verma",
    citizenMobile: "6543210987",
  },
  {
    id: "CON-009",
    referenceNumber: "CN-2025-009",
    citizenId: "USR-007",
    serviceType: "Gas",
    address: "29, FC Road, Pune, Maharashtra",
    status: "PENDING",
    createdAt: "2025-05-23T10:30:00Z",
    citizenName: "Ravi Deshmukh",
    citizenMobile: "3210987654",
  },
  {
    id: "CON-010",
    referenceNumber: "CN-2025-010",
    citizenId: "USR-013",
    serviceType: "Electricity",
    address: "22, VIP Road, Chandigarh",
    status: "REJECTED",
    createdAt: "2025-05-17T15:00:00Z",
    citizenName: "Arun Thakur",
    citizenMobile: "6655443322",
  },
  {
    id: "CON-011",
    referenceNumber: "CN-2025-011",
    citizenId: "USR-012",
    serviceType: "Water",
    address: "8, Cathedral Road, Chennai, Tamil Nadu",
    status: "COMPLETED",
    createdAt: "2025-05-05T08:00:00Z",
    citizenName: "Meera Iyer",
    citizenMobile: "7766554433",
  },
  {
    id: "CON-012",
    referenceNumber: "CN-2025-012",
    citizenId: "USR-014",
    serviceType: "Electricity",
    address: "90, Kalyani Nagar, Pune, Maharashtra",
    status: "UNDER_REVIEW",
    createdAt: "2025-05-24T12:00:00Z",
    citizenName: "Pooja Bhat",
    citizenMobile: "5544332211",
  },
];

export default function ConnectionsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState("All Status");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const perPage = 10;

  const filtered = useMemo(() => {
    let list = MOCK_CONNECTIONS;
    if (statusFilter !== "All Status") {
      list = list.filter((c) => c.status === statusFilter);
    }
    const q = search.toLowerCase().trim();
    if (q) {
      list = list.filter(
        (c) =>
          c.referenceNumber.toLowerCase().includes(q) ||
          c.citizenName?.toLowerCase().includes(q) ||
          c.serviceType.toLowerCase().includes(q) ||
          c.id.toLowerCase().includes(q)
      );
    }
    list.sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
    return list;
  }, [statusFilter, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / perPage));
  const paged = filtered.slice(page * perPage, (page + 1) * perPage);

  const refresh = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      toast.success("Connections refreshed");
    }, 600);
  }, []);

  const getStatusBadge = (status: string) => {
    const config = statusConfig[status];
    if (config) {
      return (
        <Badge variant={config.variant as any}>
          {config.label}
        </Badge>
      );
    }
    return <Badge>{status}</Badge>;
  };

  const getServiceIcon = (type: string) => {
    return SERVICE_ICONS[type] || null;
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Connection Requests
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Manage utility connection requests from citizens
          </p>
        </div>
        <Button variant="outline" onClick={refresh} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <Input
                placeholder="Search by reference, citizen, service..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                className="pl-10"
              />
            </div>
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-gray-400" />
              <Select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {s === "All Status" ? s : (statusConfig[s]?.label || s)}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <Card>
          <CardContent className="p-0">
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 p-4">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-4 w-28" />
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-5 w-20 rounded-full" />
                  <Skeleton className="h-4 w-28" />
                  <Skeleton className="h-8 w-16" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16">
            <Filter className="mb-3 h-12 w-12 text-gray-300 dark:text-gray-600" />
            <p className="text-lg font-medium text-gray-500 dark:text-gray-400">
              No connection requests found
            </p>
            <p className="text-sm text-gray-400 dark:text-gray-500">
              Try adjusting your search or filter criteria
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-700">
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Reference
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Citizen
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Service Type
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Status
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Submitted Date
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                    {paged.map((conn) => (
                      <tr
                        key={conn.id}
                        className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                      >
                        <td className="px-4 py-3">
                          <span className="font-mono text-sm text-brand-600 dark:text-brand-400">
                            {conn.referenceNumber}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">
                          {conn.citizenName || "—"}
                        </td>
                        <td className="px-4 py-3">
                          <span className="inline-flex items-center gap-1.5 rounded-md bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
                            {getServiceIcon(conn.serviceType)}
                            {conn.serviceType}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          {getStatusBadge(conn.status)}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                          {formatDate(conn.createdAt)}
                        </td>
                        <td className="px-4 py-3">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              router.push(
                                `/admin/connections/${conn.id}`
                              )
                            }
                          >
                            <Eye className="mr-1 h-4 w-4" />
                            View
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {(page * perPage) + 1}–{Math.min((page + 1) * perPage, filtered.length)} of{" "}
              {filtered.length} requests
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
        </>
      )}
    </motion.div>
  );
}

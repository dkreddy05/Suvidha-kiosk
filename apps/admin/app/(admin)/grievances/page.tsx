"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
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
import type { AdminGrievance } from "@/types/index";
import { adminApi } from "@/lib/api/client";

const CATEGORIES = [
  "All Categories",
  "Water Supply",
  "Electricity",
  "Sewage",
  "Road",
  "Waste",
  "Other",
];

const STATUS_OPTIONS = [
  "All Status",
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "REJECTED",
  "CLOSED",
];

const GRIEVANCE_STATUS_CONFIG: Record<string, { label: string; variant: "open" | "in_progress" | "resolved" | "rejected" | "default" }> = {
  OPEN: { label: "Open", variant: "open" },
  IN_PROGRESS: { label: "In Progress", variant: "in_progress" },
  RESOLVED: { label: "Resolved", variant: "resolved" },
  REJECTED: { label: "Rejected", variant: "rejected" },
  CLOSED: { label: "Closed", variant: "default" },
};

const MOCK_GRIEVANCES: AdminGrievance[] = [
  {
    id: "GRV-001",
    referenceNumber: "REF-2025-001",
    citizenId: "USR-001",
    category: "Water Supply",
    description: "No water supply in our area for the past 3 days. Residents are facing severe difficulties.",
    status: "OPEN",
    submittedAt: "2025-05-20T08:30:00Z",
    citizenName: "Rahul Sharma",
    citizenMobile: "9876543210",
  },
  {
    id: "GRV-002",
    referenceNumber: "REF-2025-002",
    citizenId: "USR-003",
    category: "Electricity",
    description: "Frequent power cuts in Sector 12 area. Voltage fluctuations damaging appliances.",
    status: "IN_PROGRESS",
    submittedAt: "2025-05-18T14:15:00Z",
    citizenName: "Amit Kumar",
    citizenMobile: "7654321098",
  },
  {
    id: "GRV-003",
    referenceNumber: "REF-2025-003",
    citizenId: "USR-005",
    category: "Road",
    description: "Large pothole on Civil Lines main road causing accidents. Immediate repair needed.",
    status: "RESOLVED",
    submittedAt: "2025-05-15T10:00:00Z",
    citizenName: "Vijay Singh",
    citizenMobile: "5432109876",
  },
  {
    id: "GRV-004",
    referenceNumber: "REF-2025-004",
    citizenId: "USR-008",
    category: "Sewage",
    description: "Sewage line blocked near Ashram Road junction. Foul smell and unhygienic conditions.",
    status: "OPEN",
    submittedAt: "2025-05-22T09:45:00Z",
    citizenName: "Anjali Mehta",
    citizenMobile: "2109876543",
  },
  {
    id: "GRV-005",
    referenceNumber: "REF-2025-005",
    citizenId: "USR-001",
    category: "Waste",
    description: "Garbage not collected from our street for over a week. Overflowing bins.",
    status: "REJECTED",
    submittedAt: "2025-05-10T11:30:00Z",
    citizenName: "Rahul Sharma",
    citizenMobile: "9876543210",
  },
  {
    id: "GRV-006",
    referenceNumber: "REF-2025-006",
    citizenId: "USR-010",
    category: "Water Supply",
    description: "Low water pressure in Marine Drive area during peak hours.",
    status: "IN_PROGRESS",
    submittedAt: "2025-05-19T16:00:00Z",
    citizenName: "Kavita Nair",
    citizenMobile: "9988776655",
  },
  {
    id: "GRV-007",
    referenceNumber: "REF-2025-007",
    citizenId: "USR-015",
    category: "Electricity",
    description: "Street lights not working on Hazratganj main road since a week. Safety concern.",
    status: "CLOSED",
    submittedAt: "2025-05-05T07:00:00Z",
    citizenName: "Manoj Tiwari",
    citizenMobile: "4433221100",
  },
  {
    id: "GRV-008",
    referenceNumber: "REF-2025-008",
    citizenId: "USR-011",
    category: "Other",
    description: "Request for new street light installation at Jubilee Hills park entrance.",
    status: "OPEN",
    submittedAt: "2025-05-23T13:00:00Z",
    citizenName: "Suresh Reddy",
    citizenMobile: "8877665544",
  },
  {
    id: "GRV-009",
    referenceNumber: "REF-2025-009",
    citizenId: "USR-005",
    category: "Road",
    description: "Damaged footpath on MI Road needs urgent repair. Elderly residents struggling to walk.",
    status: "RESOLVED",
    submittedAt: "2025-05-12T08:00:00Z",
    citizenName: "Vijay Singh",
    citizenMobile: "5432109876",
  },
  {
    id: "GRV-010",
    referenceNumber: "REF-2025-010",
    citizenId: "USR-013",
    category: "Water Supply",
    description: "Water tanker not reaching our colony despite multiple complaints.",
    status: "REJECTED",
    submittedAt: "2025-05-08T10:30:00Z",
    citizenName: "Arun Thakur",
    citizenMobile: "6655443322",
  },
  {
    id: "GRV-011",
    referenceNumber: "REF-2025-011",
    citizenId: "USR-004",
    category: "Sewage",
    description: "Manhole cover missing near Green Park Colony entrance. Dangerous for pedestrians.",
    status: "IN_PROGRESS",
    submittedAt: "2025-05-21T15:00:00Z",
    citizenName: "Sunita Verma",
    citizenMobile: "6543210987",
  },
  {
    id: "GRV-012",
    referenceNumber: "REF-2025-012",
    citizenId: "USR-007",
    category: "Waste",
    description: "Bulk waste pickup request for household renovation debris.",
    status: "RESOLVED",
    submittedAt: "2025-05-14T12:00:00Z",
    citizenName: "Ravi Deshmukh",
    citizenMobile: "3210987654",
  },
];

export default function GrievancesPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState("All Status");
  const [categoryFilter, setCategoryFilter] = useState("All Categories");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const perPage = 10;

  const filtered = useMemo(() => {
    let list = MOCK_GRIEVANCES;
    if (statusFilter !== "All Status") {
      list = list.filter((g) => g.status === statusFilter);
    }
    if (categoryFilter !== "All Categories") {
      list = list.filter((g) => g.category === categoryFilter);
    }
    const q = search.toLowerCase().trim();
    if (q) {
      list = list.filter(
        (g) =>
          g.referenceNumber.toLowerCase().includes(q) ||
          g.citizenName?.toLowerCase().includes(q) ||
          g.category.toLowerCase().includes(q) ||
          g.id.toLowerCase().includes(q)
      );
    }
    list.sort(
      (a, b) =>
        new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()
    );
    return list;
  }, [statusFilter, categoryFilter, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / perPage));
  const paged = filtered.slice(page * perPage, (page + 1) * perPage);

  const refresh = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      toast.success("Grievances refreshed");
    }, 600);
  }, []);

  const getStatusBadge = (status: string) => {
    const config = GRIEVANCE_STATUS_CONFIG[status] || statusConfig[status];
    if (config) {
      return <Badge variant={config.variant as any}>{config.label}</Badge>;
    }
    return <Badge>{status}</Badge>;
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
            Grievance Management
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            View and manage citizen grievances
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
                placeholder="Search by reference, citizen, category..."
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
                    {s === "All Status" ? s : GRIEVANCE_STATUS_CONFIG[s]?.label || s}
                  </option>
                ))}
              </Select>
              <Select
                value={categoryFilter}
                onChange={(e) => {
                  setCategoryFilter(e.target.value);
                  setPage(0);
                }}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
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
              No grievances match your filters
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
                        Category
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
                    {paged.map((grievance) => (
                      <tr
                        key={grievance.id}
                        className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                      >
                        <td className="px-4 py-3">
                          <span className="font-mono text-sm text-brand-600 dark:text-brand-400">
                            {grievance.referenceNumber}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">
                          {grievance.citizenName || "—"}
                        </td>
                        <td className="px-4 py-3">
                          <Badge variant="info">{grievance.category}</Badge>
                        </td>
                        <td className="px-4 py-3">
                          {getStatusBadge(grievance.status)}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                          {formatDate(grievance.submittedAt)}
                        </td>
                        <td className="px-4 py-3">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              router.push(
                                `/admin/grievances/${grievance.referenceNumber}`
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
              {filtered.length} grievances
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

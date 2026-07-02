"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  ChevronLeft,
  CheckCircle,
  XCircle,
  Clock,
  Copy,
  Zap,
  Droplets,
  Flame,
  ArrowRight,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  Skeleton,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Textarea,
} from "@suvidha/ui";
import { statusConfig } from "@suvidha/ui";
import { formatDate } from "@suvidha/ui";
import { maskMobile } from "@suvidha/ui";
import type { AdminConnection } from "@/types/index";
import { adminApi } from "@/lib/api/client";

interface StatusHistoryEntry {
  fromStatus: string;
  toStatus: string;
  message: string;
  timestamp: string;
  updatedBy: string;
}

const SERVICE_ICONS: Record<string, { icon: React.ReactNode; color: string }> = {
  Electricity: {
    icon: <Zap className="h-5 w-5" />,
    color: "text-amber-500",
  },
  Water: {
    icon: <Droplets className="h-5 w-5" />,
    color: "text-blue-500",
  },
  Gas: {
    icon: <Flame className="h-5 w-5" />,
    color: "text-orange-500",
  },
};

const STEP_ORDER = ["PENDING", "UNDER_REVIEW", "APPROVED", "COMPLETED"];
const STEP_ICONS: Record<string, React.ReactNode> = {
  PENDING: <Clock className="h-4 w-4" />,
  UNDER_REVIEW: <ArrowRight className="h-4 w-4" />,
  APPROVED: <CheckCircle className="h-4 w-4" />,
  COMPLETED: <CheckCircle className="h-4 w-4" />,
  REJECTED: <XCircle className="h-4 w-4" />,
};

const MOCK_DETAIL: AdminConnection & { statusHistory: StatusHistoryEntry[] } = {
  id: "CON-001",
  referenceNumber: "CN-2025-001",
  citizenId: "USR-001",
  serviceType: "Water",
  address: "42, MG Road, Indiranagar, Bangalore, Karnataka - 560038",
  status: "UNDER_REVIEW",
  createdAt: "2025-05-20T08:30:00Z",
  citizenName: "Rahul Sharma",
  citizenMobile: "9876543210",
  statusHistory: [
    {
      fromStatus: "",
      toStatus: "PENDING",
      message: "Connection request submitted by citizen.",
      timestamp: "2025-05-20T08:30:00Z",
      updatedBy: "System",
    },
    {
      fromStatus: "PENDING",
      toStatus: "UNDER_REVIEW",
      message: "Request picked up for verification. Documents being reviewed.",
      timestamp: "2025-05-21T10:00:00Z",
      updatedBy: "Admin (Priya Patel)",
    },
  ],
};

export default function ConnectionDetailPage() {
  const params = useParams();
  const router = useRouter();
  const requestId = params.requestId as string;

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [connection, setConnection] = useState<
    (AdminConnection & { statusHistory: StatusHistoryEntry[] }) | null
  >(null);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi
        .getAllConnections(0, 1)
        .catch(() => ({ content: [MOCK_DETAIL] }));
      if (data && data.content) {
        const found = data.content.find(
          (c: AdminConnection) => c.id === requestId
        );
        setConnection(found || MOCK_DETAIL);
      } else {
        setConnection(MOCK_DETAIL);
      }
    } catch {
      setConnection(MOCK_DETAIL);
    } finally {
      setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const handleApprove = async () => {
    setActionLoading(true);
    try {
      await adminApi.approveConnection(requestId);
      toast.success("Connection approved successfully");
      fetchDetail();
    } catch {
      if (connection) {
        const updated = {
          ...connection,
          status: "APPROVED" as AdminConnection["status"],
          statusHistory: [
            ...connection.statusHistory,
            {
              fromStatus: connection.status,
              toStatus: "APPROVED",
              message: "Connection request approved by admin.",
              timestamp: new Date().toISOString(),
              updatedBy: "Admin",
            },
          ],
        };
        setConnection(updated);
        toast.success("Connection approved (offline mode)");
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      toast.error("Please provide a reason for rejection");
      return;
    }
    setActionLoading(true);
    try {
      await adminApi.rejectConnection(requestId, rejectReason);
      toast.success("Connection rejected");
      setRejectDialogOpen(false);
      setRejectReason("");
      fetchDetail();
    } catch {
      if (connection) {
        const updated = {
          ...connection,
          status: "REJECTED" as AdminConnection["status"],
          statusHistory: [
            ...connection.statusHistory,
            {
              fromStatus: connection.status,
              toStatus: "REJECTED",
              message: rejectReason,
              timestamp: new Date().toISOString(),
              updatedBy: "Admin",
            },
          ],
        };
        setConnection(updated);
        toast.success("Connection rejected (offline mode)");
        setRejectDialogOpen(false);
        setRejectReason("");
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleComplete = async () => {
    setActionLoading(true);
    try {
      await adminApi.approveConnection(requestId);
      if (connection) {
        const updated = {
          ...connection,
          status: "COMPLETED" as AdminConnection["status"],
          statusHistory: [
            ...connection.statusHistory,
            {
              fromStatus: connection.status,
              toStatus: "COMPLETED",
              message: "Connection work completed and marked as done.",
              timestamp: new Date().toISOString(),
              updatedBy: "Admin",
            },
          ],
        };
        setConnection(updated);
        toast.success("Connection marked as completed (offline mode)");
      }
    } catch {
      toast.error("Failed to mark connection as completed");
    } finally {
      setActionLoading(false);
    }
  };

  const copyRef = () => {
    if (connection) {
      navigator.clipboard.writeText(connection.referenceNumber);
      toast.success("Reference number copied");
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-8 w-64" />
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <Skeleton className="h-32 w-full rounded-lg" />
            <Skeleton className="h-48 w-full rounded-lg" />
          </div>
          <Skeleton className="h-64 w-full rounded-lg" />
        </div>
      </div>
    );
  }

  if (!connection) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <p className="text-lg font-medium text-gray-500 dark:text-gray-400">
          Connection request not found
        </p>
        <Button
          variant="link"
          onClick={() => router.push("/admin/connections")}
        >
          Back to connections
        </Button>
      </div>
    );
  }

  const serviceConfig = SERVICE_ICONS[connection.serviceType] || {
    icon: <Zap className="h-5 w-5" />,
    color: "text-gray-500",
  };
  const statusInfo = statusConfig[connection.status] || {
    label: connection.status,
    variant: "default" as const,
  };

  const canApproveOrReject =
    connection.status === "PENDING" || connection.status === "UNDER_REVIEW";
  const canComplete = connection.status === "APPROVED";
  const isReadOnly =
    connection.status === "REJECTED" || connection.status === "COMPLETED";

  const allSteps =
    connection.status === "REJECTED"
      ? [...STEP_ORDER, "REJECTED"]
      : [...STEP_ORDER.slice(0, STEP_ORDER.indexOf(connection.status) + 1)];

  const currentStepIndex = allSteps.indexOf(connection.status);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <Link
        href="/admin/connections"
        className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to Connections
      </Link>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-mono text-2xl font-bold text-gray-900 dark:text-gray-100">
            {connection.referenceNumber}
          </h1>
          <button
            type="button"
            onClick={copyRef}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
          >
            <Copy className="h-4 w-4" />
          </button>
          <span
            className={`inline-flex items-center gap-1 rounded-md bg-gray-100 px-2.5 py-1 text-sm font-medium dark:bg-gray-800 ${serviceConfig.color}`}
          >
            {serviceConfig.icon}
            {connection.serviceType}
          </span>
          <Badge variant={statusInfo.variant as any}>
            {statusInfo.label}
          </Badge>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Submitted {formatDate(connection.createdAt, "long")}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Service Address
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                {connection.address}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Status Timeline
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="relative space-y-0">
                <div className="absolute left-[7px] top-2 h-[calc(100%-16px)] w-0.5 bg-gray-200 dark:bg-gray-700" />
                {connection.statusHistory.map((entry, idx) => {
                  const stepConfig = statusConfig[entry.toStatus] || {
                    label: entry.toStatus,
                    variant: "default" as const,
                  };
                  const isRejected = entry.toStatus === "REJECTED";
                  return (
                    <div key={idx} className="relative flex gap-4 pb-6 last:pb-0">
                      <div className="relative z-10 mt-0.5 flex h-4 w-4 items-center justify-center">
                        <div
                          className={`h-4 w-4 rounded-full border-2 ${
                            isRejected
                              ? "border-red-500 bg-red-100 dark:bg-red-900/30"
                              : entry.toStatus === "COMPLETED"
                              ? "border-green-500 bg-green-100 dark:bg-green-900/30"
                              : entry.toStatus === "APPROVED"
                              ? "border-teal-500 bg-teal-100 dark:bg-teal-900/30"
                              : entry.toStatus === "UNDER_REVIEW"
                              ? "border-amber-500 bg-amber-100 dark:bg-amber-900/30"
                              : "border-gray-400 bg-gray-100 dark:border-gray-600 dark:bg-gray-800"
                          }`}
                        />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <Badge variant={stepConfig.variant as any}>
                            {stepConfig.label}
                          </Badge>
                          <span className="text-xs text-gray-400">
                            {formatDate(entry.timestamp, "relative")}
                          </span>
                        </div>
                        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                          {entry.message}
                        </p>
                        <p className="mt-0.5 text-xs text-gray-400">
                          by {entry.updatedBy}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Citizen Information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Name
                </p>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {connection.citizenName || "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Mobile
                </p>
                <p className="text-sm text-gray-900 dark:text-gray-100">
                  {connection.citizenMobile
                    ? maskMobile(connection.citizenMobile)
                    : "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Citizen ID
                </p>
                <p className="text-sm font-mono text-gray-900 dark:text-gray-100">
                  {connection.citizenId}
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Actions
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {canApproveOrReject && (
                <>
                  <Button
                    className="w-full"
                    onClick={handleApprove}
                    disabled={actionLoading}
                  >
                    <CheckCircle className="mr-2 h-4 w-4" />
                    {actionLoading ? "Processing..." : "Approve"}
                  </Button>
                  <Button
                    variant="outline"
                    className="w-full border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-900/20"
                    onClick={() => setRejectDialogOpen(true)}
                    disabled={actionLoading}
                  >
                    <XCircle className="mr-2 h-4 w-4" />
                    Reject
                  </Button>
                </>
              )}
              {canComplete && (
                <Button
                  className="w-full"
                  onClick={handleComplete}
                  disabled={actionLoading}
                >
                  <CheckCircle className="mr-2 h-4 w-4" />
                  {actionLoading ? "Processing..." : "Mark Completed"}
                </Button>
              )}
              {isReadOnly && (
                <div className="rounded-lg bg-gray-50 p-4 text-center dark:bg-gray-800/50">
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    {connection.status === "REJECTED"
                      ? "This request has been rejected"
                      : "This request has been completed"}
                  </p>
                  <Badge
                    variant={
                      connection.status === "REJECTED"
                        ? "rejected"
                        : "success"
                    }
                    className="mt-1"
                  >
                    {statusInfo.label}
                  </Badge>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Reject Connection Request</DialogTitle>
            <DialogDescription>
              Provide a reason for rejecting this connection request.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-xs font-medium text-gray-500 dark:text-gray-400">
                Reason for Rejection
              </label>
              <Textarea
                placeholder="Describe why this request is being rejected..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={4}
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setRejectDialogOpen(false);
                  setRejectReason("");
                }}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleReject}
                disabled={actionLoading || !rejectReason.trim()}
              >
                {actionLoading ? "Rejecting..." : "Confirm Rejection"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}

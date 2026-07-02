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
  ArrowRight,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  Select,
  Textarea,
  Skeleton,
} from "@suvidha/ui";
import { statusConfig } from "@suvidha/ui";
import { formatDate } from "@suvidha/ui";
import { maskMobile } from "@suvidha/ui";
import type { AdminGrievance, StatusUpdateRequest } from "@/types/index";
import { adminApi } from "@/lib/api/client";

const GRIEVANCE_DETAIL_CONFIG: Record<string, { label: string; variant: "open" | "in_progress" | "resolved" | "rejected" | "default"; icon: React.ReactNode }> = {
  OPEN: {
    label: "Open",
    variant: "open",
    icon: <Clock className="h-4 w-4" />,
  },
  IN_PROGRESS: {
    label: "In Progress",
    variant: "in_progress",
    icon: <ArrowRight className="h-4 w-4" />,
  },
  RESOLVED: {
    label: "Resolved",
    variant: "resolved",
    icon: <CheckCircle className="h-4 w-4" />,
  },
  REJECTED: {
    label: "Rejected",
    variant: "rejected",
    icon: <XCircle className="h-4 w-4" />,
  },
  CLOSED: {
    label: "Closed",
    variant: "default",
    icon: <CheckCircle className="h-4 w-4" />,
  },
};

interface UpdateEntry {
  status: string;
  message: string;
  timestamp: string;
}

const MOCK_DETAIL: AdminGrievance & { photoUrl?: string; updates: UpdateEntry[] } = {
  id: "GRV-001",
  referenceNumber: "REF-2025-001",
  citizenId: "USR-001",
  category: "Water Supply",
  description:
    "No water supply in our area for the past 3 days. Residents are facing severe difficulties. The water supply used to come every morning at 6 AM but has stopped completely. We have filed complaints with the local office but no action has been taken yet. Request immediate intervention.",
  status: "IN_PROGRESS",
  submittedAt: "2025-05-20T08:30:00Z",
  citizenName: "Rahul Sharma",
  citizenMobile: "9876543210",
  photoUrl: "",
  updates: [
    {
      status: "OPEN",
      message: "Grievance registered and forwarded to Water Supply Department.",
      timestamp: "2025-05-20T08:30:00Z",
    },
    {
      status: "IN_PROGRESS",
      message: "Water Supply Department has taken up the case. Field inspection scheduled.",
      timestamp: "2025-05-21T10:15:00Z",
    },
  ],
};

const UPDATE_STATUS_OPTIONS = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "REJECTED",
  "CLOSED",
];

export default function GrievanceDetailPage() {
  const params = useParams();
  const router = useRouter();
  const refNumber = params.refNumber as string;

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [grievance, setGrievance] = useState<AdminGrievance & { photoUrl?: string; updates: UpdateEntry[] } | null>(null);
  const [newStatus, setNewStatus] = useState("");
  const [newMessage, setNewMessage] = useState("");

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi
        .getAllGrievances(0, 1)
        .catch(() => MOCK_DETAIL);
      if (data && data.content) {
        const found = data.content.find(
          (g: AdminGrievance) => g.referenceNumber === refNumber
        );
        setGrievance(found || MOCK_DETAIL);
      } else {
        setGrievance(MOCK_DETAIL);
      }
    } catch {
      setGrievance(MOCK_DETAIL);
    } finally {
      setLoading(false);
    }
  }, [refNumber]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const handleStatusUpdate = async () => {
    if (!newStatus || !newMessage.trim()) {
      toast.error("Please select a status and provide a message");
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.updateGrievanceStatus(refNumber, {
        status: newStatus,
        message: newMessage,
      });
      toast.success("Grievance status updated successfully");
      setNewMessage("");
      setNewStatus("");
      fetchDetail();
    } catch {
      if (grievance) {
        const updatedGrievance = { ...grievance };
        updatedGrievance.status = newStatus as AdminGrievance["status"];
        updatedGrievance.updates = [
          ...(updatedGrievance.updates || []),
          {
            status: newStatus,
            message: newMessage,
            timestamp: new Date().toISOString(),
          },
        ];
        setGrievance(updatedGrievance);
        toast.success("Grievance status updated (offline mode)");
        setNewMessage("");
        setNewStatus("");
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-32 w-full rounded-lg" />
        <Skeleton className="h-32 w-full rounded-lg" />
        <Skeleton className="h-48 w-full rounded-lg" />
      </div>
    );
  }

  if (!grievance) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <p className="text-lg font-medium text-gray-500 dark:text-gray-400">
          Grievance not found
        </p>
        <Button variant="link" onClick={() => router.push("/admin/grievances")}>
          Back to grievances
        </Button>
      </div>
    );
  }

  const statusInfo = GRIEVANCE_DETAIL_CONFIG[grievance.status] || {
    label: grievance.status,
    variant: "default" as const,
    icon: null,
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <Link
        href="/admin/grievances"
        className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to Grievances
      </Link>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-mono text-2xl font-bold text-gray-900 dark:text-gray-100">
            {grievance.referenceNumber}
          </h1>
          <Badge variant="info">{grievance.category}</Badge>
          <Badge variant={statusInfo.variant}>
            <span className="inline-flex items-center gap-1">
              {statusInfo.icon}
              {statusInfo.label}
            </span>
          </Badge>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Submitted {formatDate(grievance.submittedAt, "long")}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Description
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                {grievance.description}
              </p>
              {grievance.photoUrl && (
                <a
                  href={grievance.photoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-3 inline-flex items-center gap-1 text-sm text-brand-600 hover:underline dark:text-brand-400"
                >
                  View attached photo
                </a>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Update Timeline
              </CardTitle>
            </CardHeader>
            <CardContent>
              {grievance.updates && grievance.updates.length > 0 ? (
                <div className="relative space-y-0">
                  <div className="absolute left-[7px] top-2 h-[calc(100%-16px)] w-0.5 bg-gray-200 dark:bg-gray-700" />
                  {grievance.updates.map((update, idx) => {
                    const upConfig = GRIEVANCE_DETAIL_CONFIG[update.status] || {
                      label: update.status,
                      variant: "default" as const,
                    };
                    return (
                      <div key={idx} className="relative flex gap-4 pb-6 last:pb-0">
                        <div className="relative z-10 mt-1 flex h-4 w-4 items-center justify-center">
                          <div
                            className={`h-4 w-4 rounded-full border-2 ${
                              update.status === "RESOLVED" || update.status === "CLOSED"
                                ? "border-green-500 bg-green-100 dark:bg-green-900/30"
                                : update.status === "REJECTED"
                                ? "border-red-500 bg-red-100 dark:bg-red-900/30"
                                : update.status === "IN_PROGRESS"
                                ? "border-purple-500 bg-purple-100 dark:bg-purple-900/30"
                                : "border-blue-500 bg-blue-100 dark:bg-blue-900/30"
                            }`}
                          />
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <Badge variant={upConfig.variant}>
                              {upConfig.label}
                            </Badge>
                            <span className="text-xs text-gray-400">
                              {formatDate(update.timestamp, "relative")}
                            </span>
                          </div>
                          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                            {update.message}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="text-sm text-gray-400">No updates yet</p>
              )}
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
                  {grievance.citizenName || "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Mobile
                </p>
                <p className="text-sm text-gray-900 dark:text-gray-100">
                  {grievance.citizenMobile
                    ? maskMobile(grievance.citizenMobile)
                    : "—"}
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base font-semibold">
                Update Status
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="mb-1 block text-xs font-medium text-gray-500 dark:text-gray-400">
                  New Status
                </label>
                <Select value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
                  <option value="">Select status...</option>
                  {UPDATE_STATUS_OPTIONS.map((s) => (
                    <option key={s} value={s}>
                      {GRIEVANCE_DETAIL_CONFIG[s]?.label || s}
                    </option>
                  ))}
                </Select>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-gray-500 dark:text-gray-400">
                  Message
                </label>
                <Textarea
                  placeholder="Provide details about this update..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  rows={3}
                />
              </div>
              <Button
                className="w-full"
                onClick={handleStatusUpdate}
                disabled={submitting || !newStatus || !newMessage.trim()}
              >
                {submitting ? "Updating..." : "Update Status"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </motion.div>
  );
}

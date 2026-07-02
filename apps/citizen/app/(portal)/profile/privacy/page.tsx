"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  Shield,
  ArrowLeft,
  Download,
  FileJson,
  History,
  AlertTriangle,
  Eye,
  Trash2,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  ConfirmDialog,
  ConfirmDialogTrigger,
  ConfirmDialogContent,
  ConfirmDialogHeader,
  ConfirmDialogTitle,
  ConfirmDialogDescription,
  ConfirmDialogFooter,
  ConfirmDialogCancel,
  ConfirmDialogAction,
} from "@suvidha/ui";
import { formatDate, maskMobile, maskAadhaar } from "@/lib/utils";
import { useCitizenData, useExportData, useDeleteAccount, useAuditLogs } from "@/hooks/profile/useProfile";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { PageHeader } from "@/components/shared/PageHeader";
import { useAuthStore } from "@/store/auth.store";
import { toast } from "sonner";
import type { AuditLogEntry } from "@/types/api";

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.05, delayChildren: 0.08 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 14 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3, ease: "easeOut" } },
};

const AUDIT_PAGE_SIZE = 10;

export default function PrivacyCenterPage() {
  const router = useRouter();
  const [auditPage, setAuditPage] = useState(0);

  const { data: citizenData, isLoading: citizenLoading, error: citizenError } = useCitizenData();
  const { refetch: triggerExport, isFetching: exportLoading } = useExportData();
  const { data: auditLogsPage, isLoading: auditLoading, error: auditError } = useAuditLogs(auditPage);
  const deleteAccountMutation = useDeleteAccount();
  const logout = useAuthStore((s) => s.logout);

  const handleExport = async () => {
    try {
      const result = await triggerExport();
      if (result.data) {
        const blob = result.data;
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "suvidha-my-data.json";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        toast.success("Your data export has been downloaded");
      }
    } catch {
      toast.error("Failed to export data. Please try again.");
    }
  };

  const handleDeleteAccount = async () => {
    try {
      await deleteAccountMutation.mutateAsync();
      toast.success("Your account has been deleted");
      logout();
      router.push("/");
    } catch {
      toast.error("Failed to delete account. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Privacy Center"
        description="Manage your personal data in accordance with the Digital Personal Data Protection Act"
        breadcrumbs={[{ label: "Profile", href: "/profile" }, { label: "Privacy Center" }]}
        actions={
          <Badge variant="info" className="text-xs">
            <Shield className="h-3 w-3 mr-1" />
            DPDP Compliant
          </Badge>
        }
      />

      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="px-4 sm:px-6 lg:px-8 pb-8 space-y-6 max-w-4xl"
      >
        {/* Section 1: My Data */}
        <motion.div variants={itemVariants}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Eye className="h-5 w-5 text-brand-500" />
                My Data
              </CardTitle>
              <p className="text-sm text-muted dark:text-muted-dark">
                Personal information stored in your account
              </p>
            </CardHeader>
            <CardContent className="space-y-6">
              {citizenLoading ? (
                <LoadingSkeleton rows={6} />
              ) : citizenError ? (
                <p className="text-sm text-danger">Failed to load profile data</p>
              ) : citizenData ? (
                <>
                  {/* Citizen Details */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <DataField label="Full Name" value={citizenData.citizen.name ?? "—"} />
                    <DataField
                      label="Mobile"
                      value={maskMobile(citizenData.citizen.mobile)}
                    />
                    <DataField
                      label="Aadhaar"
                      value={
                        citizenData.citizen.aadhaarLast4
                          ? `xxxx-xxxx-${citizenData.citizen.aadhaarLast4}`
                          : "Not provided"
                      }
                    />
                    <DataField label="Language" value={(citizenData.citizen.languagePref || "en").toUpperCase()} />
                    <DataField label="Role" value={citizenData.citizen.role} />
                    <DataField
                      label="Member Since"
                      value={formatDate(citizenData.citizen.createdAt, "long")}
                    />
                  </div>

                  {/* Consents */}
                  {citizenData.consents.length > 0 && (
                    <div className="border-t border-border pt-5">
                      <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">
                        Active Consents
                      </h4>
                      <div className="space-y-3">
                        {citizenData.consents.map((consent) => (
                          <div
                            key={consent.id}
                            className="flex items-center justify-between rounded-lg border border-border bg-gray-50 dark:bg-gray-900/50 p-3"
                          >
                            <div className="space-y-1 min-w-0 flex-1">
                              <p className="text-sm font-medium text-gray-900 dark:text-gray-100 capitalize">
                                {consent.consentType.replace(/_/g, " ").toLowerCase()}
                              </p>
                              <div className="flex items-center gap-3 text-xs text-muted dark:text-muted-dark">
                                <span>Granted: {formatDate(consent.grantedAt, "short")}</span>
                                <span>Expires: {formatDate(consent.expiresAt, "short")}</span>
                              </div>
                            </div>
                            <StatusBadge status="ACTIVE" />
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              ) : null}
            </CardContent>
          </Card>
        </motion.div>

        {/* Section 2: Export My Data */}
        <motion.div variants={itemVariants}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileJson className="h-5 w-5 text-brand-500" />
                Export My Data
              </CardTitle>
              <p className="text-sm text-muted dark:text-muted-dark">
                Download all your personal data in JSON format
              </p>
            </CardHeader>
            <CardContent>
              <Button onClick={handleExport} loading={exportLoading}>
                <Download className="h-4 w-4 mr-2" />
                Download my data (JSON)
              </Button>
            </CardContent>
          </Card>
        </motion.div>

        {/* Section 3: Audit Log */}
        <motion.div variants={itemVariants}>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <History className="h-5 w-5 text-brand-500" />
                Audit Log
              </CardTitle>
              <p className="text-sm text-muted dark:text-muted-dark">
                Track all activities on your account
              </p>
            </CardHeader>
            <CardContent>
              {auditLoading ? (
                <LoadingSkeleton type="table" rows={5} />
              ) : auditError ? (
                <p className="text-sm text-danger">Failed to load audit logs</p>
              ) : auditLogsPage && auditLogsPage.content.length > 0 ? (
                <div className="space-y-4">
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-border">
                          <th className="text-left py-3 px-2 font-medium text-muted dark:text-muted-dark text-xs uppercase tracking-wider">
                            Action
                          </th>
                          <th className="text-left py-3 px-2 font-medium text-muted dark:text-muted-dark text-xs uppercase tracking-wider">
                            Details
                          </th>
                          <th className="text-left py-3 px-2 font-medium text-muted dark:text-muted-dark text-xs uppercase tracking-wider hidden sm:table-cell">
                            IP Address
                          </th>
                          <th className="text-right py-3 px-2 font-medium text-muted dark:text-muted-dark text-xs uppercase tracking-wider">
                            Date
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {auditLogsPage.content.map((entry: AuditLogEntry) => (
                          <tr
                            key={entry.id}
                            className="border-b border-border last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-900/30 transition-colors"
                          >
                            <td className="py-3 px-2">
                              <span className="font-semibold text-gray-900 dark:text-gray-100 capitalize">
                                {entry.action.replace(/_/g, " ").toLowerCase()}
                              </span>
                            </td>
                            <td className="py-3 px-2 text-muted dark:text-muted-dark max-w-[200px] truncate">
                              {entry.details}
                            </td>
                            <td className="py-3 px-2 text-muted dark:text-muted-dark font-mono text-xs hidden sm:table-cell">
                              {entry.ip}
                            </td>
                            <td className="py-3 px-2 text-right text-muted dark:text-muted-dark whitespace-nowrap">
                              {formatDate(entry.createdAt, "short")}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination */}
                  <div className="flex items-center justify-between pt-2">
                    <p className="text-xs text-muted dark:text-muted-dark">
                      Page {auditLogsPage.number + 1} of {auditLogsPage.totalPages}
                    </p>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={auditLogsPage.first}
                        onClick={() => setAuditPage((p) => Math.max(0, p - 1))}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={auditLogsPage.last}
                        onClick={() => setAuditPage((p) => p + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center py-10 text-center">
                  <History className="h-10 w-10 text-muted dark:text-muted-dark mb-3" />
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    No audit log entries
                  </p>
                  <p className="text-xs text-muted dark:text-muted-dark mt-1">
                    Activity records will appear here
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>

        {/* Section 4: Delete Account */}
        <motion.div variants={itemVariants}>
          <Card className="border-danger/30 dark:border-danger/20">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-danger">
                <AlertTriangle className="h-5 w-5" />
                Delete Account
              </CardTitle>
              <p className="text-sm text-muted dark:text-muted-dark">
                Permanently remove your account and all associated data
              </p>
            </CardHeader>
            <CardContent>
              <div className="rounded-lg bg-danger/5 dark:bg-danger/10 border border-danger/20 p-4 space-y-3">
                <div className="flex items-start gap-3">
                  <Trash2 className="h-5 w-5 text-danger shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm text-gray-900 dark:text-gray-100">
                      Once you delete your account, there is no going back. This action is
                      irreversible and will remove all your data including bills, grievances,
                      and connection requests.
                    </p>
                  </div>
                </div>

                <ConfirmDialog>
                  <ConfirmDialogTrigger asChild>
                    <Button variant="destructive">
                      <Trash2 className="h-4 w-4 mr-2" />
                      Delete Account
                    </Button>
                  </ConfirmDialogTrigger>
                  <ConfirmDialogContent>
                    <ConfirmDialogHeader>
                      <ConfirmDialogTitle>Are you sure?</ConfirmDialogTitle>
                      <ConfirmDialogDescription>
                        This action cannot be undone. All your data including bills, grievances,
                        and connection requests will be permanently deleted.
                      </ConfirmDialogDescription>
                    </ConfirmDialogHeader>
                    <ConfirmDialogFooter>
                      <ConfirmDialogCancel>Cancel</ConfirmDialogCancel>
                      <ConfirmDialogAction
                        className="bg-danger hover:bg-red-700 text-white"
                        onClick={handleDeleteAccount}
                      >
                        {deleteAccountMutation.isPending ? (
                          <span className="flex items-center gap-2">
                            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                            Deleting...
                          </span>
                        ) : (
                          "Delete My Account"
                        )}
                      </ConfirmDialogAction>
                    </ConfirmDialogFooter>
                  </ConfirmDialogContent>
                </ConfirmDialog>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </motion.div>
    </div>
  );
}

function DataField({ label, value }: { label: string; value: string }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium text-muted dark:text-muted-dark uppercase tracking-wider">
        {label}
      </p>
      <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{value}</p>
    </div>
  );
}

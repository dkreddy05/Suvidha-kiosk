"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { Button, Card, CardHeader, CardTitle, CardContent, Input } from "@suvidha/ui";
import { useDeleteAccount } from "@/hooks/profile/useProfile";
import { useAuthStore } from "@/store/auth.store";
import { toast } from "sonner";

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.08, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
};

export default function DeleteAccountPage() {
  const router = useRouter();
  const [confirmationText, setConfirmationText] = useState("");
  const deleteAccountMutation = useDeleteAccount();
  const logout = useAuthStore((s) => s.logout);

  const isConfirmed = confirmationText === "DELETE";

  const handleDelete = async () => {
    if (!isConfirmed) return;
    try {
      await deleteAccountMutation.mutateAsync();
      toast.success("Your account has been permanently deleted");
      logout();
      router.push("/");
    } catch {
      toast.error("Failed to delete account. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center px-4 py-12">
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="w-full max-w-md"
      >
        <motion.div variants={itemVariants}>
          <Card className="border-danger/30 dark:border-danger/20">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-4">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-danger/10">
                  <AlertTriangle className="h-8 w-8 text-danger" />
                </div>
              </div>
              <CardTitle className="text-danger">Delete Account</CardTitle>
              <p className="text-sm text-muted dark:text-muted-dark mt-1">
                This action is irreversible
              </p>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="rounded-lg bg-danger/5 dark:bg-danger/10 border border-danger/20 p-4 text-sm text-gray-900 dark:text-gray-100 space-y-2">
                <p>
                  <strong>What will be deleted:</strong>
                </p>
                <ul className="list-disc pl-5 space-y-1 text-muted dark:text-muted-dark">
                  <li>Your profile and personal information</li>
                  <li>All linked utility accounts</li>
                  <li>Billing history and payment records</li>
                  <li>Submitted grievances and their status</li>
                  <li>Connection request history</li>
                </ul>
                <p className="text-danger text-xs font-medium mt-2">
                  This data cannot be recovered once deleted.
                </p>
              </div>

              <div className="space-y-1.5">
                <label
                  htmlFor="delete-confirm"
                  className="text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Type <span className="font-bold text-danger">DELETE</span> to confirm
                </label>
                <Input
                  id="delete-confirm"
                  placeholder='Type "DELETE" to confirm'
                  value={confirmationText}
                  onChange={(e) => setConfirmationText(e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-3 pt-2">
                <Button
                  variant="destructive"
                  disabled={!isConfirmed}
                  loading={deleteAccountMutation.isPending}
                  onClick={handleDelete}
                  className="w-full"
                >
                  <AlertTriangle className="h-4 w-4 mr-2" />
                  Permanently Delete My Account
                </Button>

                <Link href="/profile">
                  <Button variant="outline" className="w-full">
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Cancel
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </motion.div>
    </div>
  );
}

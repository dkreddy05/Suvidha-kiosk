import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors",
  {
    variants: {
      variant: {
        default: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-100",
        pending: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300",
        paid: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300",
        overdue: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300",
        open: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300",
        resolved: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300",
        rejected: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300",
        in_progress: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300",
        approved: "bg-teal-100 text-teal-800 dark:bg-teal-900/30 dark:text-teal-300",
        info: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300",
        warning: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300",
        success: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };

export const statusConfig: Record<string, { label: string; variant: NonNullable<BadgeProps["variant"]> }> = {
  PENDING: { label: "Pending", variant: "pending" },
  PAID: { label: "Paid", variant: "paid" },
  OVERDUE: { label: "Overdue", variant: "overdue" },
  OPEN: { label: "Open", variant: "open" },
  RESOLVED: { label: "Resolved", variant: "resolved" },
  REJECTED: { label: "Rejected", variant: "rejected" },
  IN_PROGRESS: { label: "In Progress", variant: "in_progress" },
  APPROVED: { label: "Approved", variant: "approved" },
  COMPLETED: { label: "Completed", variant: "success" },
  UNDER_REVIEW: { label: "Under Review", variant: "warning" },
  CANCELLED: { label: "Cancelled", variant: "default" },
  FAILED: { label: "Failed", variant: "rejected" },
};

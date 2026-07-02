"use client";

import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useState, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Smartphone,
  CreditCard,
  Banknote,
  CheckCircle2,
  ArrowLeft,
  Download,
  AlertTriangle,
} from "lucide-react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Skeleton,
  Badge,
} from "@suvidha/ui";
import { formatCurrency, formatDate } from "@/lib/utils";
import { useAccounts } from "@/hooks/billing/useAccounts";
import { useBillById, useBills } from "@/hooks/billing/useBills";
import { usePayBill } from "@/hooks/billing/usePayment";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { PaymentMethod, Bill } from "@/types/billing";
import { toast } from "sonner";

type Step = "review" | "payment-method" | "confirm" | "success";

const stepVariants = {
  enter: { opacity: 0, x: 40 },
  center: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
};

const paymentMethods: {
  value: PaymentMethod;
  icon: typeof Smartphone;
  title: string;
  description: string;
}[] = [
  {
    value: "UPI",
    icon: Smartphone,
    title: "UPI",
    description: "Pay via UPI (Google Pay, PhonePe, Paytm)",
  },
  {
    value: "CARD",
    icon: CreditCard,
    title: "Card",
    description: "Debit / Credit Card",
  },
  {
    value: "CASH",
    icon: Banknote,
    title: "Cash",
    description: "Cash (Kiosk / Counter)",
  },
];

function StepIndicator({ currentStep }: { currentStep: Step }) {
  const steps: { key: Step; label: string }[] = [
    { key: "review", label: "Review" },
    { key: "payment-method", label: "Payment" },
    { key: "confirm", label: "Confirm" },
  ];
  const currentIdx = steps.findIndex((s) => s.key === currentStep);

  return (
    <div className="flex items-center gap-2 mb-8">
      {steps.map((step, idx) => {
        const isCompleted = idx < currentIdx;
        const isCurrent = idx === currentIdx;
        return (
          <div key={step.key} className="flex items-center gap-2 flex-1">
            <div
              className={
                "flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium transition-colors " +
                (isCompleted
                  ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300"
                  : isCurrent
                    ? "bg-brand-100 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300"
                    : "bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500")
              }
            >
              <span
                className={
                  "flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold " +
                  (isCompleted
                    ? "bg-green-500 text-white"
                    : isCurrent
                      ? "bg-brand-500 text-white"
                      : "bg-gray-300 dark:bg-gray-600 text-white")
                }
              >
                {isCompleted ? "✓" : idx + 1}
              </span>
              <span className="hidden sm:inline">{step.label}</span>
            </div>
            {idx < steps.length - 1 && (
              <div
                className={
                  "h-0.5 flex-1 " +
                  (isCompleted
                    ? "bg-green-400"
                    : "bg-gray-200 dark:bg-gray-700")
                }
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

export default function PayBillPage() {
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const accountId = params.accountId as string;
  const billId = searchParams.get("billId");

  const [step, setStep] = useState<Step>("review");
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod | null>(null);

  const { data: accounts, isLoading: accountsLoading } = useAccounts();
  const { data: bills, isLoading: billsLoading } = useBills(accountId);
  const { data: singleBill, isLoading: singleBillLoading } = useBillById(
    accountId,
    billId ?? ""
  );
  const payBillMutation = usePayBill(accountId);

  const account = useMemo(() => {
    if (!accounts) return undefined;
    return accounts.find((a) => a.id === accountId);
  }, [accounts, accountId]);

  const targetBill = useMemo(() => {
    if (billId && singleBill) return singleBill;
    if (!bills) return undefined;
    const billList = Array.isArray(bills) ? bills : [];
    return billId
      ? billList.find((b: Bill) => b.id === billId)
      : billList.find((b: Bill) => b.status === "PENDING" || b.status === "OVERDUE");
  }, [billId, singleBill, bills]);

  const amount = targetBill
    ? targetBill.amount + (targetBill.lateFee ?? 0)
    : 0;

  const isLoading =
    accountsLoading ||
    billsLoading ||
    (!!billId && singleBillLoading);

  const handlePay = useCallback(async () => {
    if (!targetBill || !selectedMethod) return;
    try {
      const result = await payBillMutation.mutateAsync({
        billIds: [targetBill.id],
        amount: amount,
        method: selectedMethod,
      });
      toast.success("Payment successful!", {
        description: `Transaction ID: ${result.transactionId}`,
      });
      setStep("success");
    } catch (err: any) {
      toast.error("Payment failed", {
        description: err?.message ?? "An unexpected error occurred. Please try again.",
      });
    }
  }, [targetBill, selectedMethod, amount, payBillMutation]);

  const paymentResponse = payBillMutation.data as {
    transactionId?: string;
    receiptUrl?: string;
    createdAt?: string;
    method?: PaymentMethod;
    amount?: number;
  } | null;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Pay Bill"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Account", href: `/billing/${accountId}` },
            { label: "Pay" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8 pb-8 max-w-2xl mx-auto">
          <Card>
            <CardContent className="p-6 space-y-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-10 w-full rounded-lg" />
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  if (!account) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Pay Bill"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Account", href: `/billing/${accountId}` },
            { label: "Pay" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertTriangle}
            title="Account not found"
            description="The account you're looking for doesn't exist."
            actionLabel="Back to Billing"
            onAction={() => router.push("/billing")}
          />
        </div>
      </div>
    );
  }

  if (!targetBill) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <PageHeader
          title="Pay Bill"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Account", href: `/billing/${accountId}` },
            { label: "Pay" },
          ]}
        />
        <div className="px-4 sm:px-6 lg:px-8">
          <EmptyState
            icon={AlertTriangle}
            title="No pending bills"
            description="This account has no pending or overdue bills to pay."
            actionLabel="View Account"
            onAction={() => router.push(`/billing/${accountId}`)}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <PageHeader
        title="Pay Bill"
        breadcrumbs={[
          { label: "Billing", href: "/billing" },
          { label: account.providerName, href: `/billing/${accountId}` },
          { label: "Pay" },
        ]}
      />

      <div className="px-4 sm:px-6 lg:px-8 pb-8 max-w-2xl mx-auto">
        <StepIndicator currentStep={step} />

        <AnimatePresence mode="wait">
          {step === "review" && (
            <motion.div
              key="review"
              variants={stepVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.3 }}
            >
              <Card>
                <CardHeader>
                  <CardTitle>Review Bill</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center gap-3 pb-4 border-b border-border">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
                      <span className="text-lg font-bold text-gray-500 dark:text-gray-400">
                        {account.serviceType.charAt(0)}
                      </span>
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {account.providerName}
                      </p>
                      <p className="text-xs font-mono text-muted dark:text-muted-dark">
                        ••••{account.accountNumber.slice(-4)}
                      </p>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Bill Month</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {targetBill.billMonth}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Bill Number</span>
                      <span className="text-sm font-mono font-medium text-gray-900 dark:text-gray-100">
                        {targetBill.billNumber}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Bill Amount</span>
                      <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {formatCurrency(targetBill.amount)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Due Date</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {formatDate(targetBill.dueDate)}
                      </span>
                    </div>
                    {targetBill.lateFee && targetBill.lateFee > 0 && (
                      <div className="flex items-center justify-between text-red-600 dark:text-red-400">
                        <span className="text-sm font-medium">Late Fee</span>
                        <span className="text-sm font-semibold">
                          +{formatCurrency(targetBill.lateFee)}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="border-t border-border pt-4 flex items-center justify-between">
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                      Total Payable
                    </span>
                    <span className="text-xl font-bold text-gray-900 dark:text-gray-100">
                      {formatCurrency(amount)}
                    </span>
                  </div>

                  <Button
                    className="w-full mt-2"
                    size="lg"
                    onClick={() => setStep("payment-method")}
                  >
                    Proceed to Payment
                  </Button>
                </CardContent>
              </Card>
            </motion.div>
          )}

          {step === "payment-method" && (
            <motion.div
              key="payment-method"
              variants={stepVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.3 }}
            >
              <Card>
                <CardHeader>
                  <CardTitle>Select Payment Method</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  {paymentMethods.map((method) => {
                    const Icon = method.icon;
                    const isSelected = selectedMethod === method.value;
                    return (
                      <button
                        key={method.value}
                        onClick={() => setSelectedMethod(method.value)}
                        className={
                          "w-full flex items-center gap-4 p-4 rounded-xl border-2 text-left transition-all duration-200 " +
                          (isSelected
                            ? "border-brand-500 bg-brand-50 dark:bg-brand-900/10"
                            : "border-border bg-white dark:bg-surface-dark hover:border-gray-300 dark:hover:border-gray-600")
                        }
                      >
                        <div
                          className={
                            "flex h-10 w-10 items-center justify-center rounded-lg shrink-0 " +
                            (isSelected
                              ? "bg-brand-100 text-brand-600 dark:bg-brand-900/30 dark:text-brand-300"
                              : "bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400")
                          }
                        >
                          <Icon className="h-5 w-5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p
                            className={
                              "text-sm font-semibold " +
                              (isSelected
                                ? "text-brand-700 dark:text-brand-300"
                                : "text-gray-900 dark:text-gray-100")
                            }
                          >
                            {method.title}
                          </p>
                          <p className="text-xs text-muted dark:text-muted-dark mt-0.5">
                            {method.description}
                          </p>
                        </div>
                        <div
                          className={
                            "flex h-5 w-5 items-center justify-center rounded-full border-2 shrink-0 " +
                            (isSelected
                              ? "border-brand-500 bg-brand-500"
                              : "border-gray-300 dark:border-gray-600")
                          }
                        >
                          {isSelected && (
                            <div className="h-2 w-2 rounded-full bg-white" />
                          )}
                        </div>
                      </button>
                    );
                  })}

                  <div className="flex items-center gap-3 pt-4">
                    <Button
                      variant="outline"
                      onClick={() => setStep("review")}
                      className="flex-1"
                    >
                      Back
                    </Button>
                    <Button
                      className="flex-1"
                      disabled={!selectedMethod}
                      onClick={() => setStep("confirm")}
                    >
                      Continue
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          )}

          {step === "confirm" && (
            <motion.div
              key="confirm"
              variants={stepVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.3 }}
            >
              <Card>
                <CardHeader>
                  <CardTitle>Confirm Payment</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="rounded-xl bg-gray-50 dark:bg-gray-800/50 p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Amount</span>
                      <span className="text-lg font-bold text-gray-900 dark:text-gray-100">
                        {formatCurrency(amount)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Payment Method</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100 capitalize">
                        {selectedMethod === "UPI"
                          ? "UPI"
                          : selectedMethod === "CARD"
                            ? "Debit / Credit Card"
                            : "Cash (Kiosk / Counter)"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Reference</span>
                      <span className="text-xs font-mono font-medium text-gray-900 dark:text-gray-100">
                        {payBillMutation.data
                          ? (payBillMutation.data as any).transactionId ?? "TXN-REF"
                          : "Generating..."}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Account</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {account.providerName} (••••{account.accountNumber.slice(-4)})
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-muted dark:text-muted-dark">Bill</span>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {targetBill.billMonth} - {targetBill.billNumber}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 pt-2">
                    <Button
                      variant="outline"
                      onClick={() => setStep("payment-method")}
                      className="flex-1"
                      disabled={payBillMutation.isPending}
                    >
                      Back
                    </Button>
                    <Button
                      className="flex-1"
                      size="lg"
                      disabled={payBillMutation.isPending}
                      onClick={handlePay}
                    >
                      {payBillMutation.isPending
                        ? "Processing..."
                        : `Pay ${formatCurrency(amount)}`}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          )}

          {step === "success" && (
            <motion.div
              key="success"
              variants={stepVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.3 }}
            >
              <Card>
                <CardContent className="py-10 px-6 text-center">
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{
                      type: "spring",
                      stiffness: 200,
                      damping: 15,
                      delay: 0.1,
                    }}
                    className="flex justify-center mb-6"
                  >
                    <div className="flex h-20 w-20 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
                      <CheckCircle2 className="h-10 w-10 text-green-600 dark:text-green-400" />
                    </div>
                  </motion.div>

                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 }}
                  >
                    <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">
                      Payment Successful!
                    </h2>
                    <p className="text-sm text-muted dark:text-muted-dark mb-6">
                      Your payment of{" "}
                      <span className="font-semibold text-gray-900 dark:text-gray-100">
                        {formatCurrency(amount)}
                      </span>{" "}
                      has been processed.
                    </p>
                  </motion.div>

                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.5 }}
                    className="rounded-xl bg-gray-50 dark:bg-gray-800/50 p-4 mb-6 text-left space-y-2"
                  >
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted dark:text-muted-dark">Transaction ID</span>
                      <span className="font-mono font-medium text-gray-900 dark:text-gray-100">
                        {paymentResponse?.transactionId ?? "TXN-SUCCESS"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted dark:text-muted-dark">Amount Paid</span>
                      <span className="font-semibold text-gray-900 dark:text-gray-100">
                        {formatCurrency(amount)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted dark:text-muted-dark">Payment Method</span>
                      <span className="font-medium text-gray-900 dark:text-gray-100 capitalize">
                        {selectedMethod === "UPI"
                          ? "UPI"
                          : selectedMethod === "CARD"
                            ? "Card"
                            : "Cash"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted dark:text-muted-dark">Account</span>
                      <span className="font-medium text-gray-900 dark:text-gray-100">
                        {account.providerName}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted dark:text-muted-dark">Bill</span>
                      <span className="font-medium text-gray-900 dark:text-gray-100">
                        {targetBill.billMonth}
                      </span>
                    </div>
                  </motion.div>

                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.7 }}
                    className="flex flex-col sm:flex-row gap-3"
                  >
                    <Button
                      variant="outline"
                      className="flex-1"
                      onClick={() => {
                        const tid = paymentResponse?.transactionId ?? "unknown";
                        window.open(`/billing/receipt/${tid}`, "_blank");
                      }}
                    >
                      <Download className="h-4 w-4 mr-2" />
                      Download Receipt
                    </Button>
                    <Button
                      className="flex-1"
                      onClick={() => router.push("/billing")}
                    >
                      Back to Dashboard
                    </Button>
                  </motion.div>
                </CardContent>
              </Card>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

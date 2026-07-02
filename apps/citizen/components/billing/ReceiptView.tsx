"use client";

import React from "react";
import { Receipt, CheckCircle2 } from "lucide-react";
import { Card, CardTitle, CardContent, Badge } from "@suvidha/ui";
import { formatCurrency, formatDate } from "@/lib/utils";
import { StatusBadge } from "../shared/StatusBadge";
import type { Payment } from "@/types/billing";

interface ReceiptViewProps {
  payment: Payment;
}

export function ReceiptView({ payment }: ReceiptViewProps) {
  return (
    <Card className="print:shadow-none print:border-0 w-full max-w-2xl mx-auto">
      <CardContent className="p-6 sm:p-8">
        <div className="text-center pb-6 border-b border-border mb-6">
          <div className="flex items-center justify-center gap-2 text-brand-600 dark:text-brand-400 mb-2">
            <Receipt className="h-6 w-6" />
            <span className="font-display text-xl font-bold">Suvidha</span>
          </div>
          <CardTitle className="text-2xl">Payment Receipt</CardTitle>
          <p className="text-sm text-muted mt-1">
            Transaction ID:{" "}
            <span className="font-mono text-foreground">{payment.transactionId}</span>
          </p>
        </div>

        <div className="flex items-center justify-center gap-2 mb-6">
          <CheckCircle2 className="h-5 w-5 text-success" />
          <span className="text-success font-semibold text-lg">Payment Successful</span>
        </div>

        <div className="space-y-4 mb-6">
          <div className="flex items-center justify-between py-2 border-b border-border">
            <span className="text-muted">Date & Time</span>
            <span className="font-medium">{formatDate(payment.createdAt, "long")}</span>
          </div>
          <div className="flex items-center justify-between py-2 border-b border-border">
            <span className="text-muted">Payment Method</span>
            <Badge variant="info">{payment.method}</Badge>
          </div>
          <div className="flex items-center justify-between py-2 border-b border-border">
            <span className="text-muted">Amount Paid</span>
            <span className="font-display text-2xl font-bold text-foreground">
              {formatCurrency(payment.amount)}
            </span>
          </div>
          <div className="flex items-center justify-between py-2 border-b border-border">
            <span className="text-muted">Account</span>
            <span className="font-medium">{payment.accountId}</span>
          </div>
          <div className="flex items-start justify-between py-2 border-b border-border">
            <span className="text-muted">Bills Covered</span>
            <div className="text-right">
              {payment.billIds.map((billId, i) => (
                <div key={billId} className="font-mono text-sm">
                  {billId}{i < payment.billIds.length - 1 ? "," : ""}
                </div>
              ))}
            </div>
          </div>
          <div className="flex items-center justify-between py-2">
            <span className="text-muted">Status</span>
            <StatusBadge status="PAID" />
          </div>
        </div>

        <div className="border-t border-border pt-6 text-center text-xs text-muted">
          <p>This is a computer-generated receipt and does not require a physical signature.</p>
          <p className="mt-1">For queries, contact your utility provider.</p>
        </div>
      </CardContent>
    </Card>
  );
}

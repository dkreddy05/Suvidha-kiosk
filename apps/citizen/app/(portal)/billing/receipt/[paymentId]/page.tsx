"use client";

import { useParams, useRouter } from "next/navigation";
import { Download, Printer, AlertTriangle, Receipt, CheckCircle2 } from "lucide-react";
import { Card, CardTitle, CardContent, Button, Skeleton, Badge } from "@suvidha/ui";
import { formatCurrency, formatDate } from "@/lib/utils";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import type { Payment, PaymentMethod } from "@/types/billing";
import { jsPDF } from "jspdf";
import { ReceiptView } from "@/components/billing/ReceiptView";

function ReceiptSkeleton() {
  return (
    <div className="max-w-2xl mx-auto">
      <Card>
        <CardContent className="p-6 space-y-4">
          <div className="text-center space-y-2 pb-4 border-b border-border">
            <Skeleton className="h-6 w-48 mx-auto" />
            <Skeleton className="h-4 w-32 mx-auto" />
          </div>
          <div className="space-y-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="flex items-center justify-between">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-4 w-36" />
              </div>
            ))}
          </div>
          <div className="border-t border-border pt-4 text-center">
            <Skeleton className="h-10 w-40 mx-auto rounded-lg" />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function buildMockPayment(paymentId: string): Payment {
  const methods: PaymentMethod[] = ["UPI", "CARD", "CASH"];
  return {
    transactionId: paymentId,
    accountId: "ACC-" + paymentId.slice(0, 8).toUpperCase(),
    amount: Math.floor(Math.random() * 5000) + 500,
    method: methods[Math.floor(Math.random() * methods.length)],
    status: "SUCCESS",
    createdAt: new Date().toISOString(),
    billIds: [`BILL-${paymentId.slice(0, 6).toUpperCase()}`, `BILL-${paymentId.slice(6, 12).toUpperCase()}`],
  };
}

export default function ReceiptPage() {
  const params = useParams();
  const router = useRouter();
  const paymentId = params.paymentId as string;
  const payment = buildMockPayment(paymentId);

  const handlePrint = () => window.print();

  const handleDownloadPDF = () => {
    const doc = new jsPDF({
      orientation: "portrait",
      unit: "mm",
      format: "a4",
    });

    // Font settings
    doc.setFont("helvetica", "bold");
    doc.setFontSize(22);
    doc.setTextColor(37, 99, 235); // Brand blue
    doc.text("Suvidha Portal", 20, 20);

    doc.setFontSize(16);
    doc.setTextColor(17, 24, 39);
    doc.text("Payment Receipt", 20, 30);

    // Separator line
    doc.setDrawColor(229, 231, 235);
    doc.line(20, 35, 190, 35);

    // Receipt details
    doc.setFont("helvetica", "normal");
    doc.setFontSize(11);
    doc.setTextColor(107, 114, 128);

    let y = 45;
    const addRow = (label: string, value: string) => {
      doc.setFont("helvetica", "normal");
      doc.setTextColor(107, 114, 128);
      doc.text(label, 20, y);
      doc.setFont("helvetica", "bold");
      doc.setTextColor(17, 24, 39);
      doc.text(value, 90, y);
      y += 10;
    };

    addRow("Transaction ID:", payment.transactionId);
    addRow("Date & Time:", formatDate(payment.createdAt, "long"));
    addRow("Payment Method:", payment.method);
    addRow("Account:", payment.accountId);
    addRow("Bills Covered:", payment.billIds.join(", "));
    addRow("Amount Paid:", formatCurrency(payment.amount));
    addRow("Status:", "Payment Successful");

    // Divider
    doc.line(20, y, 190, y);
    y += 10;

    // Footer info
    doc.setFont("helvetica", "italic");
    doc.setFontSize(9);
    doc.setTextColor(156, 163, 175);
    doc.text("This is a computer-generated receipt and does not require a physical signature.", 20, y);
    doc.text("For support, contact support@suvidha.gov.in or your utility provider.", 20, y + 5);

    doc.save(`receipt-${payment.transactionId}.pdf`);
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 print:bg-white">
      <div className="print:hidden">
        <PageHeader
          title="Payment Receipt"
          breadcrumbs={[
            { label: "Billing", href: "/billing" },
            { label: "Receipt" },
          ]}
          actions={
            <div className="flex gap-3">
              <Button variant="outline" onClick={handlePrint}>
                <Printer className="mr-2 h-4 w-4" />
                Print
              </Button>
              <Button onClick={handleDownloadPDF}>
                <Download className="mr-2 h-4 w-4" />
                Download PDF
              </Button>
            </div>
          }
        />
      </div>

      <div className="px-4 sm:px-6 lg:px-8 pb-8">
        <ReceiptView payment={payment} />
      </div>
    </div>
  );
}
